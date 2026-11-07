package dev.tassis.createfieldwork.isolation;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.RefreshScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/** Server-owned affected-only barriers. Failure retains this transaction, not other crafts' commits. */
public final class TransitionCoordinator {
    public record Position(ResourceKey<Level> dimension, BlockPos pos) {
        public Position { pos = pos.immutable(); }
    }
    public record View(long token, Map<CraftKey, Set<BlockPos>> candidates) {
        public View {
            Map<CraftKey, Set<BlockPos>> copy = new HashMap<>();
            candidates.forEach((key, value) -> copy.put(key, Set.copyOf(value)));
            candidates = Map.copyOf(copy);
        }
    }
    private record MarkerMove(Position from, Position to) {}
    private record Frame(String kind, Transaction transaction, boolean root) {}
    private static final class Transaction {
        final long id;
        final Set<Position> positions = new HashSet<>();
        final Set<CraftKey> crafts = new HashSet<>();
        final List<MarkerMove> ledger = new ArrayList<>();
        final Map<Position, Boolean> markerIntent = new HashMap<>();
        final Map<CraftKey, Set<BlockPos>> before = new HashMap<>();
        final Set<ResourceKey<Level>> levels = new HashSet<>();
        RefreshScope refreshScope = RefreshScope.none();
        boolean mutated;
        boolean unknownSources;
        boolean failed;
        Transaction(long id) { this.id = id; }
    }

    private final FieldworkServerState state;
    private final Deque<Frame> stack = new ArrayDeque<>();
    private final Map<Position, Set<Long>> positions = new HashMap<>();
    private final Map<CraftKey, Set<Long>> crafts = new HashMap<>();
    private final Map<Long, Transaction> withheld = new LinkedHashMap<>();
    private long nextToken;

    public TransitionCoordinator(FieldworkServerState state) { this.state = state; }
    public boolean active() { state.checkThread(); return !stack.isEmpty(); }
    public Set<Long> failedTokens() { state.checkThread(); return Set.copyOf(withheld.keySet()); }

    public boolean blocked(ServerLevel level, BlockPos pos, View view) {
        state.checkThread();
        return blocked(positions.get(new Position(level.dimension(), pos)), view);
    }

    public boolean blocked(CraftKey key, View view) { state.checkThread(); return blocked(crafts.get(key), view); }

    private boolean blocked(Set<Long> holders, View view) {
        return holders != null && holders.stream().anyMatch(id -> view == null || id != view.token);
    }

    public void begin(String kind, ServerLevel level, Iterable<BlockPos> input) {
        state.checkThread();
        var copy = copy(input);
        boolean join = !stack.isEmpty() && (kind.equals("move") || kind.equals("marker")
            || (kind.equals("assemble") && stack.peek().kind.equals("split")
                && copy.stream().anyMatch(pos -> stack.peek().transaction.positions.contains(new Position(level.dimension(), pos)))));
        var tx = join ? stack.peek().transaction : new Transaction(++nextToken);
        tx.unknownSources |= kind.equals("load");
        capture(tx, level, copy); // BEFORE touch/registry.begin and any upstream callback/write
        for (var pos : copy) position(tx, level, pos);
        state.invalidateVisibility();
        if (!join) {
            try { state.refresh().refresh(null, tx.refreshScope); }
            catch (RuntimeException | Error error) { fail(tx, error); throw error; }
        }
        stack.push(new Frame(kind, tx, !join));
    }

    public void expectLoad(ServerLevel level, UUID id) {
        state.checkThread();
        var key = new CraftKey(level.dimension(), id);
        if (SubLevelContainer.getContainer(level).getSubLevel(id) != null) return;
        // Replace a failed, allocation-free load's barrier atomically with this
        // new load attempt. Never release a token holding partial world writes.
        for (var old : List.copyOf(withheld.values())) {
            if (old.unknownSources && old.positions.isEmpty() && old.crafts.equals(Set.of(key))) release(old);
        }
        touch(stack.peek().transaction, key);
        state.invalidateVisibility();
    }
    public void mutating() { state.checkThread(); stack.peek().transaction.mutated = true; }
    public void expectMarker(ServerLevel level, BlockPos pos, boolean present) {
        state.checkThread(); stack.peek().transaction.markerIntent.put(new Position(level.dimension(), pos), present);
    }
    public boolean unknownPublicationPending() {
        state.checkThread();
        return !stack.isEmpty() || withheld.values().stream().anyMatch(tx -> tx.unknownSources);
    }

