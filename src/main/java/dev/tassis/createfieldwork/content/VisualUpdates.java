package dev.tassis.createfieldwork.content;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.isolation.IsolationRegistry;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator.Position;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

/** Derived visuals never roll back a committed radio policy; failed synchronization is retried cold. */
public final class VisualUpdates {
    private final FieldworkServerState state;
    private final Set<Position> pending = new HashSet<>();
    public VisualUpdates(FieldworkServerState state) { this.state = state; }
    public int pendingCount() { state.checkThread(); return pending.size(); }

    public void update(ServerLevel level, BlockPos pos) {
        state.checkThread();
        Position position = new Position(level.dimension(), pos);
        try {
            boolean active = false;
            if (level.getBlockState(pos).is(FieldworkContent.ISOLATOR.get())) {
                var owner = Sable.HELPER.getContaining(level, pos);
                if (owner != null) {
                    var snapshot = state.registry().snapshot(IsolationRuntime.key(owner));
                    active = snapshot.status() == IsolationRegistry.Status.READY && snapshot.positions().contains(pos);
                }
                IsolationRuntime.visual(level, pos, active);
            }
            pending.remove(position);
        } catch (RuntimeException | LinkageError | AssertionError error) {
            if (pending.add(position)) LogUtils.getLogger().warn("Fieldwork visual synchronization queued at {}; radio policy remains committed", position, error);
        }
    }

    public void retry() {
        state.checkThread();
        int budget = 128;
        for (var position : Set.copyOf(pending)) {
            if (budget-- == 0) break;
            var level = state.server().getLevel(position.dimension());
            if (level == null) pending.remove(position);
            else if (level.hasChunkAt(position.pos())) update(level, position.pos());
        }
    }
    public void clear() { state.checkThread(); pending.clear(); }
}
