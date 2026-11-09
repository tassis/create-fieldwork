package dev.tassis.createfieldwork.controller.routing;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/** Endpoint publication mutation and retained physical-refresh obligations. */
final class ControllerReconciliation {
    private final FieldworkServerState state;
    private final ControllerSources sources;

    ControllerReconciliation(FieldworkServerState state, ControllerSources sources) {
        this.state = state;
        this.sources = sources;
    }

    RefreshScope stage(ControllerSources.Selection selection, TransitionCoordinator.View view) {
        RefreshScope changed = selection.networks();
        var work = selection.endpoints();
        for (int index = 0; index < work.size(); index++) {
            var link = work.get(index);
            var association = sources.endpoints.get(link);
            if (association == null) {
                continue;
            }
            try {
                changed = changed.union(sources.reconcileOneView(association.level(), link, view));
            } catch (RuntimeException | Error error) {
                for (int remaining = index; remaining < work.size(); remaining++) {
                    if (sources.endpoints.containsKey(work.get(remaining))) {
                        sources.dirty.add(work.get(remaining));
                    }
                }
                sources.reconcileRetry = sources.reconcileRetry.union(changed);
                throw error;
            }
        }
        return changed;
    }

    void drain() {
        RefreshScope changed = RefreshScope.none();
        var work = new ArrayList<>(sources.dirty);
        sources.dirty.removeAll(work);
        for (int index = 0; index < work.size(); index++) {
            var endpoint = work.get(index);
            var association = sources.endpoints.get(endpoint);
            if (association == null) {
                continue;
            }
            try {
                boolean radio = containsIdentity(
                    Create.REDSTONE_LINK_NETWORK_HANDLER
                        .networksIn(association.level())
                        .get(endpoint.getNetworkKey()),
                    endpoint
                );
                Collection<?> playerEntries = association.player() == null
                    ? null
                    : LinkedControllerServerHandler.receivedInputs
                        .get(association.level())
                        .get(association.player());
                boolean collection = containsIdentity(playerEntries, endpoint);
                changed = changed.union(
                    !radio || !collection
                        ? sources.unregister(association.level(), endpoint)
                        : sources.reconcileOne(association.level(), endpoint)
                );
            } catch (RuntimeException | Error error) {
                for (int remaining = index; remaining < work.size(); remaining++) {
                    if (sources.endpoints.containsKey(work.get(remaining))) {
                        sources.dirty.add(work.get(remaining));
                    }
                }
                try {
                    refreshReconciled(changed);
                } catch (RuntimeException | Error refreshError) {
                    error.addSuppressed(refreshError);
                }
                throw error;
            }
        }
        refreshReconciled(changed);
    }

    void reconcilePlayer(UUID player) {
        refreshReconciled(reconcileSelected(sources.byPlayer.get(player)));
    }

    void reconcileLectern(ResourceKey<Level> dimension, BlockPos position) {
        var key = new ControllerSources.LecternKey(dimension, position);
        refreshReconciled(reconcileSelected(sources.byLectern.get(key)));
    }

    boolean reconcileEndpoint(ServerLevel level, IRedstoneLinkable endpoint) {
        var changed = sources.reconcileOneView(level, endpoint, null);
        refreshReconciled(changed);
        return !changed.isEmpty();
    }

    RefreshScope reconcileOne(ServerLevel level, IRedstoneLinkable endpoint,
                              TransitionCoordinator.View view) {
        if (!(endpoint instanceof ControllerEndpoint tagged)) {
            return RefreshScope.none();
        }
        Scope next = sources.scope(level, tagged, view);
        var previous = tagged.fieldwork$publishedNetwork();
        boolean sameNetwork = previous != null
            && previous.isFrequency(level.dimension(), endpoint.getNetworkKey());
        var network = sameNetwork
            ? previous
            : RefreshScope.frequency(level.dimension(), endpoint.getNetworkKey());

        var oldPublished = tagged.fieldwork$published();
        var oldNetwork = tagged.fieldwork$publishedNetwork();
        var oldPrivate = tagged.fieldwork$lastPrivate();
        boolean oldRestricted = tagged.fieldwork$restricted();
        tagged.fieldwork$lastPrivate(
            next instanceof Scope.Private privateScope ? privateScope.craft() : null
        );
        tagged.fieldwork$restricted(next != Scope.WORLD);
        tagged.fieldwork$published(next);
        tagged.fieldwork$publishedNetwork(network);
        try {
            sources.registerOrUpdate(level, endpoint, tagged, next);
        } catch (RuntimeException | Error error) {
            tagged.fieldwork$published(oldPublished);
            tagged.fieldwork$publishedNetwork(oldNetwork);
            tagged.fieldwork$lastPrivate(oldPrivate);
            tagged.fieldwork$restricted(oldRestricted);
            throw error;
        }
        if (Objects.equals(next, oldPublished) && sameNetwork) {
            return RefreshScope.none();
        }
        return previous == null ? network : previous.union(network);
    }

    void nativeRemovalFailed(ServerLevel level, IRedstoneLinkable endpoint) {
        var association = sources.endpoints.get(endpoint);
        if (association == null || association.level() != level) {
            return;
        }
        sources.dirty.add(endpoint);
        sources.reconcileRetry = sources.reconcileRetry.union(
            RefreshScope.frequency(level.dimension(), endpoint.getNetworkKey())
        );
    }

    void levelUnloaded(ServerLevel level) {
        sources.reconcileRetry = sources.reconcileRetry.without(level.dimension());
    }

    void retain(RefreshScope scope) {
        sources.reconcileRetry = sources.reconcileRetry.union(scope);
    }

    void refreshReconciled(RefreshScope changed) {
        var scope = sources.reconcileRetry.union(changed);
        if (scope.isEmpty()) {
            return;
        }
        sources.reconcileRetry = RefreshScope.none();
        try {
            state.invalidateVisibility();
            state.refresh().refresh(null, scope);
        } catch (RuntimeException | Error error) {
            sources.reconcileRetry = sources.reconcileRetry.union(scope);
            throw error;
        }
    }

    void clear() {
        sources.reconcileRetry = RefreshScope.none();
    }

    private RefreshScope reconcileSelected(Collection<IRedstoneLinkable> selected) {
        RefreshScope changed = RefreshScope.none();
        if (selected == null) {
            return changed;
        }
        for (var endpoint : new ArrayList<>(selected)) {
            var association = sources.endpoints.get(endpoint);
            if (association != null) {
                changed = changed.union(sources.reconcileOneView(association.level(), endpoint, null));
            }
        }
        return changed;
    }

    private static boolean containsIdentity(Collection<?> values, Object expected) {
        if (values == null) {
            return false;
        }
        for (var value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }
}
