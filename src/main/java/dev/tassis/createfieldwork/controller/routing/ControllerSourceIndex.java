package dev.tassis.createfieldwork.controller.routing;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable association snapshots and reverse-index mutation for controller endpoints. */
final class ControllerSourceIndex {
    private final FieldworkServerState state;
    private final ControllerSources sources;

    ControllerSourceIndex(FieldworkServerState state, ControllerSources sources) {
        this.state = state;
        this.sources = sources;
    }

    void registerOrUpdate(ServerLevel level, IRedstoneLinkable link,
                          ControllerEndpoint endpoint, Scope publication) {
        var next = association(level, endpoint, publication);
        var previous = sources.endpoints.get(link);
        if (next.equals(previous)) {
            return;
        }
        if (previous != null) {
            removeAssociation(link, previous);
        }
        try {
            addAssociation(link, next);
            sources.endpoints.put(link, next);
        } catch (RuntimeException | Error error) {
            removeAssociation(link, next);
            sources.endpoints.remove(link);
            if (previous != null) {
                try {
                    addAssociation(link, previous);
                    sources.endpoints.put(link, previous);
                } catch (RuntimeException | Error rollback) {
                    error.addSuppressed(rollback);
                }
            }
            throw error;
        }
    }

    RefreshScope unregister(ServerLevel expectedLevel, IRedstoneLinkable endpoint) {
        var association = sources.endpoints.get(endpoint);
        if (association == null || association.level() != expectedLevel) {
            return RefreshScope.none();
        }
        sources.endpoints.remove(endpoint);
        removeAssociation(endpoint, association);
        sources.dirty.remove(endpoint);

        RefreshScope result = RefreshScope.frequency(expectedLevel.dimension(), endpoint.getNetworkKey());
        if (endpoint instanceof ControllerEndpoint tagged
            && tagged.fieldwork$publishedNetwork() != null) {
            result = result.union(tagged.fieldwork$publishedNetwork());
        }
        return result;
    }

    ControllerSources.Selection select(Set<UUID> devices, Set<CraftKey> crafts,
                                       Set<TransitionCoordinator.Position> positions) {
        Set<IRedstoneLinkable> selected = ControllerSources.identitySet();
        addSelected(sources.byDevice, devices, selected);
        addSelected(sources.byCraft, crafts, selected);
        for (var position : positions) {
            var values = sources.byLectern.get(
                new ControllerSources.LecternKey(position.dimension(), position.pos())
            );
            if (values != null) {
                selected.addAll(values);
            }
        }
        return new ControllerSources.Selection(new ArrayList<>(selected), networks(selected));
    }

    RefreshScope affectedNetworks(Set<CraftKey> crafts) {
        if (crafts.isEmpty()) {
            return RefreshScope.none();
        }
        Set<IRedstoneLinkable> selected = ControllerSources.identitySet();
        addSelected(sources.byCraft, crafts, selected);
        return networks(selected);
    }

    ControllerSources.BindingChange bindingChange(UUID player, Set<UUID> devices) {
        Set<IRedstoneLinkable> selected = ControllerSources.identitySet();
        var playerEndpoints = sources.byPlayer.get(player);
        if (playerEndpoints != null) {
            selected.addAll(playerEndpoints);
        }
        addSelected(sources.byDevice, devices, selected);

        RefreshScope networks = RefreshScope.none();
        Set<CraftKey> crafts = new HashSet<>();
        for (var link : selected) {
            if (!(link instanceof ControllerEndpoint endpoint)) {
                continue;
            }
            var association = sources.endpoints.get(link);
            if (association == null) {
                continue;
            }
            var previous = endpoint.fieldwork$publishedNetwork();
            if (previous != null) {
                networks = networks.union(previous);
            }
            networks = networks.union(
                RefreshScope.frequency(association.level().dimension(), link.getNetworkKey())
            );
            crafts.addAll(association.crafts());
        }
        return new ControllerSources.BindingChange(Set.copyOf(devices), networks, Set.copyOf(crafts));
    }

