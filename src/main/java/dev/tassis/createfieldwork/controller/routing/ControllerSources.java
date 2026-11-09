package dev.tassis.createfieldwork.controller.routing;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.component.ItemContainerContents;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

/** Verified server packet provenance, stored without strong player or block-entity references. */
public final class ControllerSources {
    public record BindingChange(Set<UUID> devices, RefreshScope networks, Set<CraftKey> crafts) {}
    public record Selection(java.util.List<IRedstoneLinkable> endpoints, RefreshScope networks) {
        public Selection { endpoints = java.util.List.copyOf(endpoints); }
    }
    public record PlayerTransition(ServerPlayer actor, boolean suspended) {}
    record LecternKey(ResourceKey<Level> dimension, BlockPos position) {}
    record Association(ServerLevel level, UUID player, UUID device, LecternKey lectern,
                       Set<CraftKey> crafts) {}
    public record Source(UUID player, UUID session, ResourceKey<Level> dimension, BlockPos lectern,
                         WeakReference<BlockEntity> lecternEntity, UUID device, long bindingRevision,
                          ItemContainerContents program, net.minecraft.world.InteractionHand hand) {}
    /** Opaque pre-add state used to restore a duplicate or failed native registration exactly. */
    public static final class NativeAddSnapshot {
        private final IRedstoneLinkable endpoint;
        private final Association association;
        private final Source source;
        private final Scope published;
        private final boolean restricted;
        private final CraftKey lastPrivate;
        private final RefreshScope publishedNetwork;
        private final boolean dirty;

        private NativeAddSnapshot(IRedstoneLinkable endpoint, Association association, ControllerEndpoint tagged, boolean dirty) {
            this.endpoint = endpoint;
            this.association = association;
            this.source = tagged == null ? null : tagged.fieldwork$source();
            this.published = tagged == null ? null : tagged.fieldwork$published();
            this.restricted = tagged != null && tagged.fieldwork$restricted();
            this.lastPrivate = tagged == null ? null : tagged.fieldwork$lastPrivate();
            this.publishedNetwork = tagged == null ? null : tagged.fieldwork$publishedNetwork();
            this.dirty = dirty;
        }
    }

    private final FieldworkServerState state;
    private final ControllerSourceValidation validation;
    private final ControllerSourceIndex index;
    private final ControllerSourceSafetySweep safetySweep;
    private final ControllerInputSessions inputSessions;
    private final ControllerReconciliation reconciliation;

    RefreshScope reconcileRetry = RefreshScope.none();

    final IdentityHashMap<IRedstoneLinkable, Association> endpoints = new IdentityHashMap<>();
    final Map<UUID, Set<IRedstoneLinkable>> byPlayer = new java.util.HashMap<>();
    final Map<UUID, Set<IRedstoneLinkable>> byDevice = new java.util.HashMap<>();
    final Map<LecternKey, Set<IRedstoneLinkable>> byLectern = new java.util.HashMap<>();
    final IdentityHashMap<ServerLevel, Set<IRedstoneLinkable>> byLevel = new IdentityHashMap<>();
    final Map<CraftKey, Set<IRedstoneLinkable>> byCraft = new java.util.HashMap<>();
    final Set<IRedstoneLinkable> dirty = identitySet();
    private int postTicks;

    public ControllerSources(FieldworkServerState state) {
        this.state = state;
        validation = new ControllerSourceValidation(state);
        index = new ControllerSourceIndex(state, this);
        safetySweep = new ControllerSourceSafetySweep(state, this);
        inputSessions = new ControllerInputSessions(state, this);
        reconciliation = new ControllerReconciliation(state, this);
    }

    static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public Source begin(ServerPlayer actor, BlockPos lectern, net.minecraft.world.item.ItemStack controller) {
        state.checkThread();
        return inputSessions.begin(actor, lectern, controller);
    }

    public void end(Source previous) {
        state.checkThread();
        inputSessions.end(previous);
    }

    /** Each input capture makes new weak-reference wrappers; compare server provenance, not record identity. */
    public static boolean sameProvenance(Source before, Source after) {
        return ControllerSourceValidation.sameProvenance(before, after);
    }

