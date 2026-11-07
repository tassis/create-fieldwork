package dev.tassis.createfieldwork.test;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class RecoveryProbe {
    record Fixture(ServerSubLevel craft, BlockPos origin, BlockPos stored, BlockPos outside, long token) {}
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    RecoveryProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    void run() {
        ProbeLifecycle.afterCommit = checks::refresh;
        Fixture a = failed(level, checks, new BlockPos(0, 120, 0), 1, false);
        checks.check("failed A owns one named token", a.token > 0 && ProbeLifecycle.pendingTokens().contains(a.token));
        checks.signal("failed A external power stays zero", checks.initialized(a.outside), 0);
        checks.check("unrepaired A cannot release its token", !ProbeLifecycle.recover(a.token));

        Fixture b = assembled(level, checks, new BlockPos(512, 120, 0), 0);
        checks.check("unrelated B can commit while A failed", ProbeScope.isReady(b.craft) && !ProbeScope.isPrivate(b.craft));
        checks.signal("unrelated B retains world communication", checks.initialized(b.outside), 15);
        checks.check("B success retains only A token", ProbeLifecycle.pendingTokens().equals(Set.of(a.token)));
        checks.signal("B success does not release A", checks.initialized(a.outside), 0);

        level.setBlock(a.stored.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        checks.check("named A repair validates and saves before release", ProbeLifecycle.recover(a.token));
        checks.check("A repair clears A token only", ProbeLifecycle.pendingTokens().isEmpty());
        checks.check("A repaired identity is READY private", ProbeScope.isReady(a.craft) && ProbeScope.isPrivate(a.craft));
        checks.signal("A repair retains private boundary", checks.initialized(a.outside), 0);
        checks.signal("A repair does not disrupt B", checks.initialized(b.outside), 15);

        Fixture multi = failed(level, checks, new BlockPos(64, 120, 0), 2, false);
        checks.check("multi-marker partial write leaves one physical marker", ProbeLifecycle.markers(multi.craft).size() == 1);
        checks.check("remaining marker cannot hide missing transfer", !ProbeScope.isReady(multi.craft) && !ProbeLifecycle.recover(multi.token));
        level.setBlock(multi.stored.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        checks.check("all expected markers required for named recovery", ProbeLifecycle.recover(multi.token) && ProbeLifecycle.markers(multi.craft).size() == 2);

        Fixture duplicate = failed(level, checks, new BlockPos(96, 120, 0), 1, true);
        checks.check("source-clear failure leaves duplicate marker identities", level.getBlockState(duplicate.origin.offset(1, 0, 0)).is(Blocks.OBSIDIAN)
            && level.getBlockState(duplicate.stored.offset(1, 0, 0)).is(Blocks.OBSIDIAN));
        checks.check("duplicate source prevents recovery", !ProbeLifecycle.recover(duplicate.token));
        level.setBlock(duplicate.origin.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 3);
        checks.check("verified source removal permits named recovery", ProbeLifecycle.recover(duplicate.token));
        checks.check("all independent recovery tokens released", ProbeLifecycle.pendingTokens().isEmpty());
        for (var craft : List.copyOf(SubLevelContainer.getContainer(level).getAllSubLevels()))
            SubLevelContainer.getContainer(level).removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
    }

    static Fixture failed(ServerLevel level, ScopeProbeAssertions checks, BlockPos origin, int markers, boolean sourceClear) {
        Set<Long> before = ProbeLifecycle.pendingTokens();
        ProbeLifecycle.skipMarkerWriteOnce = !sourceClear;
        ProbeLifecycle.skipMarkerSourceClearOnce = sourceClear;
        Fixture f = assembled(level, checks, origin, markers);
        long token = ProbeLifecycle.pendingTokens().stream().filter(id -> !before.contains(id)).findFirst().orElseThrow();
        return new Fixture(f.craft, origin, f.stored, f.outside, token);
    }

    static Fixture assembled(ServerLevel level, ScopeProbeAssertions checks, BlockPos origin, int markers) {
        level.setChunkForced(new ChunkPos(origin).x, new ChunkPos(origin).z, true);
        var source = checks.link(origin, false);
        checks.link(origin.offset(3, 0, 0), true);
        BlockPos outside = origin.offset(0, 0, 8);
        checks.link(outside, true);
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            level.setBlock(origin.offset(x, -1, 0), x == 0 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
            blocks.add(origin.offset(x, -1, 0));
        }
        source.transmit(15);
        for (int x = 1; x <= markers; x++) {
            level.setBlock(origin.offset(x, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
            blocks.add(origin.offset(x, 0, 0));
        }
        blocks.add(origin); blocks.add(origin.offset(3, 0, 0));
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        BlockPos stored = craft.getPlot().getCenterBlock();
        checks.initialized(stored).transmit(15);
        checks.initialized(stored.offset(3, 0, 0));
        checks.refresh();
        return new Fixture(craft, origin, stored, outside, -1);
    }
}