    public void allocated(SubLevel craft) {
        state.checkThread();
        CraftKey key = IsolationRuntime.key(craft);
        state.registry().begin(key);
        if (!stack.isEmpty()) touch(stack.peek().transaction, key);
    }

    public void destinations(ServerLevel level, Iterable<BlockPos> input) {
        var copy = copy(input);
        var tx = stack.peek().transaction;
        capture(tx, level, copy);
        for (var pos : copy) position(tx, level, pos);
        state.invalidateVisibility(); // same-root expansion must invalidate decisive train cache too
        state.refresh().refresh(null, tx.refreshScope);
    }

    public void markerMove(ServerLevel source, BlockPos from, ServerLevel destination, BlockPos to) {
        if (source.getBlockState(from).is(FieldworkContent.ISOLATOR.get())) {
            stack.peek().transaction.ledger.add(new MarkerMove(new Position(source.dimension(), from), new Position(destination.dimension(), to)));
            expectMarker(source, from, false);
            expectMarker(destination, to, true);
        }
    }

    public void end(String kind, boolean success) {
        state.checkThread();
        var frame = stack.pop();
        if (!frame.kind.equals(kind)) throw new IllegalStateException("Unbalanced Fieldwork transaction");
        var tx = frame.transaction;
        tx.failed |= !success;
        if (!frame.root) return;
        if (tx.positions.isEmpty() && tx.crafts.isEmpty()) { state.invalidateVisibility(); return; }
        Set<Position> visuals;
        try {
            if (tx.failed || !validLedger(tx)) { fail(tx, null); return; }
            visuals = publish(tx);
        } catch (RuntimeException | Error error) { fail(tx, error); throw error; }
        synchronizeVisuals(visuals); // never feed post-commit visual errors back into fail(tx)
    }

    private void position(Transaction tx, ServerLevel level, BlockPos pos) {
        Position key = new Position(level.dimension(), pos);
        Set<Long> holders = positions.computeIfAbsent(key, ignored -> new HashSet<>());
        if (holders.stream().anyMatch(id -> id != tx.id)) tx.failed = true;
        holders.add(tx.id); tx.positions.add(key);
        var owner = Sable.HELPER.getContaining(level, pos);
        if (owner != null) touch(tx, IsolationRuntime.key(owner));
    }

    private void capture(Transaction tx, ServerLevel level, Collection<BlockPos> input) {
        tx.levels.add(level.dimension());
        Set<CraftKey> affectedCrafts = new HashSet<>(tx.crafts);
        Set<Position> affectedPositions = new HashSet<>(tx.positions);
        for (var pos : input) {
            affectedPositions.add(new Position(level.dimension(), pos));
            var owner = Sable.HELPER.getContaining(level, pos);
            if (owner != null) affectedCrafts.add(IsolationRuntime.key(owner));
        }
        tx.refreshScope = tx.refreshScope.union(state.refresh().affected(tx.levels, affectedCrafts, affectedPositions));
        if (tx.unknownSources) for (var dimension : tx.levels)
            tx.refreshScope = tx.refreshScope.union(RefreshScope.level(dimension));
    }

    private void touch(Transaction tx, CraftKey key) {
        tx.levels.add(key.dimension());
        var holders = crafts.computeIfAbsent(key, ignored -> new HashSet<>());
        if (holders.stream().anyMatch(id -> id != tx.id)) tx.failed = true;
        holders.add(tx.id);
        if (tx.crafts.add(key)) {
            var previous = state.registry().snapshot(key);
            if (previous.status() == IsolationRegistry.Status.READY) {
                tx.before.put(key, previous.positions());
                for (var pos : previous.positions()) tx.markerIntent.putIfAbsent(new Position(key.dimension(), pos), true);
            }
        }
        state.registry().begin(key);
    }

    private boolean validLedger(Transaction tx) {
        for (var intent : tx.markerIntent.entrySet()) {
            var level = state.server().getLevel(intent.getKey().dimension);
            if (level == null || level.getBlockState(intent.getKey().pos).is(FieldworkContent.ISOLATOR.get()) != intent.getValue()) return false;
        }
        for (var move : tx.ledger) {
            ServerLevel destination = state.server().getLevel(move.to.dimension);
            ServerLevel source = state.server().getLevel(move.from.dimension);
            if (destination == null || source == null || !destination.getBlockState(move.to.pos).is(FieldworkContent.ISOLATOR.get())
                || (!move.from.equals(move.to) && source.getBlockState(move.from.pos).is(FieldworkContent.ISOLATOR.get()))) return false;
        }
        return true;
    }