    void addAssociation(IRedstoneLinkable endpoint, ControllerSources.Association association) {
        add(sources.byPlayer, association.player(), endpoint);
        add(sources.byDevice, association.device(), endpoint);
        add(sources.byLectern, association.lectern(), endpoint);
        sources.byLevel.computeIfAbsent(
            association.level(),
            ignored -> ControllerSources.identitySet()
        ).add(endpoint);
        for (var craft : association.crafts()) {
            add(sources.byCraft, craft, endpoint);
        }
    }

    private ControllerSources.Association association(ServerLevel level, ControllerEndpoint endpoint,
                                                      Scope publication) {
        var source = endpoint.fieldwork$source();
        if (source == null) {
            Set<CraftKey> crafts = publication instanceof Scope.Private privateScope
                ? Set.of(privateScope.craft())
                : Set.of();
            return new ControllerSources.Association(level, null, null, null, crafts);
        }

        Set<CraftKey> crafts = new HashSet<>();
        if (publication instanceof Scope.Private privateScope) {
            crafts.add(privateScope.craft());
        }
        if (source.device() != null) {
            var targetLevel = state.server().getLevel(source.dimension());
            var target = targetLevel == null
                ? null
                : state.bindings().target(source.device(), targetLevel);
            if (target != null) {
                crafts.add(target);
            }
        }
        if (source.lectern() != null && source.dimension().equals(level.dimension())) {
            var physical = Sable.HELPER.getContaining(level, source.lectern());
            if (physical != null) {
                crafts.add(IsolationRuntime.key(physical));
            }
        }
        var lectern = source.lectern() == null
            ? null
            : new ControllerSources.LecternKey(source.dimension(), source.lectern());
        return new ControllerSources.Association(
            level,
            source.player(),
            source.device(),
            lectern,
            Set.copyOf(crafts)
        );
    }

    void removeAssociation(IRedstoneLinkable endpoint,
                           ControllerSources.Association association) {
        remove(sources.byPlayer, association.player(), endpoint);
        remove(sources.byDevice, association.device(), endpoint);
        remove(sources.byLectern, association.lectern(), endpoint);
        var levelEndpoints = sources.byLevel.get(association.level());
        if (levelEndpoints != null) {
            levelEndpoints.remove(endpoint);
            if (levelEndpoints.isEmpty()) {
                sources.byLevel.remove(association.level());
            }
        }
        for (var craft : association.crafts()) {
            remove(sources.byCraft, craft, endpoint);
        }
    }

    private RefreshScope networks(Set<IRedstoneLinkable> selected) {
        RefreshScope result = RefreshScope.none();
        for (var link : selected) {
            var association = sources.endpoints.get(link);
            if (association == null) {
                continue;
            }
            result = result.union(
                RefreshScope.frequency(association.level().dimension(), link.getNetworkKey())
            );
            if (link instanceof ControllerEndpoint endpoint
                && endpoint.fieldwork$publishedNetwork() != null) {
                result = result.union(endpoint.fieldwork$publishedNetwork());
            }
        }
        return result;
    }

    private static <K> void addSelected(Map<K, Set<IRedstoneLinkable>> index,
                                        Set<K> keys, Set<IRedstoneLinkable> selected) {
        for (var key : keys) {
            var values = index.get(key);
            if (values != null) {
                selected.addAll(values);
            }
        }
    }

    private static <K> void add(Map<K, Set<IRedstoneLinkable>> index, K key,
                                IRedstoneLinkable endpoint) {
        if (key != null) {
            index.computeIfAbsent(key, ignored -> ControllerSources.identitySet()).add(endpoint);
        }
    }

    private static <K> void remove(Map<K, Set<IRedstoneLinkable>> index, K key,
                                   IRedstoneLinkable endpoint) {
        if (key == null) {
            return;
        }
        var values = index.get(key);
        if (values == null) {
            return;
        }
        values.remove(endpoint);
        if (values.isEmpty()) {
            index.remove(key);
        }
    }
}
