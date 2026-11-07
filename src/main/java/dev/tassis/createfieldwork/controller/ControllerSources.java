package dev.tassis.createfieldwork.controller;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.Scope;
import dev.tassis.createfieldwork.wireless.ScopeResolver;
import dev.tassis.createfieldwork.wireless.RefreshScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

/** Verified server packet provenance, stored without strong player or block-entity references. */
public final class ControllerSources {
    public record Source(UUID player, UUID session, ResourceKey<Level> dimension, BlockPos lectern,
                         CraftKey lecternCraft, WeakReference<BlockEntity> lecternEntity) {}
    private final FieldworkServerState state;
    private Source current;

    public ControllerSources(FieldworkServerState state) { this.state = state; }

    public Source begin(ServerPlayer actor, BlockPos lectern) {
        state.checkThread();
        var previous = current;
        var level = actor.serverLevel();
        var owner = lectern == null ? null : Sable.HELPER.getContaining(level, lectern);
        current = new Source(actor.getUUID(), state.players().bind(actor), level.dimension(), lectern == null ? null : lectern.immutable(),
            owner == null ? null : IsolationRuntime.key(owner), new WeakReference<>(lectern == null ? null : level.getBlockEntity(lectern)));
        return previous;
    }
    public void end(Source previous) { state.checkThread(); current = previous; }

    /** Each input capture makes new weak-reference wrappers; compare server provenance, not record identity. */
    public static boolean sameProvenance(Source before, Source after) {
        if (before == null || after == null) return before == after;
        return before.player.equals(after.player) && before.session.equals(after.session)
            && before.dimension.equals(after.dimension) && Objects.equals(before.lectern, after.lectern)
            && Objects.equals(before.lecternCraft, after.lecternCraft)
            && (before.lectern == null || before.lecternEntity.get() == after.lecternEntity.get());
    }

    public void capture(IRedstoneLinkable endpoint, ServerLevel level) {
        state.checkThread();
        if (!(endpoint instanceof ControllerEndpoint tagged)) return;
        tagged.fieldwork$source(current);
        Scope scope = scope(level, tagged, null);
        // Keep an existing entry's last published scope until reconciliation
        // after updatePosition; that upstream method itself does not refresh.
        if (tagged.fieldwork$published() == null) {
            tagged.fieldwork$published(scope);
            tagged.fieldwork$publishedNetwork(RefreshScope.frequency(level.dimension(), endpoint.getNetworkKey()));
        }
        tagged.fieldwork$restricted(scope != Scope.WORLD);
        if (scope instanceof Scope.Private privateScope) tagged.fieldwork$lastPrivate(privateScope.craft());
        else if (scope == Scope.WORLD) tagged.fieldwork$lastPrivate(null);
    }

    public Scope scope(ServerLevel level, ControllerEndpoint endpoint, TransitionCoordinator.View view) {
        state.checkThread();
        var source = endpoint.fieldwork$source();
        if (source == null) return Scope.PENDING;
        var last = endpoint.fieldwork$lastPrivate();
        boolean historyRestricted = endpoint.fieldwork$restricted() && (last == null || !state.players().publicCraft(last, view));
        boolean restricted = historyRestricted || state.players().restricted(source.player, source.session, view)
            || (source.lecternCraft != null && !state.players().publicCraft(source.lecternCraft, view));
        var actor = state.players().actor(source.player, source.session);
        boolean valid = actor != null && !actor.isRemoved() && !actor.isDeadOrDying() && actor.level() == level
            && actor.connection != null && actor.connection.isAcceptingMessages() && source.dimension.equals(level.dimension());
        if (!valid) return restricted ? Scope.PENDING : Scope.WORLD; // preserve upstream WORLD timeout semantics
        if (source.lectern == null) {
            Scope result = state.players().scope(source.player, source.session, level, view);
            return result == Scope.PENDING && !restricted ? Scope.WORLD : result;
        }
        var block = level.getBlockEntity(source.lectern);
        if (!(block instanceof LecternControllerBlockEntity) || block != source.lecternEntity.get()) return restricted ? Scope.PENDING : Scope.WORLD;
        var owner = Sable.HELPER.getContaining(level, source.lectern);
        if (!Objects.equals(source.lecternCraft, owner == null ? null : IsolationRuntime.key(owner))) return Scope.PENDING;
        return ScopeResolver.at(level, source.lectern, view);
    }

