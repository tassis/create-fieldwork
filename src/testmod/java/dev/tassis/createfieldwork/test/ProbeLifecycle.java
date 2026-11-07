package dev.tassis.createfieldwork.test;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/** Executable candidate transaction model. Test-only, deliberately not production. */
public final class ProbeLifecycle {
    public static boolean enabled;
    public static boolean useQuarantine = true;
    public static int depth;
    public static int commits;
    public static Runnable observation;
    public static Runnable afterCommit;
    public static String failAtCheckpoint;
    public static int packedNbtInputs;
    public static int packedPositionsObserved;
    public static int initializingChunkChecks;
    public static boolean skipMarkerWriteOnce;
    public static boolean skipMarkerSourceClearOnce;
    public static boolean checkMarkerTransfers = true;
    public static int withheldCommits;
    public static final class InjectedFailure extends RuntimeException {
        public InjectedFailure(String stage) { super("intentional lifecycle failure at " + stage); }
    }
    public static final List<Map<String, Object>> events = new ArrayList<>();
    public static final List<String> violations = new ArrayList<>();
    private static final Map<Key, Set<Long>> moving = new HashMap<>();
    private static final Map<CraftKey, Set<Long>> craftTokens = new HashMap<>();
    private static final Map<Long, Transaction> pending = new LinkedHashMap<>();
    private static final Deque<Frame> stack = new ArrayDeque<>();
    private static long nextToken;
    private record Key(Level level, BlockPos pos) {}
    private record CraftKey(ServerLevel level, UUID id) {}
    private record MarkerMove(ServerLevel source, BlockPos from, ServerLevel destination, BlockPos to) {}
    private record Frame(String kind, Transaction transaction, boolean root) {}
    private static final class Transaction {
        final long id = ++nextToken;
        final String kind;
        final Set<Key> positions = new HashSet<>();
        final Set<CraftKey> crafts = new HashSet<>();
        final List<MarkerMove> ledger = new ArrayList<>();
        boolean failed;
        Transaction(String kind) { this.kind = kind; }
    }

    public static boolean quarantined(Level level, BlockPos pos) {
        return enabled && useQuarantine && moving.containsKey(new Key(level, pos));
    }

    public static boolean craftPending(dev.ryanhcode.sable.sublevel.SubLevel craft) {
        return enabled && craft.getLevel() instanceof ServerLevel level && craftTokens.containsKey(new CraftKey(level, craft.getUniqueId()));
    }

    public static void begin(String kind, ServerLevel level, Iterable<BlockPos> positions) {
        List<BlockPos> inputs = new ArrayList<>();
        for (var pos : positions) inputs.add(pos.immutable());
        boolean join = !stack.isEmpty() && (kind.equals("move") || (kind.equals("assemble")
            && stack.stream().anyMatch(f -> f.kind.equals("split") && f.transaction == stack.peek().transaction)
            && inputs.stream().anyMatch(p -> stack.peek().transaction.positions.contains(new Key(level, p)))));
        Transaction tx = join ? stack.peek().transaction : new Transaction(kind);
        if (!join) ProbeTrain.invalidate();
        stack.push(new Frame(kind, tx, !join));
        depth = stack.size();
        for (var pos : inputs) position(tx, level, pos);
        events.add(Map.of("stage", "begin:" + kind, "depth", depth, "token", tx.id));
    }

    public static void addDestination(ServerLevel level, Iterable<BlockPos> positions) {
        for (var pos : positions) position(stack.peek().transaction, level, pos);
    }

    private static void position(Transaction tx, ServerLevel level, BlockPos pos) {
        Key key = new Key(level, pos.immutable());
        Set<Long> holders = moving.computeIfAbsent(key, k -> new HashSet<>());
        if (holders.stream().anyMatch(id -> id != tx.id)) tx.failed = true;
        holders.add(tx.id);
        tx.positions.add(key);
        var owner = Sable.HELPER.getContaining(level, pos);
        if (owner != null) touch(tx, level, owner.getUniqueId());
    }

    private static void touch(Transaction tx, ServerLevel level, UUID id) {
        CraftKey key = new CraftKey(level, id);
        Set<Long> holders = craftTokens.computeIfAbsent(key, k -> new HashSet<>());
        if (holders.stream().anyMatch(token -> token != tx.id)) tx.failed = true;
        holders.add(tx.id);
        tx.crafts.add(key);
        ProbeScope.initializing(id);
    }

    public static void allocated(dev.ryanhcode.sable.sublevel.SubLevel craft) {
        if (enabled && !stack.isEmpty() && craft.getLevel() instanceof ServerLevel level)
            touch(stack.peek().transaction, level, craft.getUniqueId());
    }

    public static void expectLoad(ServerLevel level, UUID id) {
        if (SubLevelContainer.getContainer(level).getSubLevel(id) == null) touch(stack.peek().transaction, level, id);
    }

