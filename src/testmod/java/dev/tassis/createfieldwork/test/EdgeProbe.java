package dev.tassis.createfieldwork.test;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Adversarial but real network consumer / lifecycle paths; not a shipped mod. */
final class EdgeProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    EdgeProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    void run() {
        ProbeLifecycle.afterCommit = checks::refresh;
        stale();
        failedMarker(false, new BlockPos(128, 120, 0));
        failedMarker(true, new BlockPos(160, 120, 0));
    }

    private void stale() {
        BlockPos origin = new BlockPos(0, 120, 0);
        var originalReceiver = checks.simulated(origin.offset(3, 0, 0), SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST));
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            level.setBlock(origin.offset(x, -1, 0), Blocks.STONE.defaultBlockState(), 3);
            positions.add(origin.offset(x, -1, 0));
        }
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(3, 0, 0));
        var container = SubLevelContainer.getContainer(level);
        ServerSubLevel a = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        BlockPos stored = a.getPlot().getCenterBlock();
        var receiver = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0));
        receiver.tick();
        var fake = new Endpoint(stored, receiver.getBehaviour(LinkBehaviour.TYPE).getNetworkKey());
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, fake);
        receiver.updateSignal();
        checks.simulatedSignal("live non-BE endpoint participates in private network", receiver, 15);
        fake.alive = false;
        ProbeEndpointGuard.enabled = false;
        receiver.updateSignal();
        checks.simulatedSignal("negative control Simulated reads dead cached strength", receiver, 15);
        ProbeEndpointGuard.enabled = true;
        receiver.updateSignal();
        checks.simulatedSignal("restricted consumer rejects dead non-BE endpoint", receiver, 0);

        var data = SubLevelSerializer.toData(a, List.of());
        var oldId = a.getUniqueId();
        container.removeSubLevel(a, SubLevelRemovalReason.REMOVED);
        UUID newId = UUID.randomUUID();
        var replacementData = new SubLevelData(newId, data.bounds(), data.pose(), data.dependencies(), data.fullTag().copy());
        var b = SubLevelSerializer.fullyLoad(level, replacementData);
        checks.check("plot reuse exact position with different UUID", b != null && !newId.equals(oldId) && b.getPlot().getCenterBlock().equals(stored)
            && Sable.HELPER.getContaining(level, stored).getUniqueId().equals(newId));
        receiver = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0));
        receiver.tick();
        // Explicit adversarial residual bucket entry, not a claim unload normally
        // leaves one. addToNetwork would immediately clean a dead entry.
        Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(level, fake).add(fake);
        ProbeEndpointGuard.enabled = false;
        receiver.updateSignal();
        checks.simulatedSignal("negative control reused plot misattributes stale endpoint", receiver, 15);
        ProbeEndpointGuard.enabled = true;
        receiver.updateSignal();
        checks.simulatedSignal("restricted liveness guard closes stale plot-reuse path", receiver, 0);
        Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, receiver.getBehaviour(LinkBehaviour.TYPE));
        checks.check("Create updater independently cleans dead endpoint", !Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(level, fake).contains(fake));

        var worldReceiver = checks.simulated(new BlockPos(68, 120, 0), SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST));
        var worldFake = new Endpoint(new BlockPos(64, 120, 0), worldReceiver.getBehaviour(LinkBehaviour.TYPE).getNetworkKey());
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, worldFake);
        worldFake.alive = false;
        int aliveReads = worldFake.aliveReads;
        worldReceiver.updateSignal();
        checks.simulatedSignal("unrestricted consumer preserves upstream stale behavior", worldReceiver, 15);
        checks.check("unrestricted guard does not add liveness behavior", worldFake.aliveReads == aliveReads);
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, worldFake);
        container.removeSubLevel(b, SubLevelRemovalReason.REMOVED);
        ProbeEndpointGuard.enabled = false;
    }

    private void failedMarker(boolean guarded, BlockPos origin) {
        var source = checks.link(origin, false);
        var outside = checks.link(origin.offset(0, 0, 8), true);
        source.transmit(15);
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        List<BlockPos> positions = List.of(origin, origin.below(), origin.offset(1, 0, 0));
        ProbeLifecycle.checkMarkerTransfers = guarded;
        ProbeLifecycle.skipMarkerWriteOnce = true;
        int commits = ProbeLifecycle.commits;
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        checks.check((guarded ? "guarded" : "negative") + " per-block failure was swallowed by upstream normal return", craft != null && !ProbeLifecycle.skipMarkerWriteOnce);
        checks.check((guarded ? "guarded" : "negative") + " marker really failed to arrive", ProbeLifecycle.markers(craft).isEmpty());
        checks.initialized(craft.getPlot().getCenterBlock()).transmit(15);
        checks.refresh();
        if (!guarded) {
            checks.signal("negative control identity-only rebuild exposes failed marker transfer", outside, 15);
            checks.check("negative control incorrectly success-commits", ProbeLifecycle.commits == commits + 1);
        } else {
            checks.signal("marker transfer ledger keeps partial failure closed", outside, 0);
            checks.check("partial transfer does not publish READY empty", !ProbeScope.isReady(craft) && ProbeScope.isPrivate(craft) && ProbeLifecycle.commits == commits);
            ProbeLifecycle.begin("unrelated-success", level, List.of(new BlockPos(500, 120, 500)));
            ProbeLifecycle.end("unrelated-success", level, true);
            checks.signal("unrelated success cannot release failed quarantine", outside, 0);
            checks.check("failed commit explicitly recorded", ProbeLifecycle.withheldCommits > 0);
        }
        ProbeLifecycle.checkMarkerTransfers = true;
        if (guarded) ProbeLifecycle.enabled = false; // teardown only, not recovery proof
        SubLevelContainer.getContainer(level).removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
    }

    private static final class Endpoint implements IRedstoneLinkable {
        private final BlockPos position;
        private final Couple<Frequency> key;
        private boolean alive = true;
        private int aliveReads;
        Endpoint(BlockPos position, Couple<Frequency> key) { this.position = position; this.key = key; }
        @Override public int getTransmittedStrength() { return 15; }
        @Override public void setReceivedStrength(int strength) {}
        @Override public boolean isListening() { return false; }
        @Override public boolean isAlive() { aliveReads++; return alive; }
        @Override public Couple<Frequency> getNetworkKey() { return key; }
        @Override public BlockPos getLocation() { return position; }
    }
}
