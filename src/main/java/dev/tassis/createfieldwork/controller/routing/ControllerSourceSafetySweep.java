package dev.tassis.createfieldwork.controller.routing;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Periodic diagnostic repair kept separate from ordinary dirty-endpoint work. */
final class ControllerSourceSafetySweep {
    private final FieldworkServerState state;
    private final ControllerSources sources;

    ControllerSourceSafetySweep(FieldworkServerState state, ControllerSources sources) {
        this.state = state;
        this.sources = sources;
    }

    RefreshScope run() {
        RefreshScope changed = RefreshScope.none();
        for (var level : state.server().getAllLevels()) {
            changed = changed.union(run(level));
        }
        return changed;
    }

    private RefreshScope run(ServerLevel level) {
        Map<IRedstoneLinkable, Map<java.util.UUID, Collection<?>>> owners = ownerEntries(level);
        Map<?, ?> levelRadios = com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level);
        Map<IRedstoneLinkable, Set<Object>> radioBuckets = radioBuckets(levelRadios);

        Set<IRedstoneLinkable> canonical = ControllerSources.identitySet();
        sources.endpoints.forEach((endpoint, association) -> {
            if (association.level() == level) {
                canonical.add(endpoint);
            }
        });
        Set<IRedstoneLinkable> indexedLevel = sources.byLevel.get(level) == null
            ? ControllerSources.identitySet()
            : identityCopy(sources.byLevel.get(level));
        Set<IRedstoneLinkable> all = ControllerSources.identitySet();
        all.addAll(owners.keySet());
        all.addAll(radioBuckets.keySet());
        all.addAll(canonical);
        all.addAll(indexedLevel);

        RefreshScope changed = RefreshScope.none();
        for (var link : all) {
            changed = changed.union(repairOne(level, levelRadios, owners, radioBuckets, link, changed));
        }
        return changed;
    }

    private RefreshScope repairOne(ServerLevel level, Map<?, ?> levelRadios,
                                   Map<IRedstoneLinkable, Map<java.util.UUID, Collection<?>>> owners,
                                   Map<IRedstoneLinkable, Set<Object>> radioBuckets,
                                   IRedstoneLinkable link, RefreshScope changedSoFar) {
        var association = sources.endpoints.get(link);
        var source = link instanceof ControllerEndpoint endpoint ? endpoint.fieldwork$source() : null;
        var linkOwners = owners.getOrDefault(link, Map.of());
        var buckets = radioBuckets.getOrDefault(link, Set.of());
        boolean expectedOwner = source != null && linkOwners.containsKey(source.player());
        boolean exactRadio = buckets.contains(link.getNetworkKey());
        boolean provenance = source != null && source.dimension().equals(level.dimension());

        RefreshScope retained = changedSoFar;
        try {
            if (expectedOwner && exactRadio && provenance) {
                for (var owner : linkOwners.entrySet()) {
                    if (!owner.getKey().equals(source.player())) {
                        owner.getValue().removeIf(candidate -> candidate == link);
                    }
                }
                removeWrongRadioBuckets(levelRadios, link, link.getNetworkKey());
                RefreshScope changed = RefreshScope.none();
                if (association != null && association.level() != level) {
                    changed = changed.union(sources.unregister(association.level(), link));
                    retained = retained.union(changed);
                }
                if (!sources.endpoints.containsKey(link)) {
                    sources.nativeAdded(level, link);
                }
                repairAssociationIndexes(link, sources.endpoints.get(link));
                return changed.union(sources.reconcileOne(level, link));
            }

            RefreshScope changed;
            if (association != null) {
                changed = sources.unregister(association.level(), link);
            } else {
                changed = RefreshScope.frequency(level.dimension(), link.getNetworkKey());
                if (link instanceof ControllerEndpoint endpoint
                    && endpoint.fieldwork$publishedNetwork() != null) {
                    changed = changed.union(endpoint.fieldwork$publishedNetwork());
                }
            }
            retained = retained.union(changed);
            // Preserve publication/network obligations before native cleanup can
            // throw or invoke a third-party callback.
            sources.retainRefresh(retained);
            for (var owner : linkOwners.values()) {
                owner.removeIf(candidate -> candidate == link);
            }
            removeWrongRadioBuckets(levelRadios, link, null);
            return changed;
        } catch (RuntimeException | Error error) {
            sources.retainRefresh(retained);
            if (sources.endpoints.containsKey(link)) {
                sources.dirty.add(link);
            }
            throw error;
        }
    }

    private static Map<IRedstoneLinkable, Map<java.util.UUID, Collection<?>>> ownerEntries(ServerLevel level) {
        Map<IRedstoneLinkable, Map<java.util.UUID, Collection<?>>> owners = new IdentityHashMap<>();
        Map<java.util.UUID, ?> levelInputs = LinkedControllerServerHandler.receivedInputs.get(level);
        for (var ownerEntry : levelInputs.entrySet()) {
            var values = (Collection<?>) ownerEntry.getValue();
            for (Object object : new ArrayList<>(values)) {
                if (object instanceof IRedstoneLinkable link) {
                    owners.computeIfAbsent(link, ignored -> new HashMap<>()).put(ownerEntry.getKey(), values);
                }
            }
        }
        return owners;
    }

    private static Map<IRedstoneLinkable, Set<Object>> radioBuckets(Map<?, ?> levelRadios) {
        Map<IRedstoneLinkable, Set<Object>> result = new IdentityHashMap<>();
        for (var bucketEntry : levelRadios.entrySet()) {
            for (Object object : new ArrayList<>((Collection<?>) bucketEntry.getValue())) {
                if (object instanceof IRedstoneLinkable link && object instanceof ControllerEndpoint) {
                    result.computeIfAbsent(link, ignored -> new HashSet<>()).add(bucketEntry.getKey());
                }
            }
        }
        return result;
    }

    private static Set<IRedstoneLinkable> identityCopy(Collection<IRedstoneLinkable> input) {
        Set<IRedstoneLinkable> copy = ControllerSources.identitySet();
        copy.addAll(input);
        return copy;
    }

    private static void removeWrongRadioBuckets(Map<?, ?> radios, IRedstoneLinkable endpoint, Object keep) {
        for (Object bucketObject : radios.values()) {
            var bucket = (Collection<?>) bucketObject;
            boolean retained = keep != null
                && bucket.stream().anyMatch(candidate -> candidate == endpoint)
                && radios.entrySet().stream().anyMatch(entry ->
                    entry.getValue() == bucket && Objects.equals(entry.getKey(), keep));
            if (!retained) {
                bucket.removeIf(candidate -> candidate == endpoint);
            }
        }
    }

    /** Scrub stale reverse memberships before restoring one immutable snapshot. */
    private void repairAssociationIndexes(IRedstoneLinkable endpoint,
                                          ControllerSources.Association association) {
        if (association == null) {
            return;
        }
        scrub(sources.byPlayer, endpoint);
        scrub(sources.byDevice, endpoint);
        scrub(sources.byLectern, endpoint);
        scrub(sources.byLevel, endpoint);
        scrub(sources.byCraft, endpoint);
        sources.addAssociation(endpoint, association);
    }

    private static <K> void scrub(Map<K, Set<IRedstoneLinkable>> index, IRedstoneLinkable endpoint) {
        var iterator = index.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            entry.getValue().remove(endpoint);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }
}