    /** Named operator repair: verify original intent, save actual blocks, then retry only this token. */
    public boolean repair(long token) {
        state.checkThread();
        var tx = withheld.get(token);
        if (tx == null || active() || !validLedger(tx)) return false;
        if (tx.mutated && tx.markerIntent.isEmpty()) return false;
        if (!tx.mutated) {
            if (!tx.before.keySet().containsAll(tx.crafts)) return false;
            for (var entry : tx.before.entrySet()) {
                var level = state.server().getLevel(entry.getKey().dimension());
                var craft = level == null ? null : SubLevelContainer.getContainer(level).getSubLevel(entry.getKey().id());
                if (!(craft instanceof ServerSubLevel loaded) || !IsolationRuntime.markers(loaded).equals(entry.getValue())) return false;
            }
        }
        Set<Position> visuals;
        try {
            state.server().saveEverything(true, true, true);
            tx.failed = false;
            visuals = publish(tx);
        } catch (RuntimeException | Error error) { fail(tx, error); return false; }
        synchronizeVisuals(visuals);
        return true;
    }

    private Set<Position> publish(Transaction tx) {
        var candidate = new HashMap<CraftKey, Set<BlockPos>>();
        for (var key : tx.crafts) {
            if (blocked(key, new View(tx.id, Map.of()))) throw new IllegalStateException("Overlapping transaction remains blocked");
            var level = state.server().getLevel(key.dimension());
            var craft = level == null ? null : SubLevelContainer.getContainer(level).getSubLevel(key.id());
            if (craft instanceof ServerSubLevel loaded && !craft.isRemoved()) {
                for (var chunk : loaded.getPlot().getLoadedChunks()) state.publications().trackChunk(level, chunk.getChunk());
                candidate.put(key, IsolationRuntime.markers(loaded));
            }
        }
        Set<Position> mutableUpdates = new HashSet<>(tx.positions);
        candidate.forEach((key, markers) -> markers.forEach(pos -> mutableUpdates.add(new Position(key.dimension(), pos))));
        Set<Position> updates = Set.copyOf(mutableUpdates);
        var view = new View(tx.id, candidate);
        // New endpoints/keys may appear after move/load or old keys disappear;
        // retain the pre-write union and add the post-reconstruction footprint.
        tx.refreshScope = tx.refreshScope.union(state.refresh().affected(tx.levels, tx.crafts, tx.positions));
        if (tx.unknownSources) for (var dimension : tx.levels)
            tx.refreshScope = tx.refreshScope.union(RefreshScope.level(dimension));
        long epoch = state.visibilityEpoch();
        state.refresh().refresh(view, tx.refreshScope);
        if (state.visibilityEpoch() != epoch) throw new IllegalStateException("Authoritative state changed during final refresh");
        for (var key : tx.crafts) {
            if (candidate.containsKey(key)) state.registry().publish(key, candidate.get(key));
            else state.registry().forget(key);
        }
        // This is the last throwable authoritative stage. Prepare the final
        // publication classification while all position/craft barriers remain
        // indexed; a failure can still safely retain the complete quarantine.
        state.publications().reclassify(view);
        state.publicationCommitted();
        release(tx);
        return updates;
    }

    private void synchronizeVisuals(Set<Position> updates) {
        for (var pos : updates) {
            var level = state.server().getLevel(pos.dimension);
            if (level != null) state.visuals().update(level, pos.pos);
        }
    }

    private void fail(Transaction tx, Throwable error) {
        tx.failed = true;
        withheld.put(tx.id, tx);
        for (var key : tx.crafts) state.registry().fail(key);
        state.invalidateVisibility();
        LogUtils.getLogger().error("Fieldwork withheld transaction {} ({} crafts / {} positions); output withdrawal may be incomplete",
            tx.id, tx.crafts.size(), tx.positions.size(), error);
    }

    private void release(Transaction tx) {
        for (var pos : tx.positions) remove(positions, pos, tx.id);
        for (var key : tx.crafts) remove(crafts, key, tx.id);
        withheld.remove(tx.id);
    }

    private static <K> void remove(Map<K, Set<Long>> index, K key, long token) {
        var holders = index.get(key);
        if (holders != null) { holders.remove(token); if (holders.isEmpty()) index.remove(key); }
    }

    public void clear() { state.checkThread(); positions.clear(); crafts.clear(); stack.clear(); withheld.clear(); }

    public static List<BlockPos> copy(Iterable<BlockPos> input) {
        var result = new ArrayList<BlockPos>();
        for (var pos : input) result.add(pos.immutable());
        return result;
    }
}
