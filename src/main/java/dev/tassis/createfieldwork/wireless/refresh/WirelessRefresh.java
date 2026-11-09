package dev.tassis.createfieldwork.wireless.refresh;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.controller.routing.ControllerEndpoint;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Cold-path endpoint refresh; final-view permission is consumed by one exact upstream entry call. */
public final class WirelessRefresh {
    private record Permit(Object endpoint, TransitionCoordinator.View view) {}
    private record Frame(TransitionCoordinator.View view, boolean targeted) {}
    private final FieldworkServerState state;
    private final CreateReceiverRefreshPass createPass = new CreateReceiverRefreshPass();
    private final SimulatedReceiverRefreshPass simulatedPass = new SimulatedReceiverRefreshPass();
    private final Deque<Frame> calls = new ArrayDeque<>();
    private final Map<ResourceKey<Level>, Set<BlockPos>> simulatedPositions = new HashMap<>();
    private Permit permit;
    private boolean refreshing;
    private boolean dirty;
    private RefreshScope pending = RefreshScope.none();

    public WirelessRefresh(FieldworkServerState state) {
        this.state = state;
    }

    public void enter(ServerLevel level, Object endpoint) {
        state.checkThread();

        boolean targeted = permit != null && permit.endpoint == endpoint;
        var granted = targeted ? permit.view : null;
        if (targeted) {
            permit = null;
        } else if (refreshing) {
            dirty = true; // a callback may overwrite an already refreshed receiver
            pending = pending.union(network(level, endpoint));
        }

        calls.push(new Frame(granted, targeted));
    }

    public void exit() {
        state.checkThread();
        calls.pop();
    }

    public TransitionCoordinator.View currentView() {
        state.checkThread();
        return calls.isEmpty() ? null : calls.peek().view;
    }

    public boolean targeted() {
        state.checkThread();
        return !calls.isEmpty() && calls.peek().targeted;
    }

    public void remember(AbstractLinkedReceiverBlockEntity receiver) {
        state.checkThread();
        if (receiver.getLevel() instanceof ServerLevel level) {
            simulatedPositions.computeIfAbsent(level.dimension(), ignored -> new HashSet<>()).add(receiver.getBlockPos().immutable());
        }
    }

    public void unloaded(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        state.checkThread();
        var indexed = simulatedPositions.get(level.dimension());
        if (indexed == null) {
            return;
        }

        indexed.removeIf(pos -> (pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z);
        if (indexed.isEmpty()) {
            simulatedPositions.remove(level.dimension());
        }
    }

    /** Packed Simulated receivers may not enter any native radio bucket until a
     * later tick. Materialize/index their exact IBE types on cold chunk paths. */
    public void trackChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        state.checkThread();
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(WirelessRefresh::simulatedBlock)) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (simulatedBlock(section.getBlockState(x, y, z))) {
                            var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            if (level.getBlockEntity(pos) instanceof AbstractLinkedReceiverBlockEntity receiver) {
                                remember(receiver);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean simulatedBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof com.simibubi.create.foundation.block.IBE<?> block &&
            AbstractLinkedReceiverBlockEntity.class.isAssignableFrom(block.getBlockEntityClass());
    }

    public void refresh(TransitionCoordinator.View view) {
        RefreshScope all = RefreshScope.none();
        for (var level : state.server().getAllLevels()) {
            all = all.union(RefreshScope.level(level.dimension()));
        }

        refresh(view, all); // explicit full refresh for callers without an affected footprint (e.g. test setup)
    }

    public void refresh(TransitionCoordinator.View view, RefreshScope scope) {
        state.checkThread();

        if (refreshing) {
            if (view != null) {
                throw new IllegalStateException("Cannot commit a reentrant final refresh");
            }
            if (!scope.isEmpty()) {
                dirty = true;
                pending = pending.union(scope);
            }
            return;
        }

        if (scope.isEmpty()) {
            return;
        }

        refreshing = true;
        RefreshScope work = scope;

        try {
            for (int pass = 0; pass < 8; pass++) {
                work = work.union(pending);
                pending = RefreshScope.none();
                dirty = false;

                for (var selected : work.levels().entrySet()) {
                    ServerLevel level = state.server().getLevel(selected.getKey());
                    if (level == null) {
                        continue;
                    }
                    var selection = selected.getValue();
                    createPass.run(level, selection, view, this);
                    simulatedPass.run(
                        level,
                        simulatedPositions.get(level.dimension()),
                        selection,
                        view,
                        this
                    );
                }

                if (!dirty) {
                    return;
                }
            }

            throw new IllegalStateException("Fieldwork refresh did not stabilize after 8 passes");
        } finally {
            refreshing = false;
            permit = null;
            pending = RefreshScope.none();
        }
    }

    private RefreshScope network(ServerLevel level, Object endpoint) {
        if (endpoint instanceof IRedstoneLinkable link) {
            return RefreshScope.frequency(level.dimension(), link.getNetworkKey());
        }
        if (endpoint instanceof AbstractLinkedReceiverBlockEntity receiver) {
            var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
            if (behaviour != null) {
                return RefreshScope.frequency(level.dimension(), behaviour.getNetworkKey());
            }
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
            if (level == null) {
                continue;
            }

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
            if (indexed != null) {
                for (var pos : Set.copyOf(indexed)) {
                    if (!involved(level, pos, crafts, positions)) {
                        continue;
                    }
                    if (level.getBlockEntity(pos) instanceof AbstractLinkedReceiverBlockEntity receiver && !receiver.isRemoved()) {
                        var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
                        result = result.union(behaviour == null ? RefreshScope.level(dimension)
                            : RefreshScope.frequency(dimension, behaviour.getNetworkKey()));
                    }
                }
            }
        }
        return result;
    }

    public static boolean involved(ServerLevel level, BlockPos pos, Set<CraftKey> crafts,
                                   Set<TransitionCoordinator.Position> positions) {
        if (positions.contains(new TransitionCoordinator.Position(level.dimension(), pos))) {
            return true;
        }

        var owner = Sable.HELPER.getContaining(level, pos);
        return owner != null && crafts.contains(IsolationRuntime.key(owner));
    }

    void invoke(Object endpoint, TransitionCoordinator.View view, Runnable operation) {
        if (permit != null) {
            throw new IllegalStateException("Unconsumed final-view permission");
        }

        permit = new Permit(endpoint, view);
        try {
            operation.run();
        } finally {
            permit = null;
        }
    }

    public void clear() {
        state.checkThread();
        calls.clear();
        simulatedPositions.clear();
        permit = null;
        dirty = false;
        refreshing = false;
        pending = RefreshScope.none();
    }
}
