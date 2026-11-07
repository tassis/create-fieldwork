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
import dev.tassis.createfieldwork.controller.ConnectionStateAccess;
import dev.tassis.createfieldwork.controller.ControllerEndpoint;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.wireless.RefreshScope;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
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
    private BlockPos stored, bReceiverPos, bSimPos, worldReceiverPos, worldSimPos;
    private RedstoneLinkBlockEntity own, other, worldReceiver;
    private AbstractLinkedReceiverBlockEntity ownSim, otherSim, worldSim;

    ControllerMovementAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    private dev.tassis.createfieldwork.server.FieldworkServerState state() { return FieldworkStateAccess.get(level.getServer()); }

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
            signal("movement fixture A starts at private15", own, 15);
            signal("same-frequency B retains independent9", other, 9);
            signal("same-frequency WORLD retains independent7", worldReceiver, 7);
            check.accept("same-frequency B Simulated starts with legal9", otherSim.receivedSignal == 9);
            check.accept("same-frequency WORLD Simulated starts with legal7", worldSim.receivedSignal == 7);
            RefreshMetrics.measure(() -> state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), key)));
            fullFrequency("full-frequency counter negative control");
            int fullFrequencyCalls = RefreshMetrics.total();
            var unchanged = actor;
            RefreshMetrics.measure(() -> press(unchanged));
            check.accept("unchanged private input does not refresh receivers", RefreshMetrics.total() == 0);

            var range = com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange;
            int previousRange = range.get();
            try {
                range.set(2); state().refresh().refresh(null);
                signal("same-private far controller obeys original range; actor=" + actor.position() + " entry=" + entry(actor).getLocation(), own, 0);
                plot(listener, 2.5, 1.1, .5, false);
                check.accept("same-private near movement is accepted by vanilla", ((ConnectionStateAccess) listener).fieldwork$awaitingPosition() == null);
                var movingActor = actor;
                RefreshMetrics.measure(() -> press(movingActor));
                check.accept("position update targets own same-frequency Create receiver", count(stored.offset(3, 0, 0), false) > 0);
                check.accept("position update targets own same-frequency Simulated receiver", count(stored.offset(2, 0, 0), true) > 0);
                check.accept("private position updater calls are below full-frequency baseline; private=" + RefreshMetrics.total()
                    + " full=" + fullFrequencyCalls, RefreshMetrics.total() < fullFrequencyCalls);
                outsiders("stable private movement");
                signal("private position refresh immediately crosses range boundary", own, 15);
                signal("private movement preserves B's legal9", other, 9);
                signal("private movement preserves WORLD's legal7", worldReceiver, 7);
                check.accept("private movement preserves unrelated Simulated9/7", otherSim.receivedSignal == 9 && worldSim.receivedSignal == 7);
                int signal = ownSim.receivedSignal; double raw = ownSim.rawSignalValue;
                ownSim.updateSignal();
                check.accept("private movement preserves Simulated native direction/raw result", signal == ownSim.receivedSignal && Double.compare(raw, ownSim.rawSignalValue) == 0);
                plot(listener, .5, 1.1, 1.5, false);
                check.accept("same-private out-of-range movement is accepted", ((ConnectionStateAccess) listener).fieldwork$awaitingPosition() == null);
                RefreshMetrics.measure(() -> press(movingActor));
                signal("private movement immediately withdraws stale15 after leaving range", own, 0);
                outsiders("stable private out-of-range movement");
            } finally { range.set(previousRange); state().refresh().refresh(null); }
            reentrantSelection(false);
            reentrantSelection(true);

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
        link(origin.offset(3, 0, 0), true); simulated(origin.offset(2, 0, 0));
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) for (int z = 0; z <= 1; z++) {
            var floor = origin.offset(x, -1, z); level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3); positions.add(floor);
        }
        positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(2, 0, 0)); positions.add(origin.offset(3, 0, 0));
        a = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        stored = a.getPlot().getCenterBlock(); own = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0)); own.tick();
        ownSim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.offset(2, 0, 0)); ownSim.tick();

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
        var bStored = b.getPlot().getCenterBlock();
        var source = (RedstoneLinkBlockEntity) level.getBlockEntity(bStored.offset(3, 0, 0)); source.tick(); source.transmit(9);
        bReceiverPos = bStored.offset(2, 0, 0); bSimPos = bStored.offset(1, 0, 0);
        other = (RedstoneLinkBlockEntity) level.getBlockEntity(bReceiverPos); other.tick();
        otherSim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(bSimPos); otherSim.tick();
        var worldSource = link(origin.offset(8, 0, 8), false); worldSource.transmit(7);
        worldReceiverPos = origin.offset(9, 0, 8); worldSimPos = origin.offset(10, 0, 8);
        worldReceiver = link(worldReceiverPos, true); worldSim = simulated(worldSimPos);
        state().refresh().refresh(null);
    }
    private void outsiders(String label) {
        check.accept(label + " skips same-frequency B Create receiver", count(bReceiverPos, false) == 0);
        check.accept(label + " skips same-frequency B Simulated receiver", count(bSimPos, true) == 0 && count(bSimPos, false) == 0);
        check.accept(label + " skips same-frequency WORLD Create receiver", count(worldReceiverPos, false) == 0);
        check.accept(label + " skips same-frequency WORLD Simulated receiver", count(worldSimPos, true) == 0 && count(worldSimPos, false) == 0);
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
        listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(stored.getX() + x, stored.getY() + y, stored.getZ() + z, ground));
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
