package dev.tassis.createfieldwork.controller;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.Scope;
import dev.tassis.createfieldwork.wireless.ScopeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

/** Accepted server movement evidence, scoped to a server-generated player-object session. */
public final class PlayerMemberships {
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final WeakReference<ServerPlayer> actor;
        CraftKey confirmed;
        CraftKey proposal;
        boolean pending;
        boolean unknownProposal;
        boolean acceptedWorld = true;
        Session(ServerPlayer actor) {
            this.actor = new WeakReference<>(actor);
            var tracking = Sable.HELPER.getTrackingSubLevel(actor);
            if (tracking != null) { confirmed = IsolationRuntime.key(tracking); pending = true; acceptedWorld = false; }
        }
    }
    public static final class Frame {
        final Session session;
        final UUID actor;
        final CraftKey proposal;
        final boolean hadPosition;
        final boolean valid;
        final boolean transition;
        final Frame previous;
        boolean accepted;
        Frame(Session session, ServerPlayer actor, ServerboundMovePlayerPacket packet, Frame previous) {
            this.session = session; this.actor = actor.getUUID(); this.previous = previous;
            hadPosition = packet.hasPosition();
            var owner = hadPosition ? Sable.HELPER.getContaining(actor.level(), packet.getX(0), packet.getZ(0)) : null;
            proposal = owner == null ? null : IsolationRuntime.key(owner);
            valid = hadPosition && (owner != null || !SubLevelContainer.getContainer(actor.level())
                .inBounds(BlockPos.containing(packet.getX(0), packet.getY(0), packet.getZ(0))));
            transition = hadPosition && (session.pending || !Objects.equals(proposal, session.confirmed) || !valid);
        }
    }

    private final FieldworkServerState state;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private Frame current;

    public PlayerMemberships(FieldworkServerState state) { this.state = state; }

    public UUID bind(ServerPlayer actor) {
        state.checkThread();
        var session = sessions.get(actor.getUUID());
        if (session == null || session.actor.get() != actor) {
            session = new Session(actor);
            sessions.put(actor.getUUID(), session);
            state.invalidateVisibility();
        }
        return session.token;
    }

    public Frame begin(ServerPlayer actor, ServerboundMovePlayerPacket packet) {
        bind(actor);
        var frame = new Frame(sessions.get(actor.getUUID()), actor, packet, current);
        current = frame;
        if (frame.transition) {
            frame.session.proposal = frame.proposal;
            frame.session.unknownProposal = !frame.valid;
            frame.session.pending = true;
            state.controllers().reconcilePlayer(actor.getUUID()); // withdraw before Sable speculatively mutates tracking
        }
        return frame;
    }

    public void accepted(ServerPlayer actor) {
        state.checkThread();
        if (current == null || !current.actor.equals(actor.getUUID()) || !current.valid) return;
        var session = current.session;
        current.accepted = true;
        session.unknownProposal = false;
        if (current.proposal != null) {
            session.confirmed = current.proposal;
            session.pending = false;
            session.proposal = null;
            session.acceptedWorld = false;
        } else if (session.confirmed == null || supportedWorld(actor) || publicCraft(session.confirmed)) {
            session.confirmed = null;
            session.pending = false;
            session.proposal = null;
            session.acceptedWorld = true;
        } else { session.pending = true; session.acceptedWorld = false; }
    }

    public void end(Frame frame) {
        state.checkThread();
        if (current != frame) throw new IllegalStateException("Unbalanced player movement evidence");
        if (frame.hadPosition && frame.transition && !frame.accepted) frame.session.pending = true;
        current = frame.previous;
        if (frame.transition) state.controllers().reconcilePlayer(frame.actor);
    }

    public ServerPlayer actor(UUID player, UUID token) {
        state.checkThread();
        var session = sessions.get(player);
        return session != null && session.token.equals(token) ? session.actor.get() : null;
    }

    public boolean restricted(UUID player, UUID token) {
        return restricted(player, token, null);
    }

    public boolean restricted(UUID player, UUID token, TransitionCoordinator.View view) {
        state.checkThread();
        var session = sessions.get(player);
        if (session == null || !session.token.equals(token)) return false;
        var actor = session.actor.get();
        var tracking = actor == null ? null : Sable.HELPER.getTrackingSubLevel(actor);
        return session.unknownProposal || (session.confirmed != null && !publicCraft(session.confirmed, view))
            || (session.proposal != null && !publicCraft(session.proposal, view))
            || (tracking != null && !publicCraft(IsolationRuntime.key(tracking), view));
    }

    public boolean associated(UUID player, UUID token, Set<CraftKey> crafts) {
        state.checkThread();
        var session = sessions.get(player);
        if (session == null || !session.token.equals(token)) return false;
        if (session.confirmed != null && crafts.contains(session.confirmed)) return true;
        if (session.proposal != null && crafts.contains(session.proposal)) return true;
        var actor = session.actor.get();
        var tracked = actor == null ? null : Sable.HELPER.getTrackingSubLevel(actor);
        return tracked != null && crafts.contains(IsolationRuntime.key(tracked));
    }

    public Scope scope(UUID player, UUID token, ServerLevel level, TransitionCoordinator.View view) {
        state.checkThread();
        var session = sessions.get(player);
        if (session == null || !session.token.equals(token)) return Scope.PENDING;
        var actor = session.actor.get();
        if (actor == null || actor.isRemoved() || actor.isDeadOrDying() || actor.level() != level
            || actor.connection == null || !actor.connection.isAcceptingMessages()) return Scope.PENDING;
        boolean restricted = restricted(player, token, view);
        if (restricted && ((ConnectionStateAccess) actor.connection).fieldwork$awaitingPosition() != null) return Scope.PENDING;
        if (session.pending && restricted) return Scope.PENDING;
        var tracking = Sable.HELPER.getTrackingSubLevel(actor);
        var tracked = tracking == null ? null : IsolationRuntime.key(tracking);
        if (restricted && !Objects.equals(tracked, session.confirmed)) return Scope.PENDING;
        if (session.confirmed != null) return ScopeResolver.craft(level, session.confirmed, view);
        if (tracked != null && !publicCraft(tracked, view)) return Scope.PENDING;
        return Scope.WORLD;
    }

    private boolean publicCraft(CraftKey key) { return state.registry().scope(key) == Scope.WORLD; }
    boolean publicCraft(CraftKey key, TransitionCoordinator.View view) {
        if (view == null) return publicCraft(key);
        var level = state.server().getLevel(key.dimension());
        return level != null && ScopeResolver.craft(level, key, view) == Scope.WORLD;
    }

    private boolean supportedWorld(ServerPlayer actor) {
        if (!actor.onGround() || actor.noPhysics || actor.mainSupportingBlockPos.isEmpty()) return false;
        var support = actor.mainSupportingBlockPos.get();
        if (Sable.HELPER.getContaining(actor.level(), support) != null) return false;
        var shape = actor.level().getBlockState(support).getCollisionShape(actor.level(), support);
        return !shape.isEmpty() && Math.abs(actor.getY() - support.getY() - shape.max(Direction.Axis.Y)) < 0.1;
    }

    public void clear() { state.checkThread(); sessions.clear(); current = null; }
}
