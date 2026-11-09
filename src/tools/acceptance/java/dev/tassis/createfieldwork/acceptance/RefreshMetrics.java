package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Test-only operation counts, not a timing benchmark or production diagnostic surface. */
public final class RefreshMetrics {
    private record Key(ResourceKey<Level> dimension, Couple<Frequency> frequency, boolean simulated, BlockPos position) {}
    private static final Map<Key, Integer> calls = new HashMap<>();
    private static boolean enabled;
    private RefreshMetrics() {}
    public static void record(ResourceKey<Level> level, Couple<Frequency> key, boolean simulated, BlockPos position, boolean targeted) {
        if (!enabled || !targeted) return;
        var snapshot = Couple.create(key.getFirst(), key.getSecond());
        calls.merge(new Key(level, snapshot, simulated, position.immutable()), 1, Integer::sum);
    }
    public static void measure(Runnable operation) {
        calls.clear(); enabled = true;
        try { operation.run(); } finally { enabled = false; }
    }
    public static int count(ResourceKey<Level> level, Couple<Frequency> key, boolean simulated) {
        return calls.entrySet().stream().filter(entry -> entry.getKey().dimension.equals(level)
            && entry.getKey().frequency.equals(key) && entry.getKey().simulated == simulated).mapToInt(Map.Entry::getValue).sum();
    }
    public static int count(ResourceKey<Level> level, Couple<Frequency> key, boolean simulated, BlockPos position) {
        return calls.getOrDefault(new Key(level, key, simulated, position), 0);
    }
    public static int total() { return calls.values().stream().mapToInt(Integer::intValue).sum(); }
    public static int total(ResourceKey<Level> dimension) {
        return calls.entrySet().stream().filter(entry -> entry.getKey().dimension.equals(dimension)).mapToInt(Map.Entry::getValue).sum();
    }
}