    public static void recordMarkerMove(ServerLevel source, BlockPos from, ServerLevel destination, BlockPos to) {
        if (source.getBlockState(from).is(Blocks.OBSIDIAN))
            stack.peek().transaction.ledger.add(new MarkerMove(source, from.immutable(), destination, to.immutable()));
    }

    public static void end(String kind, ServerLevel level, boolean success) {
        Frame frame = stack.pop();
        if (!frame.kind.equals(kind)) throw new AssertionError("Unbalanced lifecycle kind");
        Transaction tx = frame.transaction;
        tx.failed |= !success;
        events.add(Map.of("stage", "end:" + kind, "depth", depth, "success", success, "token", tx.id));
        depth = stack.size();
        if (!frame.root) return;
        if (checkMarkerTransfers && !validLedger(tx)) tx.failed = true;
        if (tx.failed) {
            withheldCommits++;
            if (!tx.positions.isEmpty() || !tx.crafts.isEmpty()) pending.put(tx.id, tx);
            events.add(Map.of("stage", "commit-withheld", "pendingPositions", tx.positions.size(), "token", tx.id));
        } else publish(tx);
    }

    private static boolean validLedger(Transaction tx) {
        for (var move : tx.ledger) {
            boolean identical = move.source == move.destination && move.from.equals(move.to);
            if (!move.destination.getBlockState(move.to).is(Blocks.OBSIDIAN)
                || (!identical && move.source.getBlockState(move.from).is(Blocks.OBSIDIAN))) return false;
        }
        return true;
    }

    private static void release(Transaction tx) {
        for (var key : tx.positions) {
            var holders = moving.get(key);
            if (holders != null) { holders.remove(tx.id); if (holders.isEmpty()) moving.remove(key); }
        }
        for (var key : tx.crafts) {
            var holders = craftTokens.get(key);
            if (holders != null) { holders.remove(tx.id); if (holders.isEmpty()) craftTokens.remove(key); }
        }
        pending.remove(tx.id);
    }

    private static void publish(Transaction tx) {
        release(tx);
        for (var key : tx.crafts) {
            var craft = SubLevelContainer.getContainer(key.level).getSubLevel(key.id);
            if (craft instanceof ServerSubLevel serverCraft && !craftPending(craft)) ProbeScope.ready(craft, !markers(serverCraft).isEmpty());
        }
        commits++;
        if (afterCommit != null) afterCommit.run();
    }

    public static Set<Long> pendingTokens() { return Set.copyOf(pending.keySet()); }
    public static boolean recover(long token) {
        Transaction tx = pending.get(token);
        if (tx == null || tx.ledger.isEmpty() || !validLedger(tx)) return false;
        for (var key : tx.crafts) {
            var holders = craftTokens.get(key);
            if (holders != null && holders.stream().anyMatch(id -> id != token)) return false;
        }
        // Verify repair is saved before removing only this operation's token.
        Set<ServerLevel> levels = new HashSet<>();
        for (var key : tx.crafts) levels.add(key.level);
        for (var level : levels) level.getServer().saveEverything(true, true, true);
        publish(tx);
        events.add(Map.of("stage", "recovered", "token", token));
        return true;
    }

    public static void discardRemovedLoad(ServerLevel level, UUID id) {
        for (var tx : List.copyOf(pending.values())) {
            if (tx.kind.equals("load") && tx.positions.isEmpty() && tx.crafts.size() == 1
                && tx.crafts.contains(new CraftKey(level, id)) && SubLevelContainer.getContainer(level).getSubLevel(id) == null) release(tx);
        }
    }

    public static void checkpoint(String stage) {
        events.add(Map.of("stage", stage, "depth", depth, "quarantinedPositions", moving.size()));
        if (stage.equals(failAtCheckpoint)) throw new InjectedFailure(stage);
        if (observation != null) {
            try { observation.run(); }
            catch (Throwable error) { violations.add(stage + ": " + error); }
        }
    }

    public static Set<BlockPos> markers(ServerSubLevel craft) {
        Set<BlockPos> result = new HashSet<>();
        for (var holder : craft.getPlot().getLoadedChunks()) {
            var chunk = holder.getChunk();
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i];
                if (section.hasOnlyAir()) continue;
                int yBase = chunk.getSectionYFromSectionIndex(i) << 4;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    if (section.getBlockState(x, y, z).is(Blocks.OBSIDIAN)) {
                        result.add(new BlockPos(chunk.getPos().getMinBlockX() + x, yBase + y, chunk.getPos().getMinBlockZ() + z));
                    }
                }
            }
        }
        return result;
    }

    public static List<BlockPos> blocks(ServerSubLevel craft) {
        List<BlockPos> result = new ArrayList<>();
        for (var holder : craft.getPlot().getLoadedChunks()) {
            var box = holder.getBoundingBox();
            if (box == null) continue;
            for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                BlockPos pos = new BlockPos(holder.getPos().getMinBlockX() + x, y, holder.getPos().getMinBlockZ() + z);
                if (!craft.getLevel().getBlockState(pos).isAir()) result.add(pos);
            }
        }
        return result;
    }

    private ProbeLifecycle() {}
}
