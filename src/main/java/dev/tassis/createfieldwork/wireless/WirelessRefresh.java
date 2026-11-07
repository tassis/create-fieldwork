package dev.tassis.createfieldwork.wireless;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.controller.ControllerEndpoint;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Cold-path endpoint refresh; final-view permission is consumed by one exact upstream entry call. */
public final class WirelessRefresh {
    private record Permit(Object endpoint, TransitionCoordinator.View view) {}
    private record Frame(TransitionCoordinator.View view, boolean targeted) {}
    private final FieldworkServerState state;
    private final Deque<Frame> calls = new ArrayDeque<>();
    private final Map<ResourceKey<Level>, Set<BlockPos>> simulatedPositions = new HashMap<>();
    private Permit permit;
    private boolean refreshing;
    private boolean dirty;
    private RefreshScope pending = RefreshScope.none();

    public WirelessRefresh(FieldworkServerState state) { this.state = state; }

    public void enter(ServerLevel level, Object endpoint) {
        state.checkThread();
        boolean targeted = permit != null && permit.endpoint == endpoint;
        var granted = targeted ? permit.view : null;
        if (targeted) permit = null;
        else if (refreshing) {
            dirty = true; // a callback may overwrite an already refreshed receiver
            pending = pending.union(network(level, endpoint));
        }
        calls.push(new Frame(granted, targeted));
    }
    public void exit() { state.checkThread(); calls.pop(); }
    public TransitionCoordinator.View currentView() { state.checkThread(); return calls.isEmpty() ? null : calls.peek().view; }
    public boolean targeted() { state.checkThread(); return !calls.isEmpty() && calls.peek().targeted; }

    public void remember(AbstractLinkedReceiverBlockEntity receiver) {
        state.checkThread();
        if (receiver.getLevel() instanceof ServerLevel level)
            simulatedPositions.computeIfAbsent(level.dimension(), ignored -> new HashSet<>()).add(receiver.getBlockPos().immutable());
    }

    public void refresh(TransitionCoordinator.View view) {
        RefreshScope all = RefreshScope.none();
        for (var level : state.server().getAllLevels()) all = all.union(RefreshScope.level(level.dimension()));
        refresh(view, all); // explicit full refresh for callers without an affected footprint (e.g. test setup)
    }

