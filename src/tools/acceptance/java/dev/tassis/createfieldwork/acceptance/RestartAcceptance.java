package dev.tassis.createfieldwork.acceptance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Independent JVM persistence using real main blocks/registry/hooks and normal Sable loading. */
final class RestartAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    private final String phase;
    private JsonObject metadata;

    RestartAcceptance(ServerLevel level, BiConsumer<String, Boolean> check, String phase) {
        this.level = level; this.check = check; this.phase = phase;
    }

    boolean tick(int tick) throws Exception {
        if (tick == 20) {
            if (phase.equals("seed")) {
                metadata = new JsonObject();
                metadata.addProperty("jvm", ManagementFactory.getRuntimeMXBean().getStartTime());
                var a = craft(new BlockPos(0, 120, 0), true);
                var b = craft(new BlockPos(96, 120, 0), false);
                metadata.addProperty("a", a.getUniqueId().toString());
                metadata.addProperty("b", b.getUniqueId().toString());
                metadata.addProperty("storedA", a.getPlot().getCenterBlock().asLong());
                metadata.addProperty("storedB", b.getPlot().getCenterBlock().asLong());
            } else {
                metadata = JsonParser.parseString(Files.readString(Path.of("restart-main.json"))).getAsJsonObject();
                check.accept("main reload occurs in a different JVM", metadata.get("jvm").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
            }
        }
        if (tick < 60) return false;
        var container = SubLevelContainer.getContainer(level);
        var a = (ServerSubLevel) container.getSubLevel(UUID.fromString(metadata.get("a").getAsString()));
        var b = (ServerSubLevel) container.getSubLevel(UUID.fromString(metadata.get("b").getAsString()));
        if ((a == null || b == null) && tick < 200) return false;
        check.accept(phase + " both saved UUIDs automatically loaded", a != null && b != null);
        var state = FieldworkStateAccess.get(level.getServer());
        for (var craft : java.util.List.of(a, b)) {
            check.accept(phase + " actual craft spans at least three plot chunks", craft.getPlot().getLoadedChunks().size() >= 3);
            var snapshot = state.registry().snapshot(IsolationRuntime.key(craft));
            check.accept(phase + " main registry reconstructs both actual markers", snapshot.positions().size() == 2);
            check.accept(phase + " same saved craft remains private", state.registry().scope(IsolationRuntime.key(craft)).equals(Scope.privateCraft(IsolationRuntime.key(craft))));
            for (var marker : snapshot.positions()) check.accept(phase + " marker visual normalized ACTIVE", level.getBlockState(marker).getValue(IsolatorBlock.ACTIVE));
        }
        BlockPos storedA = BlockPos.of(metadata.get("storedA").getAsLong());
        BlockPos storedB = BlockPos.of(metadata.get("storedB").getAsLong());
        check.accept(phase + " natural REDSTONE_BLOCK source remains15", ((RedstoneLinkBlockEntity) level.getBlockEntity(storedA)).getSignal() == 15);
        signal(phase + " normal ticks preserve internal A15", storedA.offset(35, 0, 0), 15);
        signal(phase + " private A cannot feed private B", storedB.offset(35, 0, 0), 0);
        signal(phase + " private A cannot feed world", new BlockPos(0, 120, 8), 0);
        check.accept(phase + " normal multi-chunk load leaves no failed tokens", state.transitions().failedTokens().isEmpty());
        if (phase.equals("seed")) Files.writeString(Path.of("restart-main.json"), metadata.toString());
        return true;
    }

    private ServerSubLevel craft(BlockPos origin, boolean powered) {
        for (int x = 0; x <= 35; x += 8) {
            var chunk = new ChunkPos(origin.offset(x, 0, 0));
            level.setChunkForced(chunk.x, chunk.z, true);
        }
        link(origin, false); link(origin.offset(35, 0, 0), true);
        if (powered) link(origin.offset(0, 0, 8), true);
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 35; x++) {
            BlockPos floor = origin.offset(x, -1, 0);
            level.setBlock(floor, x == 0 && powered ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
            blocks.add(floor);
        }
        for (int x : new int[]{1, 34}) {
            var marker = origin.offset(x, 0, 0);
            level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
            blocks.add(marker);
        }
        blocks.add(origin); blocks.add(origin.offset(35, 0, 0));
        return SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
    }

    private void link(BlockPos pos, boolean receiver) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos); entity.tick();
        if (receiver) { level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); entity.tick(); }
        var link = entity.getBehaviour(LinkBehaviour.TYPE);
        link.setFrequency(true, new ItemStack(Items.DIAMOND)); link.setFrequency(false, new ItemStack(Items.EMERALD));
    }
    private void signal(String name, BlockPos pos, int expected) {
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos);
        int actual = entity == null ? -1 : entity.getReceivedSignal();
        check.accept(name + " expected=" + expected + " actual=" + actual, actual == expected);
    }
}