    public void capture(IRedstoneLinkable endpoint, ServerLevel level) {
        state.checkThread();
        if (!(endpoint instanceof ControllerEndpoint tagged)) {
            return;
        }

        tagged.fieldwork$source(inputSessions.current());
        Scope scope = scope(level, tagged, null);

        // Keep an existing entry's last published scope until reconciliation
        // after updatePosition; that upstream method itself does not refresh.
        if (tagged.fieldwork$published() == null) {
            tagged.fieldwork$published(scope);
            tagged.fieldwork$publishedNetwork(RefreshScope.frequency(level.dimension(), endpoint.getNetworkKey()));
        }

        tagged.fieldwork$restricted(scope != Scope.WORLD);
        if (scope instanceof Scope.Private privateScope) {
            tagged.fieldwork$lastPrivate(privateScope.craft());
        } else if (scope == Scope.WORLD) {
            tagged.fieldwork$lastPrivate(null);
        }
    }

    /** Capture packet provenance while retaining enough state to undo an ambiguous add. */
    public NativeAddSnapshot beginNativeAdd(IRedstoneLinkable endpoint, ServerLevel level) {
        state.checkThread();
        var snapshot = snapshotNativeAdd(endpoint);
        capture(endpoint, level);
        return snapshot;
    }

    /** Snapshot without changing provenance, for an outer updatePosition transaction. */
    public NativeAddSnapshot snapshotNativeAdd(IRedstoneLinkable endpoint) {
        state.checkThread();
        var tagged = endpoint instanceof ControllerEndpoint controller ? controller : null;
        return new NativeAddSnapshot(endpoint, endpoints.get(endpoint), tagged, dirty.contains(endpoint));
    }

    /** Restore both endpoint metadata and the immutable reverse-index snapshot. */
    public void nativeAddFailed(NativeAddSnapshot snapshot) {
        state.checkThread();
        var endpoint = snapshot.endpoint;
        var currentAssociation = endpoints.get(endpoint);

        if (currentAssociation != snapshot.association) {
            if (currentAssociation != null) {
                index.removeAssociation(endpoint, currentAssociation);
            }
            endpoints.remove(endpoint);
            if (snapshot.association != null) {
                addAssociation(endpoint, snapshot.association);
                endpoints.put(endpoint, snapshot.association);
            }
        }

        if (endpoint instanceof ControllerEndpoint tagged) {
            tagged.fieldwork$source(snapshot.source);
            tagged.fieldwork$published(snapshot.published);
            tagged.fieldwork$restricted(snapshot.restricted);
            tagged.fieldwork$lastPrivate(snapshot.lastPrivate);
            tagged.fieldwork$publishedNetwork(snapshot.publishedNetwork);
        }

        if (snapshot.dirty && endpoints.containsKey(endpoint)) {
            dirty.add(endpoint);
        } else {
            dirty.remove(endpoint);
        }
    }

    public Scope scope(ServerLevel level, ControllerEndpoint endpoint, TransitionCoordinator.View view) {
        state.checkThread();
        return validation.scope(level, endpoint, view);
    }

    public boolean associated(ControllerEndpoint endpoint, Set<CraftKey> crafts) {
        state.checkThread();
        var source = endpoint.fieldwork$source();
        var target = source == null || source.device == null ? null : state.bindings().target(source.device, state.server().getLevel(source.dimension));
        return (endpoint.fieldwork$lastPrivate() != null && crafts.contains(endpoint.fieldwork$lastPrivate()))
            || (target != null && crafts.contains(target));
    }

    void addAssociation(IRedstoneLinkable endpoint, Association association) {
        index.addAssociation(endpoint, association);
    }

    /** Replace every reverse association from one immutable old snapshot. */
    void registerOrUpdate(ServerLevel level, IRedstoneLinkable endpoint, ControllerEndpoint tagged, Scope publication) {
        index.registerOrUpdate(level, endpoint, tagged, publication);
    }

