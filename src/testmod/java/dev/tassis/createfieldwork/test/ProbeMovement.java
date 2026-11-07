package dev.tassis.createfieldwork.test;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Research-only acceptance evidence, not a new client membership protocol. */
public final class ProbeMovement {
    public static boolean enabled;
    public static boolean guardControllerTransitions;
    public static Frame current;
    public static final List<Map<String, Object>> events = new ArrayList<>();
    private static final Map<ServerPlayer, Proof> proofs = new WeakHashMap<>();
    private static final Map<ServerPlayer, UUID> lastPrivate = new WeakHashMap<>();
    private static final Map<ServerPlayer, UUID> rejectedProposals = new WeakHashMap<>();
    public record Proof(Object dimension, UUID plot, Vec3 position, boolean supportedWorld) {}
    public static final class Frame {
        final ServerPlayer actor;
        final UUID rawPlot;
        final boolean validPosition;
        final boolean hadPosition;
        boolean accepted;
        Frame(ServerPlayer actor, ServerboundMovePlayerPacket packet) {
            this.actor = actor;
            hadPosition = packet.hasPosition();
            var owner = packet.hasPosition() ? Sable.HELPER.getContaining(actor.level(), packet.getX(0), packet.getZ(0)) : null;
            rawPlot = owner == null ? null : owner.getUniqueId();
            var container = SubLevelContainer.getContainer(actor.level());
            boolean unownedPlot = packet.hasPosition() && owner == null && container != null
                && container.inBounds(BlockPos.containing(packet.getX(0), packet.getY(0), packet.getZ(0)));
            validPosition = packet.hasPosition() && !unownedPlot;
        }
    }
    public static Frame begin(ServerPlayer actor, ServerboundMovePlayerPacket packet) {
        Frame old = current;
        current = new Frame(actor, packet);
        return old;
    }
    public static void accepted(ServerPlayer actor) {
        if (!enabled || current == null || current.actor != actor || !current.validPosition) return;
        current.accepted = true;
        rejectedProposals.remove(actor);
        boolean world = current.rawPlot == null && supportedWorld(actor);
        proofs.put(actor, new Proof(actor.level().dimension(), current.rawPlot, actor.position(), world));
        if (world) lastPrivate.remove(actor);
        if (current.rawPlot != null) {
            var owner = SubLevelContainer.getContainer(actor.level()).getSubLevel(current.rawPlot);
            if (owner != null && ProbeScope.isPrivate(owner)) lastPrivate.put(actor, current.rawPlot);
        }
    }
    public static void end(Frame previous) {
        Frame done = current;
        if (done.hadPosition && !done.accepted) rejectedProposals.put(done.actor, done.rawPlot);
        events.add(Map.of("accepted", done.accepted, "rawPlot", String.valueOf(done.rawPlot),
            "trackingAfter", String.valueOf(Sable.HELPER.getTrackingSubLevel(done.actor)),
            "position", done.actor.position().toString(), "worldSupported", supportedWorld(done.actor)));
        current = previous;
        if (guardControllerTransitions) {
            ProbeController.reconcile(done.actor.level());
            if (ProbeController.refresh != null) ProbeController.refresh.run();
        }
    }
    public static void remember(ServerPlayer actor, SubLevel owner) {
        if (guardControllerTransitions && owner != null && ProbeScope.isPrivate(owner)
            && (current == null || current.actor != actor || current.accepted)) lastPrivate.put(actor, owner.getUniqueId());
    }
    public static boolean pending(ProbeController.Source source, Level level) {
        if (!guardControllerTransitions || source.lectern() != null) return false;
        var actor = source.player();
        if (current != null && current.actor == actor && !current.accepted) return true;
        if (rejectedProposals.containsKey(actor)) return true;
        var owner = Sable.HELPER.getTrackingSubLevel(actor);
        if (owner != null) return false;
        UUID old = lastPrivate.get(actor);
        if (old != null) {
            var craft = SubLevelContainer.getContainer(level).getSubLevel(old);
            if (craft != null && ProbeScope.isReady(craft) && !ProbeScope.isPrivate(craft)) return false;
        }
        Proof proof = proofs.get(actor);
        return proof == null || !proof.dimension.equals(level.dimension()) || proof.plot != null
            || !proof.supportedWorld || actor.position().distanceToSqr(proof.position) > 0.01;
    }
    public static boolean lastPrivateIsRestricted(ProbeController.Source source, Level level) {
        if (!guardControllerTransitions || source.lectern() != null) return false;
        UUID id = lastPrivate.get(source.player());
        if (id == null) return false;
        var owner = SubLevelContainer.getContainer(level).getSubLevel(id);
        return owner != null && ProbeScope.isPrivate(owner);
    }
    public static boolean proposedPrivateIsUnconfirmed(ProbeController.Source source, Level level) {
        if (!guardControllerTransitions || source.lectern() != null) return false;
        UUID proposal = current != null && current.actor == source.player() && !current.accepted
            ? current.rawPlot : rejectedProposals.get(source.player());
        if (proposal == null) return false;
        var owner = SubLevelContainer.getContainer(level).getSubLevel(proposal);
        return owner != null && ProbeScope.isPrivate(owner);
    }
    public static Proof proof(ServerPlayer actor) { return proofs.get(actor); }
    private static boolean supportedWorld(ServerPlayer actor) {
        if (!actor.onGround() || actor.noPhysics || actor.mainSupportingBlockPos.isEmpty()) return false;
        // Use vanilla's geometry-computed support, not getOnPos's fallback and
        // not the packet's onGround claim alone.
        BlockPos support = actor.mainSupportingBlockPos.get();
        if (Sable.HELPER.getContaining(actor.level(), support) != null) return false;
        var shape = actor.level().getBlockState(support).getCollisionShape(actor.level(), support);
        return !shape.isEmpty() && Math.abs(actor.getY() - support.getY() - shape.max(Direction.Axis.Y)) < 0.1;
    }
    private ProbeMovement() {}
}