    public boolean associated(ControllerEndpoint endpoint, Set<CraftKey> crafts) {
        state.checkThread();
        var source = endpoint.fieldwork$source();
        return (endpoint.fieldwork$lastPrivate() != null && crafts.contains(endpoint.fieldwork$lastPrivate()))
            || (source != null && ((source.lecternCraft != null && crafts.contains(source.lecternCraft))
                || state.players().associated(source.player, source.session, crafts)));
    }

    /** A controller entry can outlive a dimension transfer for its upstream
     * timeout. Include related old dimensions without scanning their radios. */
    public RefreshScope affectedNetworks(Set<CraftKey> crafts) {
        state.checkThread();
        RefreshScope result = RefreshScope.none();
        if (crafts.isEmpty()) return result;
        for (var level : state.server().getAllLevels()) {
            for (Object collection : new ArrayList<>(LinkedControllerServerHandler.receivedInputs.get(level).values())) {
                for (Object object : new ArrayList<>((Collection<?>) collection)) {
                    if (!(object instanceof ControllerEndpoint endpoint) || !(object instanceof IRedstoneLinkable link)
                        || !associated(endpoint, crafts)) continue;
                    var previous = endpoint.fieldwork$publishedNetwork();
                    if (previous != null) result = result.union(previous);
                    result = result.union(RefreshScope.frequency(level.dimension(), link.getNetworkKey()));
                }
            }
        }
        return result;
    }

    public void reconcilePlayer(UUID player) {
        state.checkThread();
        RefreshScope changed = RefreshScope.none();
        for (var level : state.server().getAllLevels()) {
            var entries = LinkedControllerServerHandler.receivedInputs.get(level).get(player);
            if (entries != null) changed = changed.union(reconcileEntries(level, entries));
        }
        refresh(changed);
    }

    public void reconcile(ServerLevel level) {
        state.checkThread();
        RefreshScope changed = RefreshScope.none();
        for (Object collection : new ArrayList<>(LinkedControllerServerHandler.receivedInputs.get(level).values()))
            changed = changed.union(reconcileEntries(level, (Collection<?>) collection));
        refresh(changed);
    }

    public boolean reconcileEndpoint(ServerLevel level, IRedstoneLinkable endpoint) {
        var changed = reconcileOne(level, endpoint);
        refresh(changed);
        return !changed.isEmpty();
    }

    private RefreshScope reconcileEntries(ServerLevel level, Collection<?> entries) {
        RefreshScope changed = RefreshScope.none();
        for (Object object : new ArrayList<>(entries))
            if (object instanceof IRedstoneLinkable endpoint) changed = changed.union(reconcileOne(level, endpoint));
        return changed;
    }

    private RefreshScope reconcileOne(ServerLevel level, IRedstoneLinkable link) {
        state.checkThread();
        if (!(link instanceof ControllerEndpoint endpoint)) return RefreshScope.none();
        Scope next = scope(level, endpoint, null);
        var previous = endpoint.fieldwork$publishedNetwork();
        boolean sameNetwork = previous != null && previous.isFrequency(level.dimension(), link.getNetworkKey());
        var network = sameNetwork ? previous : RefreshScope.frequency(level.dimension(), link.getNetworkKey());
        if (next instanceof Scope.Private privateScope) endpoint.fieldwork$lastPrivate(privateScope.craft());
        endpoint.fieldwork$restricted(next != Scope.WORLD);
        if (next == Scope.WORLD) endpoint.fieldwork$lastPrivate(null);
        if (Objects.equals(next, endpoint.fieldwork$published()) && sameNetwork) return RefreshScope.none();
        endpoint.fieldwork$published(next);
        endpoint.fieldwork$publishedNetwork(network);
        return previous == null ? network : previous.union(network);
    }

    private void refresh(RefreshScope scope) {
        if (!scope.isEmpty()) { state.invalidateVisibility(); state.refresh().refresh(null, scope); }
    }

    public void clear() { state.checkThread(); current = null; }
}