    RefreshScope unregister(ServerLevel expectedLevel, IRedstoneLinkable endpoint) {
        return index.unregister(expectedLevel, endpoint);
    }

    /** Commit Fieldwork ownership only after native radio registration succeeds. */
    public void nativeAdded(ServerLevel level, IRedstoneLinkable endpoint) {
        state.checkThread();
        if (endpoint instanceof ControllerEndpoint tagged) {
            registerOrUpdate(level, endpoint, tagged, scope(level, tagged, null));
        }
    }

    /** Native removal owns exact unregister and old-network withdrawal. */
    public void nativeRemoved(ServerLevel level, IRedstoneLinkable endpoint) {
        state.checkThread();
        refreshReconciled(unregister(level, endpoint));
    }

    /** Native remove can mutate before throwing; retain ownership and schedule an
     * exact membership check rather than guessing which side committed. */
    public void nativeRemovalFailed(ServerLevel level, IRedstoneLinkable endpoint) {
        state.checkThread();
        reconciliation.nativeRemovalFailed(level, endpoint);
    }

    /** onUnloadWorld bypasses per-endpoint native removal. Clear only the level
     * whose upstream unload completed successfully. */
    public void levelUnloaded(ServerLevel level) {
        state.checkThread();
        var levelEndpoints = byLevel.get(level);
        if (levelEndpoints != null) {
            for (var endpoint : new ArrayList<>(levelEndpoints)) {
                unregister(level, endpoint);
            }
        }

        byLevel.remove(level);
        reconciliation.levelUnloaded(level);
    }

    public void markPlayer(UUID player) {
        state.checkThread();
        mark(byPlayer.get(player));
    }

    public void markDevices(Set<UUID> devices) {
        state.checkThread();
        for (var device : devices) {
            mark(byDevice.get(device));
        }
    }

    public void markLectern(ResourceKey<Level> dimension, BlockPos position) {
        state.checkThread();
        mark(byLectern.get(new LecternKey(dimension, position)));
    }

    public void markCrafts(Set<CraftKey> crafts) {
        state.checkThread();
        for (var craft : crafts) {
            mark(byCraft.get(craft));
        }
    }

    public void markEndpoint(IRedstoneLinkable endpoint) {
        state.checkThread();
        if (endpoints.containsKey(endpoint)) {
            dirty.add(endpoint);
        }
    }

    private void mark(Set<IRedstoneLinkable> selected) {
        if (selected != null) {
            dirty.addAll(selected);
        }
    }

    /** Immutable endpoint identities selected before a registry/barrier mutation.
     * The transaction retains this list even when quarantine changes associations. */
    public Selection select(Set<UUID> devices, Set<CraftKey> crafts, Set<TransitionCoordinator.Position> positions) {
        state.checkThread();
        return index.select(devices, crafts, positions);
    }

    /** Stage selected publication metadata against one coherent transition view.
     * Physical refresh remains coordinator-owned so barriers and candidate state
     * cannot be observed in different generations. */
    public RefreshScope stage(Selection selection, TransitionCoordinator.View view) {
        state.checkThread();
        return reconciliation.stage(selection, view);
    }

    public void reconcile(Selection selection) {
        refreshReconciled(stage(selection, null));
    }

    /** Drain only marked endpoints. Full diagnostic comparison is reserved for
     * the periodic safety sweep added by ctr-06. */
    public void drain() {
        state.checkThread();
        reconciliation.drain();
    }

    /** A controller entry can outlive a dimension transfer for its upstream
     * timeout. Include related old dimensions without scanning their radios. */
    public RefreshScope affectedNetworks(Set<CraftKey> crafts) {
        state.checkThread();
        return index.affectedNetworks(crafts);
    }

    public void reconcilePlayer(UUID player) {
        state.checkThread();
        reconciliation.reconcilePlayer(player);
    }

    public void reconcileLectern(ResourceKey<Level> dimension, BlockPos position) {
        state.checkThread();
        reconciliation.reconcileLectern(dimension, position);
    }

