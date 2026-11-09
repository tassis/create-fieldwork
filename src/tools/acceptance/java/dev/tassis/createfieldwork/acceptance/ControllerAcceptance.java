package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Real vanilla listener and real controller packet handler; outbound transport only is stubbed. */
final class ControllerAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    ControllerAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }

    void run() throws Exception {
        var state = FieldworkStateAccess.get(level.getServer());
        BlockPos origin = new BlockPos(96, 120, 0);
        var outside = link(origin.offset(0, 0, 8));
        link(origin.offset(3, 0, 0));
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            var pos = origin.offset(x, -1, 0);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3); blocks.add(pos);
        }
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        blocks.add(origin.offset(1, 0, 0)); blocks.add(origin.offset(3, 0, 0));
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        BlockPos stored = craft.getPlot().getCenterBlock();
        var inside = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0));
        inside.tick();
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MainAcceptance"), ClientInformation.createDefault());
        var transport = new Transport();
        var listener = new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        try {
            actor.setGameMode(GameType.SURVIVAL);
            var world = craft.logicalPose().transformPosition(new Vector3d(stored.getX() + .5, stored.getY() + .25, stored.getZ() + .5));
            actor.setPos(world.x, world.y, world.z);
            level.addNewPlayer(actor); listener.resetPosition();
            var item = AllItems.LINKED_CONTROLLER.asStack();
            item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
            actor.setItemInHand(InteractionHand.MAIN_HAND, item);
            actor.move(MoverType.SELF, new Vec3(0, -.5, 0));
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state.refresh().refresh(null);
            signal("ordinary handheld boarding cannot control private craft", inside, 0);
            signal("ordinary handheld boarding remains WORLD", outside, 15);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(90, 0, true));
            LinkedControllerServerHandler.tick(level);
            signal("rotation-only player packet cannot grant private handheld access", inside, 0);
            signal("rotation-only player packet preserves ordinary WORLD input", outside, 15);
            var entries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check.accept("actual controller uses original player WORLD distance position", ((com.simibubi.create.content.redstone.link.IRedstoneLinkable) entries.iterator().next()).getLocation().equals(actor.blockPosition()));

            // This frequency has only an ordinary WORLD controller and WORLD
            // receivers. Craft marker changes must not reclassify it private.
            var provenanceReceiver = link(origin.offset(0, 0, 12));
            provenanceReceiver.getBehaviour(LinkBehaviour.TYPE).setFrequency(true, new ItemStack(Items.PAPER));
            provenanceReceiver.getBehaviour(LinkBehaviour.TYPE).setFrequency(false, new ItemStack(Items.INK_SAC));
            new LinkedControllerInputPacket(List.of(0), false).handle(actor); LinkedControllerServerHandler.tick(level);
            var provenanceItem = AllItems.LINKED_CONTROLLER.asStack();
            provenanceItem.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.PAPER), new ItemStack(Items.INK_SAC))));
            actor.setItemInHand(InteractionHand.MAIN_HAND, provenanceItem);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            var currentActor = actor;
            RefreshMetrics.measure(() -> level.removeBlock(stored.offset(1, 0, 0), false));
            check.accept("marker removal leaves ordinary handheld frequency WORLD", provenanceReceiver.getReceivedSignal() == 15);
            unrelatedRefreshCounts("controller-only marker removal");
            RefreshMetrics.measure(() -> level.setBlock(stored.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3));
            check.accept("marker placement does not capture ordinary handheld frequency", provenanceReceiver.getReceivedSignal() == 15);
            unrelatedRefreshCounts("controller-only marker placement");
            new LinkedControllerInputPacket(List.of(0), false).handle(actor); LinkedControllerServerHandler.tick(level);
            actor.setItemInHand(InteractionHand.MAIN_HAND, item);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);

            var movementListener = listener;
            var acceptedMoveWork = ControllerWorkMetrics.measure(() -> movementListener.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(stored.getX() + 2.5, stored.getY() + 1.1, stored.getZ() + .5, false)));
            check.accept("position-only scoped refresh fixture accepts plot movement", awaitingPosition(listener) == null);
            check.accept("accepted movement performs zero targeted Fieldwork controller work",
                acceptedMoveWork.endpointReconciliations() == 0 && acceptedMoveWork.fullSweeps() == 0);
            RefreshMetrics.measure(() -> new LinkedControllerInputPacket(List.of(0), true).handle(currentActor));
            var controllerKey = Couple.create(Frequency.of(new ItemStack(Items.AMETHYST_SHARD)), Frequency.of(new ItemStack(Items.LAPIS_LAZULI)));
            check.accept("ordinary WORLD controller position update preserves upstream without Fieldwork refresh",
                RefreshMetrics.count(level.dimension(), controllerKey, false) == 0 && RefreshMetrics.total() == 0);
            var teleportStartWork = ControllerWorkMetrics.measure(() -> movementListener.teleport(world.x, world.y, world.z, 0, 0));
            check.accept("same-dimension teleport start performs zero targeted Fieldwork controller work",
                teleportStartWork.endpointReconciliations() == 0 && teleportStartWork.fullSweeps() == 0);
            var teleportAckWork = ControllerWorkMetrics.measure(() -> {
                try { acknowledge(movementListener); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
            check.accept("same-dimension teleport acknowledgement performs zero targeted Fieldwork controller work",
                teleportAckWork.endpointReconciliations() == 0 && teleportAckWork.fullSweeps() == 0);
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);

            var before = actor.position();
            var rejectedMoveWork = ControllerWorkMetrics.measure(() -> movementListener.handleMovePlayer(
                new ServerboundMovePlayerPacket.Pos(before.x + 5000, before.y, before.z, true)));
            check.accept("vanilla rejects excessive WORLD movement", awaitingPosition(listener) != null && actor.position().distanceToSqr(before) < .01);
            check.accept("rejected movement performs zero targeted Fieldwork controller work",
                rejectedMoveWork.endpointReconciliations() == 0 && rejectedMoveWork.fullSweeps() == 0);
            signal("rejected movement cannot grant private ordinary handheld access", inside, 0);
            signal("rejected movement leaves ordinary handheld WORLD", outside, 15);
            acknowledge(listener);
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state.refresh().refresh(null);
            signal("accepted boarding after rejection remains outside private craft", inside, 0);
            craft.logicalPose().orientation().rotateY(Math.PI / 2);
            craft.updateLastPose(); craft.forceUpdateGlobalBounds();
            SubLevelContainer.getContainer(level).physicsSystem().getPipeline().teleport(craft, craft.logicalPose().position(), craft.logicalPose().orientation());
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state.refresh().refresh(null);
            signal("rotated craft does not capture ordinary handheld", inside, 0);
            signal("rotated craft preserves ordinary WORLD controller path", outside, 15);
            var airborne = actor.position();
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(airborne.x, airborne.y + .3, airborne.z, false));
            check.accept("airborne WORLD-space move was actually accepted", awaitingPosition(listener) == null && Math.abs(actor.getY() - airborne.y - .3) < .01);
            LinkedControllerServerHandler.tick(level);
            signal("airborne tracking gap leaves ordinary handheld WORLD", outside, 15);
            signal("airborne tracking gap cannot authorize private input", inside, 0);
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            signal("accepted plot movement still cannot capture ordinary handheld", inside, 0);
            var nether = level.getServer().getLevel(Level.NETHER);
            var transferred = actor.changeDimension(new DimensionTransition(nether, new Vec3(.5, 200, .5), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
            check.accept("real player transfers to another dimension", transferred instanceof ServerPlayer && transferred.level() == nether);
            actor = (ServerPlayer) transferred; listener = actor.connection;
            signal("dimension transfer withdraws original craft input before transfer returns", inside, 0);
            signal("dimension transfer preserves upstream ordinary WORLD timeout", outside, 15);
            var back = craft.logicalPose().transformPosition(new Vector3d(stored.getX() + .5, stored.getY() + .1, stored.getZ() + .5));
            transferred = actor.changeDimension(new DimensionTransition(level, new Vec3(back.x, back.y, back.z), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
            check.accept("real player returns to original dimension", transferred instanceof ServerPlayer && transferred.level() == level);
            actor = (ServerPlayer) transferred; listener = actor.connection;
            if (awaitingPosition(listener) != null) acknowledge(listener);
            plot(listener, stored);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state.refresh().refresh(null);
            signal("accepted return restores ordinary WORLD control only", inside, 0);
            signal("accepted return restores ordinary WORLD contribution", outside, 15);
            transport.connected = false;
            LinkedControllerServerHandler.tick(level);
            signal("disconnect cannot grant private ordinary handheld input", inside, 0);
            signal("ordinary timeout compatibility retains WORLD input before removal", outside, 15);
            transport.connected = true;
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            BlockPos ground = origin.offset(0, -1, 4);
            level.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
            listener.teleport(ground.getX() + .5, ground.getY() + 1.1, ground.getZ() + .5, 0, 0);
            LinkedControllerServerHandler.tick(level);
            signal("teleport acknowledgement pending cannot grant private ordinary input", inside, 0);
            signal("teleport acknowledgement pending leaves ordinary handheld WORLD", outside, 15);
            acknowledge(listener);
            actor.move(MoverType.SELF, new Vec3(0, -.5, 0));
            listener.resetPosition();
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state.refresh().refresh(null);
            check.accept("confirmed departure uses actual server WORLD support", actor.mainSupportingBlockPos.isPresent() && Sable.HELPER.getTrackingSubLevel(actor) == null);
            signal("supported WORLD departure permits public controller", outside, 15);
            signal("supported WORLD departure no longer controls private craft", inside, 0);
            var nextWorld = actor.position().add(1, 0, 0);
            level.setBlock(BlockPos.containing(nextWorld).below(), Blocks.STONE.defaultBlockState(), 3);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(nextWorld.x, nextWorld.y, nextWorld.z, true));
            var worldActor = actor;
            RefreshMetrics.measure(() -> new LinkedControllerInputPacket(List.of(0), true).handle(worldActor));
            check.accept("pure WORLD position update preserves upstream without extra cold refresh", RefreshMetrics.total() == 0);
            var worldEntries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check.accept("pure WORLD controller still updates original position and timeout", ((com.simibubi.create.content.redstone.link.IRedstoneLinkable) worldEntries.iterator().next()).getLocation().equals(actor.blockPosition()));
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            signal("explicit handheld release removes power", outside, 0);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            listener.teleport(actor.getX(), actor.getY(), actor.getZ(), 0, 0);
            plot(listener, stored);
            check.accept("vanilla teleport acknowledgement is pending without becoming controller authority", awaitingPosition(listener) != null);
            LinkedControllerServerHandler.tick(level);
            signal("unaccepted private proposal cannot authorize craft", inside, 0);
            signal("unaccepted private proposal leaves ordinary handheld WORLD", outside, 15);
            acknowledge(listener);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            signal("accepted WORLD movement recovers rejected private proposal", outside, 15);
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);

            BlockPos lecternPos = stored.offset(2, 0, 0);
            level.setBlock(lecternPos, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
            var lectern = (LecternControllerBlockEntity) level.getBlockEntity(lecternPos);
            lectern.setController(item.copy());
            // Lectern reach is only 0.4 * BLOCK_INTERACTION_RANGE. Stand beside
            // its projected position, on an ordinary WORLD support block.
            var near = craft.logicalPose().transformPosition(new Vector3d(lecternPos.getX() + .5, lecternPos.getY(), lecternPos.getZ() + 1.5));
            var nearFloor = BlockPos.containing(near.x, near.y - 1, near.z);
            level.setBlock(nearFloor, Blocks.STONE.defaultBlockState(), 3);
            listener.teleport(near.x, nearFloor.getY() + 1.1, near.z, 0, 0);
            acknowledge(listener); actor.move(MoverType.SELF, new Vec3(0, -.5, 0)); listener.resetPosition();
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            check.accept("lectern operator is actually outside craft tracking", Sable.HELPER.getTrackingSubLevel(actor) == null);
            lectern.tryStartUsing(actor);
            check.accept("actual lectern validates WORLD-side active operator", lectern.isUsedBy(actor));
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            state.refresh().refresh(null);
            signal("private lectern follows source block rather than WORLD operator", inside, 15);
            signal("private lectern does not leak through WORLD operator", outside, 0);
            lectern.tryStopUsing(actor);
            signal("ordinary lectern stop withdraws private output before stopUsing returns", inside, 0);
            signal("stopped ordinary lectern cannot fall back WORLD", outside, 0);
            lectern.tick(); // clear Create's one-tick native deactivation latch
            lectern.tryStartUsing(actor);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            signal("ordinary lectern fresh press recovers private output", inside, 15);
            var lecternEntries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check.accept("lectern entry still uses player WORLD distance coordinate", ((com.simibubi.create.content.redstone.link.IRedstoneLinkable) lecternEntries.iterator().next()).getLocation().equals(actor.blockPosition()));
            level.removeBlock(stored.offset(1, 0, 0), false);
            signal("removing last marker releases lectern to WORLD", outside, 15);
            transport.connected = false;
            LinkedControllerServerHandler.tick(level);
            signal("released craft no longer applies private disconnect semantics", outside, 15);
            transport.connected = true;
            level.setBlock(stored.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
            signal("restoring marker withdraws public lectern contribution", outside, 0);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            LinkedControllerServerHandler.tick(level);
            signal("explicit lectern release clears same-craft power", inside, 0);
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            for (int i = 0; i < 31; i++) LinkedControllerServerHandler.tick(level);
            signal("upstream 30-tick lectern timeout still expires", inside, 0);
            lectern.tryStartUsing(actor);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            signal("ordinary lectern fresh press prepares removal seam", inside, 15);
            lectern.setRemoved();
            signal("lectern removal callback withdraws indexed output before returning", inside, 0);

            // Create's server tick calls stopUsing(null) when the recorded user
            // entity disappears. That native stop must synchronously withdraw an
            // ordinary WORLD lectern even though its Fieldwork actor is invalid.
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            BlockPos missingUserLecternPos = ground.offset(1, 1, 0);
            level.setBlock(missingUserLecternPos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(missingUserLecternPos, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
            var missingUserLectern = (LecternControllerBlockEntity) level.getBlockEntity(missingUserLecternPos);
            missingUserLectern.setController(item.copy());
            listener.teleport(ground.getX() + .5, ground.getY() + 1.1, ground.getZ() + .5, 0, 0);
            acknowledge(listener);
            actor.getPersistentData().remove("IsUsingLecternController");
            missingUserLectern.tryStartUsing(actor);
            check.accept("WORLD lectern accepts user before missing-player stop", missingUserLectern.isUsedBy(actor));
            new LinkedControllerInputPacket(List.of(0), true, missingUserLecternPos).handle(actor);
            signal("WORLD lectern fixture publishes before missing-player stop", outside, 15);
            UUID sameId = actor.getUUID();
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            signal("missing actor retains ordinary WORLD lectern until native stop", outside, 15);
            missingUserLectern.tick();
            check.accept("native missing-player tick clears WORLD lectern user", !missingUserLectern.hasUser());
            signal("native missing-player stop withdraws WORLD lectern before tick returns", outside, 0);

            // A new real player/listener object with the same UUID must receive
            // a new server session, never the old source authorization.
            transport.embedded.finishAndReleaseAll();
            actor = new ServerPlayer(level.getServer(), level, new GameProfile(sameId, "MainAcceptance"), ClientInformation.createDefault());
            transport = new Transport();
            listener = new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            actor.setPos(ground.getX() + .5, ground.getY() + 1, ground.getZ() + .5);
            actor.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
            level.addNewPlayer(actor); listener.resetPosition();
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            LinkedControllerServerHandler.tick(level);
            signal("same UUID new session cannot inherit private old input", inside, 0);
            signal("same UUID new session cannot inherit stopped WORLD lectern", outside, 0);
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            signal("fresh input from new WORLD session can control WORLD", outside, 15);
            signal("fresh WORLD session still cannot control old private craft", inside, 0);
        } finally {
            transport.connected = true;
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            transport.embedded.finishAndReleaseAll();
        }
    }

    private RedstoneLinkBlockEntity link(BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos); entity.tick();
        level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); entity.tick();
        var behaviour = entity.getBehaviour(LinkBehaviour.TYPE);
        behaviour.setFrequency(true, new ItemStack(Items.AMETHYST_SHARD));
        behaviour.setFrequency(false, new ItemStack(Items.LAPIS_LAZULI));
        return entity;
    }
    private void plot(ServerGamePacketListenerImpl listener, BlockPos stored) {
        listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(stored.getX() + .5, stored.getY() + .1, stored.getZ() + .5, true));
    }
    static void acknowledge(ServerGamePacketListenerImpl listener) throws Exception {
        var field = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
        field.setAccessible(true);
        listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(field.getInt(listener)));
    }
    static Vec3 awaitingPosition(ServerGamePacketListenerImpl listener) throws Exception {
        var field = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingPositionFromClient");
        field.setAccessible(true);
        return (Vec3) field.get(listener);
    }
    private void signal(String name, RedstoneLinkBlockEntity receiver, int expected) {
        check.accept(name + " expected=" + expected + " actual=" + receiver.getReceivedSignal(), receiver.getReceivedSignal() == expected);
    }
    private void unrelatedRefreshCounts(String name) {
        var unrelated = Couple.create(Frequency.of(new ItemStack(Items.DIAMOND)), Frequency.of(new ItemStack(Items.EMERALD)));
        check.accept(name + " does not cold-refresh unrelated Create frequency", RefreshMetrics.count(level.dimension(), unrelated, false) == 0);
        check.accept(name + " does not cold-refresh unrelated Simulated frequency", RefreshMetrics.count(level.dimension(), unrelated, true) == 0);
    }
    static final class Transport extends Connection {
        boolean connected = true;
        final EmbeddedChannel embedded;
        Transport() { super(PacketFlow.SERVERBOUND); embedded = new EmbeddedChannel(this); }
        @Override public boolean isConnected() { return connected; }
        @Override public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 0); }
        @Override public String getLoggableAddress(boolean logIps) { return "main-acceptance"; }
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, PacketSendListener listener) { if (listener != null) listener.onSuccess(); }
        @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { if (listener != null) listener.onSuccess(); }
        @Override public void disconnect(Component message) { connected = false; }
        @Override public void disconnect(DisconnectionDetails details) { connected = false; }
    }
}
