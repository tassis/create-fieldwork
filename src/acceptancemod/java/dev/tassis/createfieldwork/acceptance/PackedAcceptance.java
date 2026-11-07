package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Real serializer lazy-BE branch with keepPacked input flags; main owns all publication decisions. */
final class PackedAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    PackedAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }

    void run() {
        exercise(false, new BlockPos(0, 120, 0));
        exercise(true, new BlockPos(64, 120, 0));
    }

    private void exercise(boolean isolated, BlockPos origin) {
        var state = FieldworkStateAccess.get(level.getServer());
        var container = SubLevelContainer.getContainer(level);
        level.setBlock(origin.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(origin, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var source = (RedstoneLinkBlockEntity) level.getBlockEntity(origin); source.tick();
        source.getBehaviour(LinkBehaviour.TYPE).setFrequency(true, new ItemStack(Items.DIAMOND));
        source.getBehaviour(LinkBehaviour.TYPE).setFrequency(false, new ItemStack(Items.EMERALD));
        var blocks = new ArrayList<BlockPos>(List.of(origin, origin.below()));
        if (isolated) {
            level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
            blocks.add(origin.offset(1, 0, 0));
        }
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        var stored = craft.getPlot().getCenterBlock();
        source = (RedstoneLinkBlockEntity) level.getBlockEntity(stored); source.tick(); source.transmit(15);
        var data = SubLevelSerializer.toData(craft, List.of());
        int flags = 0;
        var chunks = data.fullTag().getCompound("plot").getCompound("chunks");
        for (String key : chunks.getAllKeys()) for (var entry : chunks.getCompound(key).getList("block_entities", 10)) {
            ((CompoundTag) entry).putBoolean("keepPacked", true); flags++;
        }
        check.accept("packed fixture actually flags saved block entities isolated=" + isolated, flags > 0);
        container.removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
        var loaded = SubLevelSerializer.fullyLoad(level, data);
        check.accept("real serializer restores packed fixture isolated=" + isolated, loaded != null);
        var scope = state.registry().scope(IsolationRuntime.key(loaded));
        check.accept("packed marker identity, not BE count, determines scope isolated=" + isolated,
            scope.equals(isolated ? Scope.privateCraft(IsolationRuntime.key(loaded)) : Scope.WORLD));
        if (isolated) {
            check.accept("unregistered PRIVATE source is not an unknown WORLD source", !state.publications().pendingWorld());
            check.accept("WORLD train LOW treats packed PRIVATE-only as invisible", condition(true).tickCompletion(level, null, new CompoundTag()));
        } else {
            check.accept("packed WORLD source remains pending before registration", state.publications().pendingWorld());
            check.accept("WORLD train LOW waits through packed registration gap", !condition(true).tickCompletion(level, null, new CompoundTag()));
        }
        source = (RedstoneLinkBlockEntity) level.getBlockEntity(stored); source.tick(); source.transmit(15);
        check.accept("packed publication resolves without lingering unknown WORLD flag", !state.publications().pendingWorld());
        check.accept("WORLD train HIGH obeys actual registered packed source scope isolated=" + isolated,
            condition(false).tickCompletion(level, null, new CompoundTag()) == !isolated);
        container.removeSubLevel(loaded, SubLevelRemovalReason.REMOVED);
        container.processSubLevelRemovals();
        check.accept("packed load leaves no failed token isolated=" + isolated, state.transitions().failedTokens().isEmpty());
    }

    private RedstoneLinkCondition condition(boolean low) {
        var condition = new RedstoneLinkCondition();
        condition.setItem(0, new ItemStack(Items.DIAMOND)); condition.setItem(1, new ItemStack(Items.EMERALD));
        condition.getData().putInt("Inverted", low ? 1 : 0);
        return condition;
    }
}