    public PlayerTransition beginPlayerTransition(ServerPlayer actor) {
        state.checkThread();
        return inputSessions.beginPlayerTransition(actor);
    }

    public void cancelPlayerTransition(PlayerTransition transition) {
        state.checkThread();
        inputSessions.cancelPlayerTransition(transition);
    }

    public void completePlayerTransition(PlayerTransition transition, ServerPlayer replacement) {
        state.checkThread();
        inputSessions.completePlayerTransition(transition, replacement);
    }

    public boolean reconcileEndpoint(ServerLevel level, IRedstoneLinkable endpoint) {
        state.checkThread();
        return reconciliation.reconcileEndpoint(level, endpoint);
    }

    RefreshScope reconcileOne(ServerLevel level, IRedstoneLinkable link) {
        return reconcileOneView(level, link, null);
    }

    RefreshScope reconcileOneView(ServerLevel level, IRedstoneLinkable link, TransitionCoordinator.View view) {
        state.checkThread();
        return reconciliation.reconcileOne(level, link, view);
    }

    /** Commit a refresh obligation through the same retained retry ledger as reconciliation. */
    public void refreshManagedAdd(RefreshScope scope) {
        state.checkThread();
        refreshReconciled(scope);
    }

    /** Coordinator refreshes share this exact retry ledger without performing
     * endpoint discovery or waiting for the periodic safety sweep. */
    public void retainRefresh(RefreshScope scope) {
        state.checkThread();
        reconciliation.retain(scope);
    }

    /** Endpoint publication metadata changes before the physical refresh. Keep
     * the exact scope when that refresh fails so a later tick cannot mistake the
     * already-updated metadata for a completed reconciliation. */
    void refreshReconciled(RefreshScope changed) {
        reconciliation.refreshReconciled(changed);
    }

    public boolean acceptInput(ServerPlayer actor, boolean press) {
        state.checkThread();
        return inputSessions.acceptInput(actor, press);
    }

    /** Reprogramming/hand/session changes can create a different native frequency
     * entry. Withdraw invalid old entries first, but never publish a new scope
     * using their old position before native updatePosition has run. */
    public void withdrawInvalidForInput(ServerPlayer actor) {
        state.checkThread();
        inputSessions.withdrawInvalidForInput(actor);
    }

    /** Native release sets TTL0 but retains the entry until tick. A synchronous
     * refresh would prune its radio membership first; remove both indexes together
     * so a subsequent press cannot reuse an unregistered entry. */
    public void releaseDeadManaged(ServerPlayer actor) {
        state.checkThread();
        inputSessions.releaseDeadManaged(actor);
    }

    public void requireRelease(ServerPlayer actor) {
        state.checkThread();
        inputSessions.requireRelease(actor);
    }

    public void invalidatePlayer(ServerPlayer actor) {
        state.checkThread();
        inputSessions.invalidatePlayer(actor);
    }

    public void playerRemoved(ServerPlayer actor) {
        state.checkThread();
        inputSessions.playerRemoved(actor);
    }

    /** Cold mutation footprint, captured BEFORE revocation/source invalidation. */
    public BindingChange bindingChange(UUID player, Set<UUID> devices) {
        state.checkThread();
        return index.bindingChange(player, devices);
    }

    public void changedDevices(Set<UUID> devices) {
        reconcile(select(devices, Set.of(), Set.of()));
    }

    /** Sole ordinary server-post owner: dirty/retry work every tick, one complete
     * diagnostic comparison per 200 server ticks. */
    public void postTick() {
        state.checkThread();
        drain();
        if (++postTicks == 200) {
            postTicks = 0;
            safetySweep();
        }
    }

    /** Stable counter seam; the diagnostic implementation lives in its collaborator. */
    private void safetySweep() {
        refreshReconciled(safetySweep.run());
    }

    public void clear() {
        state.checkThread();
        inputSessions.clear();
        reconciliation.clear();
        postTicks = 0;
        dirty.clear();
        endpoints.clear();
        byPlayer.clear();
        byDevice.clear();
        byLectern.clear();
        byLevel.clear();
        byCraft.clear();
    }
}
