package dev.tassis.createfieldwork.test;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Full vanilla listener (NOT FakePlayerNetHandler), only outbound transport stubbed. */
final class ControllerMotionProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private ServerSubLevel craft;
    private RedstoneLinkBlockEntity inside, outside;
    private ServerPlayer actor;
    private ServerGamePacketListenerImpl listener;
    private BlockPos stored;
    private MotionConnection connection;

    ControllerMotionProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    boolean tick(int tick) {
        if (tick == 20) {
            BlockPos origin = new BlockPos(0, 120, 0);
            checks.link(origin.offset(3, 0, 0), true);
            outside = checks.link(origin.offset(0, 0, 8), true);
            var blocks = new ArrayList<BlockPos>();
            for (int x = 0; x <= 3; x++) {
                level.setBlock(origin.offset(x, -1, 0), Blocks.STONE.defaultBlockState(), 3);
                blocks.add(origin.offset(x, -1, 0));
            }
            level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
            blocks.add(origin.offset(1, 0, 0)); blocks.add(origin.offset(3, 0, 0));
            craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
            stored = craft.getPlot().getCenterBlock();
            inside = checks.initialized(stored.offset(3, 0, 0));
        }
        if (tick < 40) return false;
        try {
            actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MotionResearch"), ClientInformation.createDefault());
            connection = new MotionConnection();
            listener = new ServerGamePacketListenerImpl(level.getServer(), connection, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            actor.setGameMode(GameType.SURVIVAL);
            var p = craft.logicalPose().transformPosition(new Vector3d(stored.getX() + 0.5, stored.getY() + 0.25, stored.getZ() + 0.5));
            actor.setPos(p.x, p.y, p.z);
            level.addNewPlayer(actor);
            listener.resetPosition();
            actor.setItemInHand(InteractionHand.MAIN_HAND, ControllerProbe.controller());
            ProbeController.enabled = true;
            ProbeController.refresh = checks::refresh;
            ProbeMovement.enabled = true;
            actor.move(MoverType.SELF, new Vec3(0, -0.5, 0));
            ProbeController.naturalTrackingObserved = Sable.HELPER.getTrackingSubLevel(actor) == craft;

            plotPacket();
            checks.check("real listener accepts plot-space boarding packet", ProbeMovement.proof(actor) != null && craft.getUniqueId().equals(ProbeMovement.proof(actor).plot()));
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            checks.refresh();
            checks.signal("full listener boarding controls internal", inside, 15);
            checks.signal("full listener boarding isolates world", outside, 0);

            var proof = ProbeMovement.proof(actor);
            Vec3 before = actor.position();
            worldPacket(before.x + 5000, before.y, before.z, true);
            checks.check("vanilla rejected excessive WORLD move", ((MovementAccess) listener).probe$awaitingPosition() != null && actor.position().distanceToSqr(before) < 0.01);
            checks.check("Sable tracking was speculatively cleared by rejected packet", Sable.HELPER.getTrackingSubLevel(actor) == null);
            checks.check("rejected packet cannot create accepted movement proof", ProbeMovement.proof(actor) == proof);
            LinkedControllerServerHandler.tick(level);
            checks.signal("negative control rejected move currently leaks WORLD", outside, 15);
            acknowledge();
            plotPacket();
            LinkedControllerServerHandler.tick(level);
            checks.signal("boarding packet restores private source", inside, 15);

            ProbeMovement.guardControllerTransitions = true;
            before = actor.position();
            worldPacket(before.x + 5000, before.y, before.z, true);
            LinkedControllerServerHandler.tick(level);
            checks.signal("PENDING blocks WORLD after rejected departure", outside, 0);
            checks.check("PENDING did not become a successful departure", ProbeMovement.proof(actor).plot() != null);
            acknowledge();
            plotPacket();
            checks.signal("accepted plot packet resolves PENDING internally", inside, 15);
            checks.signal("accepted plot packet keeps world isolated", outside, 0);

            before = actor.position();
            worldPacket(before.x, before.y + 0.3, before.z, false);
            checks.check("airborne WORLD packet was accepted", ((MovementAccess) listener).probe$awaitingPosition() == null && ProbeMovement.proof(actor).plot() == null);
            LinkedControllerServerHandler.tick(level);
            checks.signal("airborne tracking loss does not join WORLD", outside, 0);
            plotPacket();
            checks.signal("plot membership resumes after airborne gap", inside, 15);

            int groundY = (int) Math.floor(actor.getY()) - 1;
            level.setBlock(new BlockPos(8, groundY, 0), Blocks.STONE.defaultBlockState(), 3);
            worldPacket(8.5, groundY + 1, 0.5, true);
            if (((MovementAccess) listener).probe$awaitingPosition() != null) throw new AssertionError("world landing rejected");
            worldPacket(actor.getX(), actor.getY(), actor.getZ(), true);
            checks.check("accepted grounded WORLD movement confirms departure", ProbeMovement.proof(actor).supportedWorld() && Sable.HELPER.getTrackingSubLevel(actor) == null);
            LinkedControllerServerHandler.tick(level);
            checks.signal("confirmed WORLD departure clears craft", inside, 0);
            checks.signal("confirmed WORLD departure enables upstream world", outside, 15);

            proof = ProbeMovement.proof(actor);
            listener.teleport(actor.getX(), actor.getY(), actor.getZ(), actor.getYRot(), actor.getXRot());
            plotPacket();
            checks.check("vanilla ignores plot proposal while teleport ack is pending", ((MovementAccess) listener).probe$awaitingPosition() != null);
            checks.check("rejected private proposal leaves accepted proof unchanged", ProbeMovement.proof(actor) == proof);
            LinkedControllerServerHandler.tick(level);
            checks.signal("rejected private proposal cannot authorize private craft", inside, 0);
            acknowledge();
            worldPacket(actor.getX(), actor.getY(), actor.getZ(), true);
            LinkedControllerServerHandler.tick(level);
            checks.signal("accepted WORLD packet recovers rejected private proposal", outside, 15);

            plotPacket();
            ((EntityMovementExtension) actor).sable$setTrackingSubLevel(null);
            LinkedControllerServerHandler.tick(level);
            checks.signal("unconfirmed getter loss alone remains closed", outside, 0);
            marker(false);
            checks.signal("READY empty releases PENDING to upstream WORLD", outside, 15);
            marker(true);
            LinkedControllerServerHandler.tick(level);
            checks.signal("first marker restores PENDING without guessing WORLD", outside, 0);
            plotPacket();
            checks.signal("accepted same-instance membership restores controls", inside, 15);

            craft.logicalPose().orientation().rotateY(Math.PI / 2);
            craft.updateLastPose(); craft.forceUpdateGlobalBounds();
            SubLevelContainer.getContainer(level).physicsSystem().getPipeline().teleport(craft, craft.logicalPose().position(), craft.logicalPose().orientation());
            plotPacket();
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            checks.refresh();
            checks.signal("rotated craft retains same-instance control", inside, 15);
            checks.signal("rotated craft remains isolated", outside, 0);

            var nether = level.getServer().getLevel(Level.NETHER);
            var transferred = actor.changeDimension(new DimensionTransition(nether, new Vec3(0.5, 200, 0.5), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
            checks.check("real dimension transfer completes without client", transferred instanceof ServerPlayer && transferred.level() == nether);
            actor = (ServerPlayer) transferred;
            listener = actor.connection;
            LinkedControllerServerHandler.tick(level);
            checks.signal("dimension change withdraws old private scope", inside, 0);
            checks.signal("dimension change cannot leak old entry to WORLD", outside, 0);
            var back = craft.logicalPose().transformPosition(new Vector3d(stored.getX() + 0.5, stored.getY() + 0.1, stored.getZ() + 0.5));
            transferred = actor.changeDimension(new DimensionTransition(level, new Vec3(back.x, back.y, back.z), Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING));
            checks.check("return dimension transfer completes", transferred instanceof ServerPlayer && transferred.level() == level);
            actor = (ServerPlayer) transferred;
            listener = actor.connection;
            if (((MovementAccess) listener).probe$awaitingPosition() != null) acknowledge();
            plotPacket();
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            checks.refresh();
            checks.signal("confirmed instance after dimension return resumes control", inside, 15);
            checks.check("transport did not disconnect the research player", connection.connected);
            connection.connected = false;
            checks.check("disconnect fixture precedes player entity removal", !actor.isRemoved());
            LinkedControllerServerHandler.tick(level);
            checks.signal("closed transport withdraws private power before entity removal", inside, 0);
            checks.signal("closed private transport never falls back to WORLD", outside, 0);
            return true;
        } finally {
            if (actor != null) {
                new LinkedControllerInputPacket(List.of(0), false).handle(actor);
                LinkedControllerServerHandler.tick(level);
                level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            }
            ProbeMovement.enabled = false;
            ProbeMovement.guardControllerTransitions = false;
            ProbeController.enabled = false;
            ProbeController.refresh = null;
            SubLevelContainer.getContainer(level).removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
            if (connection != null) connection.closeEmbedded();
        }
    }

    private void plotPacket() {
        listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(stored.getX() + 0.5, stored.getY() + 0.1, stored.getZ() + 0.5, true));
    }
    private void worldPacket(double x, double y, double z, boolean ground) {
        listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(x, y, z, ground));
    }
    private void acknowledge() {
        listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(((MovementAccess) listener).probe$teleportId()));
    }
    private void marker(boolean present) {
        BlockPos pos = stored.offset(1, 0, 0);
        ProbeLifecycle.begin("motion-marker", level, List.of(pos));
        level.setBlock(pos, present ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        ProbeLifecycle.end("motion-marker", level, true);
    }

    private static final class MotionConnection extends Connection {
        boolean connected = true;
        private final EmbeddedChannel embedded;
        MotionConnection() { super(PacketFlow.SERVERBOUND); embedded = new EmbeddedChannel(this); }
        void closeEmbedded() { embedded.finishAndReleaseAll(); }
        @Override public boolean isConnected() { return connected; }
        @Override public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 0); }
        @Override public String getLoggableAddress(boolean logIps) { return "headless-motion"; }
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, PacketSendListener listener) { if (listener != null) listener.onSuccess(); }
        @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { if (listener != null) listener.onSuccess(); }
        @Override public void disconnect(Component message) { connected = false; }
        @Override public void disconnect(DisconnectionDetails details) { connected = false; }
    }
}
