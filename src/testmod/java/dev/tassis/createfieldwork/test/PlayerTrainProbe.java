package dev.tassis.createfieldwork.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackShape;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import java.util.*;

/** Normal rail graph and StationBlockEntity.assemble, no hand-made Train or tracking assignment.
 * TargetTrack NBT stands in for station placement UI; not a client interaction test. */
final class PlayerTrainProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private final List<Fixture> fixtures = new ArrayList<>();
    private record Fixture(String name, BlockPos track, BlockPos station, ServerSubLevel craft) {}

    PlayerTrainProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    boolean tick(int tick) {
        if (tick == 20) {
            ProbeTrain.enabled = true;
            fixtures.add(place("WORLD", new BlockPos(0, 120, 0), false));
            fixtures.add(place("PRIVATE", new BlockPos(64, 120, 0), true));
        }
        if (tick == 60) {
            for (var f : fixtures) {
                var station = (StationBlockEntity) level.getBlockEntity(f.station);
                checks.check(f.name + " station block survives ticks: " + level.getBlockState(f.station), station != null);
                checks.check(f.name + " station has real graph edge point", station.getStation() != null);
                var direction = station.getAssemblyDirection();
                checks.check(f.name + " station resolves rail direction", direction != null);
                BlockPos bogey = f.track.relative(direction).above();
                level.setBlock(bogey, AllBlocks.SMALL_BOGEY.getDefaultState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, direction.getAxis()), 3);
                level.setBlock(bogey.above(), AllBlocks.TRAIN_CONTROLS.getDefaultState()
                    .setValue(HorizontalDirectionalBlock.FACING, direction), 3);
                checks.check(f.name + " actual glue joins bogey and controls", level.addFreshEntity(
                    new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(level,
                        com.simibubi.create.content.contraptions.glue.SuperGlueEntity.span(bogey, bogey.above()))));
                checks.check(f.name + " enters normal station assembly mode", station.tryEnterAssemblyMode());
                int before = Create.RAILWAYS.trains.size();
                station.assemble(UUID.randomUUID());
                checks.check(f.name + " normal station registers actual train; station=" + station.saveWithFullMetadata(level.registryAccess()), Create.RAILWAYS.trains.size() == before + 1);
            }
        }
        if (tick < 100) return false;
        for (Train train : Create.RAILWAYS.trains.values()) {
            var carriage = train.carriages.getFirst();
            var state = carriage.getDimensionalIfPresent(level.dimension());
            var entity = carriage.anyAvailableEntity();
            var tracking = entity == null ? null : Sable.HELPER.getTrackingSubLevel(entity);
            var row = new LinkedHashMap<String, Object>();
            row.put("stage", "station-after-40-ticks");
            row.put("train", train.id.toString());
            row.put("anchor", String.valueOf(state.positionAnchor));
            row.put("entityPosition", entity == null ? "offline" : entity.position().toString());
            row.put("tracking", tracking == null ? "none" : tracking.getUniqueId().toString());
            row.put("membership", ProbeTrain.membership(level, train).toString());
            ProbeTrain.observations.add(row);
            checks.check("station train retains available carriage entity", entity != null);
        }
        checks.check("two station assembled trains survive normal ticks", Create.RAILWAYS.trains.size() == 2);
        ProbeTrain.enabled = false;
        return true;
    }

    private Fixture place(String name, BlockPos origin, boolean privateCraft) {
        var blocks = new ArrayList<BlockPos>();
        for (int z = -24; z <= 24; z++) {
            BlockPos track = origin.offset(0, 0, z);
            var chunk = new ChunkPos(track);
            level.setChunkForced(chunk.x, chunk.z, true);
            level.setBlock(track.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(track, AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, TrackShape.ZO), 3);
            blocks.add(track.below()); blocks.add(track);
        }
        BlockPos stationPos = origin.offset(3, 0, 0);
        for (int x = 1; x <= 4; x++) {
            BlockPos beam = origin.offset(x, -1, 0);
            level.setBlock(beam, Blocks.STONE.defaultBlockState(), 3);
            blocks.add(beam);
        }
        level.setBlock(stationPos, AllBlocks.TRACK_STATION.getDefaultState(), 3);
        blocks.add(stationPos);
        BlockPos marker = origin.offset(4, 0, 0);
        level.setBlock(marker, Blocks.OBSIDIAN.defaultBlockState(), 3);
        blocks.add(marker);
        ServerSubLevel craft = null;
        if (privateCraft) {
            craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
            origin = craft.getPlot().getCenterBlock();
            stationPos = origin.offset(3, 0, 0);
        }
        // Keep all track/BE chunks ticking with the standard vanilla force-load API.
        for (int z = -24; z <= 24; z += 8) {
            var chunk = new ChunkPos(origin.offset(0, 0, z));
            level.setChunkForced(chunk.x, chunk.z, true);
        }
        var station = (StationBlockEntity) level.getBlockEntity(stationPos);
        station.initialize();
        var tag = new CompoundTag();
        station.edgePoint.write(tag, level.registryAccess(), false);
        tag.put("TargetTrack", NbtUtils.writeBlockPos(origin.subtract(stationPos)));
        tag.putBoolean("TargetDirection", true);
        station.edgePoint.read(tag, level.registryAccess(), false);
        return new Fixture(name, origin, stationPos, craft);
    }
}
