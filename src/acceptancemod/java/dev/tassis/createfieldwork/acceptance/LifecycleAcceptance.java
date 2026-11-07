package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.tassis.createfieldwork.content.IsolatorBlock;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Normal lifecycle APIs and a real observer exception, without a research registry or behavior mixin. */
final class LifecycleAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    LifecycleAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }

    void run() {
        var state = FieldworkStateAccess.get(level.getServer());
        var container = SubLevelContainer.getContainer(level);
        BlockPos origin = new BlockPos(192, 120, 0);
        var source = link(origin, false);
        link(origin.offset(6, 0, 0), true);
        var outside = link(origin.offset(10, 0, 0), true);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 6; x++) {
            var floor = origin.offset(x, -1, 0);
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3); positions.add(floor);
        }
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        positions.add(origin); positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(6, 0, 0));
        source.transmit(15);
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        var stored = craft.getPlot().getCenterBlock();
        source = initialized(stored); source.transmit(15);
        initialized(stored.offset(6, 0, 0)); state.refresh().refresh(null);
        signal("main lifecycle baseline private denies WORLD", outside, 0);
        var uuid = craft.getUniqueId();
        var holding = container.getHoldingChunkMap();
        holding.moveToUnloaded(craft, new ChunkPos(origin));
        check.accept("normal unload enters actual Sable holding map", holding.getHoldingSubLevel(uuid) != null);
        holding.loadHoldingSubLevel(holding.getHoldingSubLevel(uuid));
        var restored = (ServerSubLevel) container.getSubLevel(uuid);
        check.accept("holding load restores same UUID and private main registry", restored != null && state.registry().scope(IsolationRuntime.key(restored)).equals(Scope.privateCraft(IsolationRuntime.key(restored))));
        initialized(stored).transmit(15); initialized(stored.offset(6, 0, 0)); state.refresh().refresh(null);
        signal("holding load cannot leak private source", outside, 0);
        var data = SubLevelSerializer.toData(restored, List.of());
        check.accept("duplicate load is rejected by real upstream", SubLevelSerializer.fullyLoad(level, data) == null);
        check.accept("rejected duplicate does not poison existing main craft", state.transitions().failedTokens().isEmpty());

        holding.moveToUnloaded(restored, new ChunkPos(origin));
        var retained = holding.getHoldingSubLevel(uuid);
        boolean[] failOnce = {true};
        container.addObserver(new SubLevelObserver() {
            @Override public void onSubLevelAdded(SubLevel added) {
                if (failOnce[0] && added.getUniqueId().equals(uuid)) {
                    failOnce[0] = false;
                    throw new IllegalStateException("acceptance allocation observer failure");
                }
            }
        });
        try { holding.loadHoldingSubLevel(retained); }
        catch (IllegalStateException expected) {
            if (!expected.getMessage().equals("acceptance allocation observer failure")) throw expected;
        }
        check.accept("real observer fault was exercised", !failOnce[0]);
        check.accept("failed load retains main quarantine", !state.transitions().failedTokens().isEmpty());
        var partial = container.getSubLevel(uuid);
        if (partial != null) container.removeSubLevel(partial, SubLevelRemovalReason.REMOVED);
        holding.loadHoldingSubLevel(retained);
        restored = (ServerSubLevel) container.getSubLevel(uuid);
        check.accept("normal retry replaces allocation-only failed load token", restored != null && state.transitions().failedTokens().isEmpty());
        check.accept("retry reconstructs actual marker, not remembered count", IsolationRuntime.markers(restored).size() == 1 && state.registry().scope(IsolationRuntime.key(restored)).equals(Scope.privateCraft(IsolationRuntime.key(restored))));
        initialized(stored).transmit(15); initialized(stored.offset(6, 0, 0));

        for (int i = 0; i < 128; i++) restored.getHeatMapManager().tick();
        int beforeSplit = container.getAllSubLevels().size();
        level.removeBlock(stored.offset(3, -1, 0), false);
        for (int i = 0; i < 512 && container.getAllSubLevels().size() == beforeSplit; i++) restored.getHeatMapManager().tick();
        check.accept("actual heatmap creates split child with main lifecycle hooks", container.getAllSubLevels().size() > beforeSplit);
        for (var child : List.copyOf(container.getAllSubLevels())) {
            var markers = IsolationRuntime.markers(child);
            Scope expected = markers.isEmpty() ? Scope.WORLD : Scope.privateCraft(IsolationRuntime.key(child));
            check.accept("split child main registry matches actual blocks " + child.getUniqueId(), state.registry().scope(IsolationRuntime.key(child)).equals(expected));
        }
        var marked = container.getAllSubLevels().stream().filter(child -> !IsolationRuntime.markers(child).isEmpty()).findFirst().orElseThrow();
        BlockPos target = new BlockPos(256, 120, 0);
        var transform = new SubLevelAssemblyHelper.AssemblyTransform(marked.getPlot().getCenterBlock(), target, 0, Rotation.NONE, level);
        var returnedMarkers = IsolationRuntime.markers(marked).stream().map(transform::apply).toList();
        SimAssemblyHelper.disassembleSubLevel(level, marked, marked.getPlot().getCenterBlock(), target, Rotation.NONE, false);
        for (var marker : returnedMarkers) check.accept("disassembled marker returns to WORLD with inactive visual", level.getBlockState(marker).is(FieldworkContent.ISOLATOR.get()) && !level.getBlockState(marker).getValue(IsolatorBlock.ACTIVE));
        check.accept("load retry split and disassembly leave no failed transaction", state.transitions().failedTokens().isEmpty());
        container.processSubLevelRemovals();
    }

    private RedstoneLinkBlockEntity link(BlockPos pos, boolean receiver) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = initialized(pos);
        if (receiver) { level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); entity.tick(); }
        var link = entity.getBehaviour(LinkBehaviour.TYPE);
        link.setFrequency(true, new ItemStack(Items.COPPER_INGOT)); link.setFrequency(false, new ItemStack(Items.IRON_INGOT));
        return entity;
    }
    private RedstoneLinkBlockEntity initialized(BlockPos pos) {
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos);
        if (entity == null) throw new AssertionError("Missing lifecycle link at " + pos);
        entity.tick(); return entity;
    }
    private void signal(String name, RedstoneLinkBlockEntity receiver, int expected) {
        check.accept(name + " expected=" + expected + " actual=" + receiver.getReceivedSignal(), receiver.getReceivedSignal() == expected);
    }
}