    public void refresh(TransitionCoordinator.View view, RefreshScope scope) {
        state.checkThread();
        if (refreshing) {
            if (view != null) throw new IllegalStateException("Cannot commit a reentrant final refresh");
            if (!scope.isEmpty()) { dirty = true; pending = pending.union(scope); }
            return;
        }
        if (scope.isEmpty()) return;
        refreshing = true;
        RefreshScope work = scope;
        try {
            for (int pass = 0; pass < 8; pass++) {
                work = work.union(pending);
                pending = RefreshScope.none();
                dirty = false;
                for (var selected : work.levels().entrySet()) {
                    ServerLevel level = state.server().getLevel(selected.getKey());
                    if (level == null) continue;
                    var selection = selected.getValue();
                    Set<IRedstoneLinkable> endpoints = Collections.newSetFromMap(new IdentityHashMap<>());
                    var networks = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level);
                    if (selection.all()) {
                        for (var bucket : new ArrayList<>(networks.values())) endpoints.addAll(new ArrayList<>(bucket));
                    } else for (var key : selection.frequencies()) {
                        var bucket = networks.get(key);
                        if (bucket != null) endpoints.addAll(new ArrayList<>(bucket));
                    }
                    for (var endpoint : endpoints) {
                        if (!endpoint.isAlive()) continue;
                        if (endpoint.isListening()) {
                            var key = endpoint.getNetworkKey();
                            if (selection.includes(key) && (!selection.filtered(key)
                                || selection.accepts(key, ScopeResolver.endpoint(level, endpoint, view))))
                                invoke(endpoint, view, () -> Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, endpoint));
                        }
                        if (endpoint instanceof LinkBehaviour behaviour && behaviour.blockEntity instanceof AbstractLinkedReceiverBlockEntity simulated)
                            remember(simulated);
                    }
                    var indexed = simulatedPositions.get(level.dimension());
                    if (indexed != null) for (var pos : Set.copyOf(indexed)) {
                        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof AbstractLinkedReceiverBlockEntity simulated && !simulated.isRemoved()) {
                            var behaviour = simulated.getBehaviour(LinkBehaviour.TYPE);
                            boolean matches;
                            if (behaviour == null) matches = selection.unrestrictedUnknown()
                                || selection.acceptsUnknown(ScopeResolver.at(level, pos, view));
                            else {
                                var key = behaviour.getNetworkKey();
                                matches = selection.includes(key) && (!selection.filtered(key)
                                    || selection.accepts(key, ScopeResolver.at(level, pos, view)));
                            }
                            if (matches) invoke(simulated, view, simulated::updateSignal);
                        } else indexed.remove(pos);
                    }
                }
                if (!dirty) return;
            }
            throw new IllegalStateException("Fieldwork refresh did not stabilize after 8 passes");
        } finally { refreshing = false; permit = null; pending = RefreshScope.none(); }
    }

    private RefreshScope network(ServerLevel level, Object endpoint) {
        if (endpoint instanceof IRedstoneLinkable link) return RefreshScope.frequency(level.dimension(), link.getNetworkKey());
        if (endpoint instanceof AbstractLinkedReceiverBlockEntity receiver) {
            var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
            if (behaviour != null) return RefreshScope.frequency(level.dimension(), behaviour.getNetworkKey());
        }
        return RefreshScope.level(level.dimension());
    }

    /** Capture raw ownership/provenance before registry.begin changes authorization; never scan blocks. */
    public RefreshScope affected(Set<ResourceKey<Level>> levels, Set<CraftKey> crafts,
                                  Set<TransitionCoordinator.Position> positions) {
        state.checkThread();
        RefreshScope result = state.controllers().affectedNetworks(crafts);
        for (var dimension : levels) {
            ServerLevel level = state.server().getLevel(dimension);
            if (level == null) continue;
            if (state.publications().unresolvedIn(level, crafts, positions)) {
                result = result.union(RefreshScope.level(dimension));
                continue;
            }
            for (var bucket : new ArrayList<>(Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).entrySet())) {
                for (var endpoint : new ArrayList<>(bucket.getValue())) {
                    boolean associated = endpoint instanceof ControllerEndpoint controller
                        && state.controllers().associated(controller, crafts);
                    if (associated || involved(level, endpoint.getLocation(), crafts, positions)) {
                        result = result.union(RefreshScope.frequency(dimension, bucket.getKey()));
                        break;
                    }
                }
            }
            var indexed = simulatedPositions.get(dimension);
            if (indexed != null) for (var pos : Set.copyOf(indexed)) {
                if (!involved(level, pos, crafts, positions)) continue;
                if (level.getBlockEntity(pos) instanceof AbstractLinkedReceiverBlockEntity receiver && !receiver.isRemoved()) {
                    var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
                    result = result.union(behaviour == null ? RefreshScope.level(dimension)
                        : RefreshScope.frequency(dimension, behaviour.getNetworkKey()));
                }
            }
        }
        return result;
    }

    public static boolean involved(ServerLevel level, BlockPos pos, Set<CraftKey> crafts,
                                   Set<TransitionCoordinator.Position> positions) {
        if (positions.contains(new TransitionCoordinator.Position(level.dimension(), pos))) return true;
        var owner = Sable.HELPER.getContaining(level, pos);
        return owner != null && crafts.contains(IsolationRuntime.key(owner));
    }

    private void invoke(Object endpoint, TransitionCoordinator.View view, Runnable operation) {
        if (permit != null) throw new IllegalStateException("Unconsumed final-view permission");
        permit = new Permit(endpoint, view);
        try { operation.run(); }
        finally { permit = null; }
    }

    public void clear() { state.checkThread(); calls.clear(); simulatedPositions.clear(); permit = null; dirty = false; refreshing = false; pending = RefreshScope.none(); }
}
