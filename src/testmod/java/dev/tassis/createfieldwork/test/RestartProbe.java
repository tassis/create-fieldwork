package dev.tassis.createfieldwork.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

/** Two independent JVMs, with real saved world/holding state between them. */
final class RestartProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private final String phase;
    private final BlockPos origin = new BlockPos(0, 120, 0);
    private ServerSubLevel craft;
    private JsonObject saved;
    private BlockPos stored;
    private final MultiRestartProbe multi;
    private final ControllerProbe controller;
    private final EdgeProbe edge;
    private final ControllerMotionProbe motion;
    private final RecoveryProbe recovery;
    private final FailureRestartProbe failureRestart;
    private final TrainProbe train;
    private final PlayerTrainProbe playerTrain;

    RestartProbe(ServerLevel level, ScopeProbeAssertions checks, String phase) {
        this.level = level;
        this.checks = checks;
        this.phase = phase;
        multi = phase.endsWith("-multi") ? new MultiRestartProbe(level, checks, phase) : null;
        controller = phase.equals("controller") ? new ControllerProbe(level, checks) : null;
        edge = phase.equals("edges") ? new EdgeProbe(level, checks) : null;
        motion = phase.equals("controller-motion") ? new ControllerMotionProbe(level, checks) : null;
        recovery = phase.equals("recovery") ? new RecoveryProbe(level, checks) : null;
        failureRestart = phase.equals("seed-failure") || phase.equals("reload-failure") ? new FailureRestartProbe(level, checks, phase) : null;
        train = phase.equals("train") ? new TrainProbe(level, checks) : null;
        playerTrain = phase.equals("train-player") ? new PlayerTrainProbe(level, checks) : null;
        ProbeLifecycle.afterCommit = checks::refresh;
    }

    boolean tick(int tick) throws Exception {
        if (multi != null) return multi.tick(tick);
        if (controller != null) { controller.run(); return true; }
        if (edge != null) { edge.run(); return true; }
        if (motion != null) return motion.tick(tick);
        if (recovery != null) { recovery.run(); return true; }
        if (failureRestart != null) return failureRestart.tick(tick);
        if (train != null) { train.run(); return true; }
        if (playerTrain != null) return playerTrain.tick(tick);
        if (!phase.equals("seed") && !phase.equals("reload")) throw new IllegalArgumentException("Unknown probe phase " + phase);
        if (tick == 20 && phase.equals("seed")) {
            level.setChunkForced(new ChunkPos(origin).x, new ChunkPos(origin).z, true);
            checks.link(origin, false);
            checks.link(origin.offset(3, 0, 0), true);
            checks.link(origin.offset(8, 0, 0), true);
            level.setBlock(origin.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
            var blocks = new ArrayList<BlockPos>();
            for (int x = 0; x <= 3; x++) {
                BlockPos pos = origin.offset(x, -1, 0);
                if (x != 0) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                blocks.add(pos);
            }
            blocks.add(origin); blocks.add(origin.offset(3, 0, 0)); blocks.add(origin.offset(1, 0, 0));
            craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
            stored = craft.getPlot().getCenterBlock();
            checks.initialized(stored);
            checks.initialized(stored.offset(3, 0, 0));
            checks.check("seed actual craft has marker", ProbeLifecycle.markers(craft).size() == 1);
            saved = new JsonObject();
            saved.addProperty("uuid", craft.getUniqueId().toString());
            saved.addProperty("stored", stored.asLong());
            saved.addProperty("jvmStartMillis", ManagementFactory.getRuntimeMXBean().getStartTime());
        } else if (tick == 20) {
            saved = JsonParser.parseString(Files.readString(Path.of("restart-fixture.json"))).getAsJsonObject();
            checks.check("reload is a different JVM", saved.get("jvmStartMillis").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
            stored = BlockPos.of(saved.get("stored").getAsLong());
        }
        if (phase.equals("reload") && craft == null) {
            craft = (ServerSubLevel) SubLevelContainer.getContainer(level).getSubLevel(UUID.fromString(saved.get("uuid").getAsString()));
            if (craft == null && tick < 100) return false;
            checks.check("saved craft automatically loads without a client", craft != null);
            checks.check("saved UUID preserved", craft.getUniqueId().toString().equals(saved.get("uuid").getAsString()));
            checks.check("startup load wrapper reconstructed scope", ProbeScope.isReady(craft));
            checks.check("saved marker block identity restored", ProbeLifecycle.markers(craft).size() == 1);
            checks.initialized(stored);
            checks.initialized(stored.offset(3, 0, 0));
        }
        if (tick < 60) return false;
        var source = checks.initialized(stored);
        AllBlocks.REDSTONE_LINK.get().updateTransmittedSignal(source.getBlockState(), level, stored);
        checks.check(phase + " real redstone source is still 15 after ticks", source.getSignal() == 15);
        checks.refresh();
        checks.signal(phase + " private external signal stays zero across ticks", checks.initialized(origin.offset(8, 0, 0)), 0);
        checks.signal(phase + " internal signal survives ticks/physics", checks.initialized(stored.offset(3, 0, 0)), 15);
        checks.check(phase + " private identity is reconstructed", ProbeScope.isPrivate(craft));
        if (phase.equals("seed")) Files.writeString(Path.of("restart-fixture.json"), saved.toString());
        return true;
    }
}
