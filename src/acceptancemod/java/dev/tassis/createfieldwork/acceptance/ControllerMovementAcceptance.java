package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.tassis.createfieldwork.controller.routing.ControllerEndpoint;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** Real input/listener, identical frequency on A, B and WORLD, with per-recipient cold counters. */
final class ControllerMovementAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    private final Couple<Frequency> key = Couple.create(Frequency.of(new ItemStack(Items.AMETHYST_SHARD)), Frequency.of(new ItemStack(Items.LAPIS_LAZULI)));
    private ServerSubLevel a, b;
    private BlockPos stored, bStored, bReceiverPos, bSimPos, worldReceiverPos, worldSimPos;
    private RedstoneLinkBlockEntity own, other, worldReceiver;
    private AbstractLinkedReceiverBlockEntity ownSim, otherSim, worldSim, bWorldSim;

    ControllerMovementAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    private FieldworkServerState state() { return FieldworkStateAccess.get(level.getServer()); }

    void run() throws Exception {
        ReceiverSelectionAcceptance.run(check);
        setup();
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MovementCase"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        var listener = new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        var item = AllItems.LINKED_CONTROLLER.asStack();
        item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        BlockPos lecternPos = stored.offset(2, 0, 0);
        LecternControllerBlockEntity lectern = null;
        try {
            actor.setGameMode(GameType.SURVIVAL);
            var world = a.logicalPose().transformPosition(new Vector3d(stored.getX() + .5, stored.getY() + .25, stored.getZ() + .5));
            actor.setPos(world.x, world.y, world.z); level.addNewPlayer(actor); listener.resetPosition();
            actor.setItemInHand(InteractionHand.MAIN_HAND, item);
            actor.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -.5, 0));
            plot(listener, .5, .1, .5, true);
            press(actor); state().refresh().refresh(null);
            signal("ordinary handheld on A cannot address private A", own, 0);
            signal("same-frequency B retains independent9", other, 9);
            signal("ordinary handheld on A remains a WORLD source", worldReceiver, 15);
            check.accept("ordinary handheld on A cannot address private A Simulated", ownSim.receivedSignal == 0);
            check.accept("same-frequency B Simulated starts with legal9", otherSim.receivedSignal == 9);
            check.accept("ordinary handheld on A reaches WORLD Simulated with native attenuation expected=11 actual=" + worldSim.receivedSignal,
                worldSim.receivedSignal == 11);

            var bWorld = b.logicalPose().transformPosition(new Vector3d(bStored.getX() + .5, bStored.getY() + .25, bStored.getZ() + .5));
            var movementListener = listener;
            var teleportStart = ControllerWorkMetrics.measure(() -> movementListener.teleport(bWorld.x, bWorld.y, bWorld.z, 0, 0));
            check.accept("same-dimension teleport start performs zero targeted controller work",
                teleportStart.endpointReconciliations() == 0 && teleportStart.fullSweeps() == 0);
            var teleportAck = ControllerWorkMetrics.measure(() -> {
                try { ControllerAcceptance.acknowledge(movementListener); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
            check.accept("same-dimension teleport acknowledgement performs zero targeted controller work",
                teleportAck.endpointReconciliations() == 0 && teleportAck.fullSweeps() == 0);
            actor.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -.5, 0)); listener.resetPosition();
            var acceptedMove = ControllerWorkMetrics.measure(() -> plot(movementListener, bStored, .5, .1, .5, true));
            check.accept("accepted movement performs zero targeted controller work",
                acceptedMove.endpointReconciliations() == 0 && acceptedMove.fullSweeps() == 0);
            check.accept("ordinary handheld fixture actually boards private craft B", dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(actor) == b);
            press(actor); state().refresh().refresh(null);
            signal("ordinary handheld on B cannot address private A", own, 0);
            signal("ordinary handheld on B cannot override private B", other, 9);
            signal("ordinary handheld on B remains a WORLD source", worldReceiver, 15);
            check.accept("ordinary handheld on B cannot address private A/B and reaches nearby WORLD Simulated",
                ownSim.receivedSignal == 0 && otherSim.receivedSignal == 9
                    && worldSim.receivedSignal == 7 && bWorldSim.receivedSignal == 11);

            listener.teleport(world.x, world.y, world.z, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -.5, 0)); listener.resetPosition();
            plot(listener, stored, .5, .1, .5, true);
            var beforeRejected = actor.position();
            var rejectedMove = ControllerWorkMetrics.measure(() -> movementListener.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(beforeRejected.x + 5000, beforeRejected.y, beforeRejected.z, true)));
            check.accept("rejected movement fixture is corrected by vanilla", ControllerAcceptance.awaitingPosition(listener) != null &&
                actor.position().distanceToSqr(beforeRejected) < .01);
            check.accept("rejected movement performs zero targeted controller work",
                rejectedMove.endpointReconciliations() == 0 && rejectedMove.fullSweeps() == 0);
            ControllerAcceptance.acknowledge(listener);
            press(actor);
            RefreshMetrics.measure(() -> state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), key)));
            fullFrequency("full-frequency counter negative control");
            var unchanged = actor;
            RefreshMetrics.measure(() -> press(unchanged));
            check.accept("unchanged ordinary WORLD input does not invoke Fieldwork receiver refresh", RefreshMetrics.total() == 0);

            // A source-kind change within PRIVATE(A), with identical key and
            // position, is NOT a position-only optimization candidate.
            level.setBlock(lecternPos, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
            lectern = (LecternControllerBlockEntity) level.getBlockEntity(lecternPos); lectern.setController(item.copy());
            var beside = a.logicalPose().transformPosition(new Vector3d(stored.getX() + 2.5, stored.getY() + .1, stored.getZ() + 1.5));
            listener.teleport(beside.x, beside.y, beside.z, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -.5, 0)); listener.resetPosition();
            plot(listener, 2.5, 0, 1.5, true); press(actor);
            lectern.tryStartUsing(actor);
            check.accept("same-craft lectern validates actual active operator", lectern.isUsedBy(actor));
            var entry = entry(actor); var samePosition = entry.getLocation();
            var sameActor = actor;
            RefreshMetrics.measure(() -> new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(sameActor));
            check.accept("handheld-to-lectern provenance change has unchanged position", entry.getLocation().equals(samePosition));
            fullFrequency("same-position source-kind change");
            signal("provenance fallback keeps same-craft15", own, 15);
            signal("ordinary lectern on A does not broadcast WORLD", worldReceiver, 7);
            check.accept("ordinary lectern on A powers only A Simulated own=" + ownSim.receivedSignal
                    + " other=" + otherSim.receivedSignal + " world=" + worldSim.receivedSignal,
                ownSim.receivedSignal == 6 && otherSim.receivedSignal == 9 && worldSim.receivedSignal == 7);
            reentrantSelection(false);
            reentrantSelection(true);

            // New actor/session, same UUID, lectern, PRIVATE(A), key and position.
            UUID id = actor.getUUID(); var oldSession = ((ControllerEndpoint) entry).fieldwork$source().session();
            var location = actor.position();
            lectern.tryStopUsing(actor); lectern.tick();
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED); transport.embedded.finishAndReleaseAll();
            actor = new ServerPlayer(level.getServer(), level, new GameProfile(id, "MovementCase"), ClientInformation.createDefault());
            transport = new ControllerAcceptance.Transport();
            listener = new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            actor.setPos(location.x, location.y, location.z); actor.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
            level.addNewPlayer(actor); listener.resetPosition();
            lectern.tryStartUsing(actor);
            check.accept("replacement session obtains real lectern authorization", lectern.isUsedBy(actor));
            var replacement = actor;
            RefreshMetrics.measure(() -> new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(replacement));
            check.accept("same-position session replacement receives a new server session", entry.getLocation().equals(samePosition)
                && !oldSession.equals(((ControllerEndpoint) entry).fieldwork$source().session()));
            fullFrequency("same-position session change");
            signal("session fallback does not erase unrelated private9", other, 9);
            signal("session fallback does not erase unrelated WORLD7", worldReceiver, 7);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            var activeAgain = actor;
            RefreshMetrics.measure(() -> new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(activeAgain));
            fullFrequency("same-position input liveness change");
        } finally {
            transport.connected = true;
            if (lectern != null && lectern.isUsedBy(actor)) {
                new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor); lectern.tryStopUsing(actor);
            } else new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED); transport.embedded.finishAndReleaseAll();
        }
        check.accept("controller movement tests leave no failed tokens", state().transitions().failedTokens().isEmpty());
    }

    private void setup() {
        var origin = new BlockPos(32, 120, 0);
        link(origin.offset(3, 0, 1), true); simulated(origin.offset(3, 0, 0));
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) for (int z = 0; z <= 1; z++) {
            var floor = origin.offset(x, -1, z); level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3); positions.add(floor);
        }
        positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(3, 0, 0)); positions.add(origin.offset(3, 0, 1));
        a = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        stored = a.getPlot().getCenterBlock(); own = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.offset(3, 0, 1)); own.tick();
        ownSim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0)); ownSim.tick();

        var bOrigin = origin.offset(0, 0, 32);
        link(bOrigin.offset(3, 0, 0), false); link(bOrigin.offset(2, 0, 0), true);
        var bSim = simulated(bOrigin.offset(1, 0, 0));
        level.setBlock(bSim.getBlockPos(), bSim.getBlockState().setValue(DirectionalBlock.FACING, Direction.EAST), 3);
        level.setBlock(bOrigin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var bBlocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            var floor = bOrigin.offset(x, -1, 0); level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3); bBlocks.add(floor);
            bBlocks.add(bOrigin.offset(x, 0, 0));
        }
        b = SubLevelAssemblyHelper.assembleBlocks(level, bOrigin, bBlocks, BoundingBox3i.from(bBlocks));
        bStored = b.getPlot().getCenterBlock();
        var source = (RedstoneLinkBlockEntity) level.getBlockEntity(bStored.offset(3, 0, 0)); source.tick(); source.transmit(9);
        bReceiverPos = bStored.offset(2, 0, 0); bSimPos = bStored.offset(1, 0, 0);
        other = (RedstoneLinkBlockEntity) level.getBlockEntity(bReceiverPos); other.tick();
        otherSim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(bSimPos); otherSim.tick();
        bWorldSim = simulated(bOrigin.offset(6, 0, 3));
        // Keep the WORLD controls outside A's assembled bounds, but clearly
        // within Simulated's native directional/range rules for both the
        // handheld position and the legal WORLD source. WORLD is a scope, not a
        // range bypass.
        var worldSource = link(origin.offset(4, 0, 3), false); worldSource.transmit(7);
        worldReceiverPos = origin.offset(5, 0, 3); worldSimPos = origin.offset(6, 0, 3);
        worldReceiver = link(worldReceiverPos, true); worldSim = simulated(worldSimPos);
        state().refresh().refresh(null);
    }
    private void fullFrequency(String label) {
        check.accept(label + " uses full same-frequency B Create path", count(bReceiverPos, false) > 0);
        check.accept(label + " uses full same-frequency B Simulated path", count(bSimPos, true) > 0);
        check.accept(label + " uses full same-frequency WORLD path", count(worldReceiverPos, false) > 0 && count(worldSimPos, true) > 0);
    }
    private void reentrantSelection(boolean broad) {
        var callback = new CallbackReceiver(stored.offset(0, 0, 1));
        com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, callback);
        boolean[] observed = {false};
        callback.action = value -> {
            if (value == 15 && !observed[0]) {
                observed[0] = true;
                var request = broad ? RefreshScope.frequency(level.dimension(), key)
                    : RefreshScope.privateReceivers(level.dimension(), key, IsolationRuntime.key(b));
                state().refresh().refresh(null, request);
            }
        };
        try {
            RefreshMetrics.measure(() -> state().refresh().refresh(null,
                RefreshScope.privateReceivers(level.dimension(), key, IsolationRuntime.key(a))));
            check.accept("receiver-limited callback request is exercised broad=" + broad, observed[0]);
            check.accept("reentrant same-key selection includes B on a later pass broad=" + broad,
                count(bReceiverPos, false) > 0 && count(bSimPos, true) > 0);
            check.accept("reentrant same-key ALL dominates, private union excludes WORLD broad=" + broad,
                broad ? count(worldReceiverPos, false) > 0 && count(worldSimPos, true) > 0
                    : count(worldReceiverPos, false) == 0 && count(worldSimPos, true) == 0);
            signal("reentrant receiver union preserves B9", other, 9);
            signal("reentrant receiver union preserves WORLD7", worldReceiver, 7);
        } finally {
            callback.action = ignored -> {};
            com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, callback);
        }
    }
    private int count(BlockPos position, boolean sim) { return RefreshMetrics.count(level.dimension(), key, sim, position); }
    private IRedstoneLinkable entry(ServerPlayer actor) { return (IRedstoneLinkable) LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID()).iterator().next(); }
    private void press(ServerPlayer actor) { new LinkedControllerInputPacket(List.of(0), true).handle(actor); }
    private void plot(ServerGamePacketListenerImpl listener, double x, double y, double z, boolean ground) {
        plot(listener, stored, x, y, z, ground);
    }
    private void plot(ServerGamePacketListenerImpl listener, BlockPos base, double x, double y, double z, boolean ground) {
        listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.getX() + x, base.getY() + y, base.getZ() + z, ground));
    }
    private void frequency(LinkBehaviour behaviour) {
        behaviour.setFrequency(true, new ItemStack(Items.AMETHYST_SHARD)); behaviour.setFrequency(false, new ItemStack(Items.LAPIS_LAZULI));
    }
    private RedstoneLinkBlockEntity link(BlockPos pos, boolean receiver) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos); entity.tick();
        if (receiver) { level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); entity.tick(); }
        frequency(entity.getBehaviour(LinkBehaviour.TYPE)); return entity;
    }
    private AbstractLinkedReceiverBlockEntity simulated(BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST), 3);
        var receiver = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(pos); receiver.tick(); frequency(receiver.getBehaviour(LinkBehaviour.TYPE)); receiver.updateSignal(); return receiver;
    }
    private void signal(String name, RedstoneLinkBlockEntity receiver, int expected) { check.accept(name + " expected=" + expected + " actual=" + receiver.getReceivedSignal(), receiver.getReceivedSignal() == expected); }
    private final class CallbackReceiver implements IRedstoneLinkable {
        final BlockPos position; IntConsumer action = ignored -> {};
        CallbackReceiver(BlockPos position) { this.position = position; }
        @Override public int getTransmittedStrength() { return 0; }
        @Override public boolean isListening() { return true; }
        @Override public void setReceivedStrength(int value) { action.accept(value); }
        @Override public boolean isAlive() { return true; }
        @Override public BlockPos getLocation() { return position; }
        @Override public Couple<Frequency> getNetworkKey() { return key; }
    }
}
