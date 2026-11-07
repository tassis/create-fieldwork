package dev.tassis.createfieldwork.isolation;

import dev.tassis.createfieldwork.wireless.Scope;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Server-thread-only registry. Full reconstruction publishes one immutable position set. */
public final class IsolationRegistry {
    public enum Status { INITIALIZING, READY, FAILED }

    public record Snapshot(Status status, Set<BlockPos> positions) {
        public Snapshot {
            Objects.requireNonNull(status, "status");
            positions = immutablePositions(positions);
            if (status != Status.READY && !positions.isEmpty())
                throw new IllegalArgumentException("Only READY snapshots contain authoritative positions");
        }
    }

    private static final Snapshot INITIALIZING = new Snapshot(Status.INITIALIZING, Set.of());
    private static final Snapshot FAILED = new Snapshot(Status.FAILED, Set.of());
    private final Map<CraftKey, Snapshot> crafts = new HashMap<>();
    private final Runnable checkThread;
    private final Runnable changed;

    public IsolationRegistry(Runnable checkThread, Runnable changed) {
        this.checkThread = Objects.requireNonNull(checkThread, "checkThread");
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** Missing known craft is INITIALIZING, never READY(empty). Does not insert or rebuild. */
    public Snapshot snapshot(CraftKey key) {
        checkThread.run();
        return crafts.getOrDefault(Objects.requireNonNull(key, "key"), INITIALIZING);
    }

    public Scope scope(CraftKey key) {
        Snapshot snapshot = snapshot(key);
        if (snapshot.status == Status.FAILED) return Scope.PENDING;
        return snapshot.status == Status.READY && snapshot.positions.isEmpty() ? Scope.WORLD : Scope.privateCraft(key);
    }

    public void begin(CraftKey key) { put(key, INITIALIZING); }

    public void fail(CraftKey key) { put(key, FAILED); }

    public void publish(CraftKey key, Collection<BlockPos> positions) {
        checkThread.run();
        put(key, new Snapshot(Status.READY, immutablePositions(positions)));
    }

    /** Incremental events cannot accidentally complete a partial reconstruction. */
    public void marker(CraftKey key, BlockPos position, boolean present) {
        Snapshot before = snapshot(key);
        if (before.status != Status.READY) throw new IllegalStateException("Craft is not READY: " + key);
        Set<BlockPos> positions = new HashSet<>(before.positions);
        if (present) positions.add(Objects.requireNonNull(position, "position").immutable());
        else positions.remove(Objects.requireNonNull(position, "position"));
        put(key, new Snapshot(Status.READY, positions));
    }

    /** A removed craft resolves private if queried by its old key; it never inherits a new plot owner. */
    public void forget(CraftKey key) {
        checkThread.run();
        if (crafts.remove(Objects.requireNonNull(key, "key")) != null) changed.run();
    }

    public void clear() {
        checkThread.run();
        if (!crafts.isEmpty()) { crafts.clear(); changed.run(); }
    }

    private void put(CraftKey key, Snapshot value) {
        checkThread.run();
        Snapshot before = crafts.put(Objects.requireNonNull(key, "key"), value);
        if (!value.equals(before)) changed.run();
    }

    private static Set<BlockPos> immutablePositions(Collection<BlockPos> source) {
        Objects.requireNonNull(source, "positions");
        Set<BlockPos> copy = new HashSet<>();
        for (BlockPos pos : source) copy.add(Objects.requireNonNull(pos, "position").immutable());
        return Set.copyOf(copy);
    }
}
