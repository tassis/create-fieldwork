package dev.tassis.createfieldwork.controller.session;

import dev.tassis.createfieldwork.platform.FieldworkServerState;
import net.minecraft.server.level.ServerPlayer;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned login identity only; player position and craft membership are not controller authority. */
public final class PlayerSessions {
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final WeakReference<ServerPlayer> actor;
        boolean suspended;
        Session(ServerPlayer actor) { this.actor = new WeakReference<>(actor); }
    }

    private final FieldworkServerState state;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public PlayerSessions(FieldworkServerState state) { this.state = state; }

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

    public ServerPlayer actor(UUID player, UUID token) {
        state.checkThread();
        var session = sessions.get(player);
        return session != null && !session.suspended && session.token.equals(token) ? session.actor.get() : null;
    }

    public boolean suspend(ServerPlayer actor) {
        state.checkThread();
        var session = sessions.get(actor.getUUID());
        if (session == null || session.actor.get() != actor || session.suspended) return false;
        session.suspended = true;
        state.invalidateVisibility();
        return true;
    }

    public void resume(ServerPlayer actor) {
        state.checkThread();
        var session = sessions.get(actor.getUUID());
        if (session == null || session.actor.get() != actor || !session.suspended) return;
        session.suspended = false;
        state.invalidateVisibility();
    }

    public boolean remove(ServerPlayer actor) {
        state.checkThread();
        var session = sessions.get(actor.getUUID());
        if (session == null || session.actor.get() != actor) return false;
        sessions.remove(actor.getUUID());
        state.invalidateVisibility();
        return true;
    }

    public void clear() { state.checkThread(); sessions.clear(); }
}
