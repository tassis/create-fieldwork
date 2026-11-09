package dev.tassis.createfieldwork.controller.routing;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Packet provenance, release barriers and player-session lifecycle. */
final class ControllerInputSessions {
    private final FieldworkServerState state;
    private final ControllerSources sources;
    private final Map<UUID, UUID> releaseRequired = new HashMap<>();
    private ControllerSources.Source current;

    ControllerInputSessions(FieldworkServerState state, ControllerSources sources) {
        this.state = state;
        this.sources = sources;
    }

    ControllerSources.Source begin(ServerPlayer actor, BlockPos lectern, ItemStack controller) {
        var previous = current;
        var level = actor.serverLevel();
        var device = ControllerDevice.read(controller);
        var program = device == null
            ? ItemContainerContents.EMPTY
            : controller.getOrDefault(
                AllDataComponents.LINKED_CONTROLLER_ITEMS,
                ItemContainerContents.EMPTY
            );
        InteractionHand hand = null;
        if (lectern == null) {
            if (controller == actor.getMainHandItem()) {
                hand = InteractionHand.MAIN_HAND;
            } else if (controller == actor.getOffhandItem()) {
                hand = InteractionHand.OFF_HAND;
            }
        }
        current = new ControllerSources.Source(
            actor.getUUID(),
            state.playerSessions().bind(actor),
            level.dimension(),
            lectern == null ? null : lectern.immutable(),
            new WeakReference<>(lectern == null ? null : level.getBlockEntity(lectern)),
            device,
            device == null ? 0 : state.bindings().revision(device),
            program,
            hand
        );
        return previous;
    }

    void end(ControllerSources.Source previous) {
        current = previous;
    }

    ControllerSources.Source current() {
        return current;
    }

    ControllerSources.PlayerTransition beginPlayerTransition(ServerPlayer actor) {
        boolean suspended = state.playerSessions().suspend(actor);
        if (!suspended) {
            return new ControllerSources.PlayerTransition(actor, false);
        }
        try {
            sources.reconcilePlayer(actor.getUUID());
        } catch (RuntimeException | Error error) {
            state.playerSessions().resume(actor);
            try {
                sources.reconcilePlayer(actor.getUUID());
            } catch (RuntimeException | Error restore) {
                error.addSuppressed(restore);
            }
            throw error;
        }
        return new ControllerSources.PlayerTransition(actor, true);
    }

    void cancelPlayerTransition(ControllerSources.PlayerTransition transition) {
        if (!transition.suspended()) {
            return;
        }
        state.playerSessions().resume(transition.actor());
        sources.reconcilePlayer(transition.actor().getUUID());
    }

    void completePlayerTransition(ControllerSources.PlayerTransition transition,
                                  ServerPlayer replacement) {
        if (transition.suspended()) {
            state.playerSessions().remove(transition.actor());
        }
        if (replacement != null) {
            state.playerSessions().bind(replacement);
        }
    }

    boolean acceptInput(ServerPlayer actor, boolean press) {
        var session = state.playerSessions().bind(actor);
        var waiting = releaseRequired.get(actor.getUUID());
        if (waiting == null) {
            return true;
        }
        if (!waiting.equals(session) || !press) {
            releaseRequired.remove(actor.getUUID());
            return true;
        }
        return false;
    }

    void withdrawInvalidForInput(ServerPlayer actor) {
        RefreshScope changed = RefreshScope.none();
        for (var level : state.server().getAllLevels()) {
            var entries = LinkedControllerServerHandler.receivedInputs
                .get(level)
                .get(actor.getUUID());
            if (entries == null) {
                continue;
            }
            for (Object object : new ArrayList<>(entries)) {
                if (object instanceof ControllerEndpoint endpoint
                    && object instanceof IRedstoneLinkable link
                    && sources.scope(level, endpoint, null) == Scope.PENDING) {
                    changed = changed.union(sources.reconcileOne(level, link));
                }
            }
        }
        sources.refreshReconciled(changed);
    }

    void releaseDeadManaged(ServerPlayer actor) {
        var level = actor.serverLevel();
        var entries = LinkedControllerServerHandler.receivedInputs
            .get(level)
            .get(actor.getUUID());
        if (entries == null) {
            return;
        }
        for (Object object : new ArrayList<>(entries)) {
            if (object instanceof ControllerEndpoint
                && object instanceof IRedstoneLinkable link
                && !link.isAlive()) {
                entries.remove(object);
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, link);
            }
        }
    }

    void requireRelease(ServerPlayer actor) {
        for (var level : state.server().getAllLevels()) {
            var entries = LinkedControllerServerHandler.receivedInputs
                .get(level)
                .get(actor.getUUID());
            if (entries == null) {
                continue;
            }
            for (Object object : entries) {
                if (object instanceof IRedstoneLinkable link
                    && link.isAlive()
                    && link.getTransmittedStrength() > 0) {
                    releaseRequired.put(actor.getUUID(), state.playerSessions().bind(actor));
                    return;
                }
            }
        }
    }

    void invalidatePlayer(ServerPlayer actor) {
        requireRelease(actor);
        for (var level : state.server().getAllLevels()) {
            var entries = LinkedControllerServerHandler.receivedInputs
                .get(level)
                .get(actor.getUUID());
            if (entries == null) {
                continue;
            }
            for (Object object : new ArrayList<>(entries)) {
                if (!(object instanceof ControllerEndpoint endpoint)) {
                    continue;
                }
                endpoint.fieldwork$source(null);
                if (object instanceof IRedstoneLinkable link && !link.isAlive()) {
                    entries.remove(object);
                    Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, link);
                }
            }
        }
        sources.reconcilePlayer(actor.getUUID());
    }

    void playerRemoved(ServerPlayer actor) {
        releaseRequired.remove(actor.getUUID());
        if (!state.playerSessions().remove(actor)) {
            return;
        }
        try {
            sources.reconcilePlayer(actor.getUUID());
        } catch (RuntimeException | Error error) {
            LogUtils.getLogger().error(
                "Fieldwork controller withdrawal will retry after player logout",
                error
            );
        }
    }

    void clear() {
        current = null;
        releaseRequired.clear();
    }
}
