package dev.tassis.createfieldwork.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Real two-craft, multi-chunk startup, lazy BE input and tick-by-tick transition fixture. */
final class MultiRestartProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private final boolean seed;
    private final BlockPos privateOrigin = new BlockPos(0, 120, 0);
    private final BlockPos sharedOrigin = new BlockPos(0, 120, 48);
    private ServerSubLevel isolated;
    private ServerSubLevel shared;
    private BlockPos privateStored;
    private BlockPos sharedStored;
    private JsonObject identity;

    MultiRestartProbe(ServerLevel level, ScopeProbeAssertions checks, String phase) {
        this.level = level;
        this.checks = checks;
        if (!phase.equals("seed-multi") && !phase.equals("reload-multi")) throw new IllegalArgumentException(phase);
        seed = phase.equals("seed-multi");
    }

    boolean tick(int tick) throws Exception {
        if (tick == 20 && seed) {
            isolated = assemble(privateOrigin, true);
            shared = assemble(sharedOrigin, false);
            privateStored = isolated.getPlot().getCenterBlock();
            sharedStored = shared.getPlot().getCenterBlock();
            identity = new JsonObject();
            identity.addProperty("privateUuid", isolated.getUniqueId().toString());
            identity.addProperty("sharedUuid", shared.getUniqueId().toString());
            identity.addProperty("privateStored", privateStored.asLong());
            identity.addProperty("sharedStored", sharedStored.asLong());
            identity.addProperty("jvmStartMillis", ManagementFactory.getRuntimeMXBean().getStartTime());
        } else if (tick == 20) {
            identity = JsonParser.parseString(Files.readString(Path.of("restart-multi-fixture.json"))).getAsJsonObject();
            checks.check("multi restart uses a distinct JVM", identity.get("jvmStartMillis").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
            privateStored = BlockPos.of(identity.get("privateStored").getAsLong());
            sharedStored = BlockPos.of(identity.get("sharedStored").getAsLong());
        }
        if (!seed && (isolated == null || shared == null)) {
            var container = SubLevelContainer.getContainer(level);
            isolated = (ServerSubLevel) container.getSubLevel(UUID.fromString(identity.get("privateUuid").getAsString()));
            shared = (ServerSubLevel) container.getSubLevel(UUID.fromString(identity.get("sharedUuid").getAsString()));
            if ((isolated == null || shared == null) && tick < 100) return false;
            checks.check("both multi-chunk crafts auto-load", isolated != null && shared != null);
            checks.check("private startup scope is ready", ProbeScope.isReady(isolated) && ProbeScope.isPrivate(isolated));
            checks.check("marker-free startup releases fail-closed scope", ProbeScope.isReady(shared) && !ProbeScope.isPrivate(shared));
            checks.check("packed BE input branch exercised", ProbeLifecycle.packedNbtInputs >= 4);
            checks.check("packed BE instances were actually deferred", ProbeLifecycle.packedPositionsObserved > 0);
            checks.check("multiple chunks observed before commit", ProbeLifecycle.initializingChunkChecks >= 6);
        }
        if (tick == 20 || (!seed && tick < 26)) {
            checks.initialized(privateStored);
            checks.initialized(privateStored.offset(3, 0, 0));
            checks.initialized(sharedStored);
            checks.initialized(sharedStored.offset(3, 0, 0));
        }
        if (tick == 26) {
            checks.check("private craft spans at least three actual plot chunks", isolated.getPlot().getLoadedChunks().size() >= 3);
            checks.check("shared craft spans at least three actual plot chunks", shared.getPlot().getLoadedChunks().size() >= 3);
            checks.check("marker-free craft has no marker block", ProbeLifecycle.markers(shared).isEmpty());
            checks.check("all partial chunk callbacks stayed fail-closed", ProbeLifecycle.violations.isEmpty());
            checks.refresh();
        }
        if (tick == 40) marker(privateStored.offset(33, 0, 0), false);
        if (tick == 50) marker(privateStored.offset(33, 0, 0), true);
        if (tick == 60) marker(privateStored.offset(34, 0, 0), true);
        if (tick == 65) marker(privateStored.offset(33, 0, 0), false);
        if (tick == 70) marker(privateStored.offset(34, 0, 0), false);
        if (tick == 75) marker(privateStored.offset(33, 0, 0), true);
        if (tick >= 26 && tick <= 90) {
            int expected = (tick >= 40 && tick < 50) || (tick >= 70 && tick < 75) ? 15 : 0;
            // No refresh here: observe normal game ticks between explicit commits.
            var outsideA = (com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity) level.getBlockEntity(privateOrigin.offset(0, 0, 8));
            var insideA = (com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity) level.getBlockEntity(privateStored.offset(3, 0, 0));
            var outsideB = (com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity) level.getBlockEntity(sharedOrigin.offset(0, 0, 8));
            checks.signal("tick " + tick + " private/shared transition output", outsideA, expected);
            checks.signal("tick " + tick + " internal communication", insideA, 15);
            checks.signal("tick " + tick + " marker-free craft stays upstream", outsideB, 15);
        }
        if (tick < 90) return false;
        checks.check("final private marker identity is one", ProbeLifecycle.markers(isolated).size() == 1 && ProbeScope.isPrivate(isolated));
        if (seed) Files.writeString(Path.of("restart-multi-fixture.json"), identity.toString());
        return true;
    }

    private ServerSubLevel assemble(BlockPos origin, boolean marked) {
        for (int x = 0; x <= 35; x += 8) level.setChunkForced(new ChunkPos(origin.offset(x, 0, 0)).x, new ChunkPos(origin).z, true);
        var source = checks.link(origin, false);
        var receiver = checks.link(origin.offset(3, 0, 0), true);
        var outside = checks.link(origin.offset(0, 0, 8), true);
        if (!marked) {
            for (var entity : List.of(source, receiver, outside)) {
                LinkBehaviour link = entity.getBehaviour(LinkBehaviour.TYPE);
                link.setFrequency(true, new ItemStack(Items.GOLD_INGOT));
                link.setFrequency(false, new ItemStack(Items.IRON_INGOT));
            }
        }
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 35; x++) {
            BlockPos pos = origin.offset(x, -1, 0);
            level.setBlock(pos, x == 0 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
            positions.add(pos);
        }
        positions.add(origin); positions.add(origin.offset(3, 0, 0));
        if (marked) {
            level.setBlock(origin.offset(33, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
            positions.add(origin.offset(33, 0, 0));
        }
        return SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
    }

    private void marker(BlockPos position, boolean present) {
        ProbeLifecycle.begin("tick-marker-mutation", level, List.of(position));
        level.setBlock(position, present ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        ProbeLifecycle.end("tick-marker-mutation", level, true);
    }
}
