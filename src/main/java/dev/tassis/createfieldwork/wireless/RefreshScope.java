package dev.tassis.createfieldwork.wireless;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import dev.tassis.createfieldwork.isolation.CraftKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Immutable cold-refresh selection. An unresolved frequency widens one dimension, not the server. */
public record RefreshScope(Map<ResourceKey<Level>, Selection> levels) {
    private static final RefreshScope NONE = new RefreshScope(Map.of());
    public record Selection(boolean all, Set<Couple<Frequency>> frequencies,
                            Map<Couple<Frequency>, Set<CraftKey>> receiverFilters) {
        public Selection(boolean all, Set<Couple<Frequency>> frequencies) {
            this(all, frequencies, Map.of());
        }
        public Selection {
            Objects.requireNonNull(frequencies, "frequencies");
            Objects.requireNonNull(receiverFilters, "receiverFilters");
            Set<Couple<Frequency>> copy = new HashSet<>();
            if (!all) for (var key : frequencies) copy.add(Couple.create(key.getFirst(), key.getSecond()));
            frequencies = Set.copyOf(copy);
            Map<Couple<Frequency>, Set<CraftKey>> filters = new HashMap<>();
            if (!all) for (var entry : receiverFilters.entrySet()) {
                if (!frequencies.contains(entry.getKey())) throw new IllegalArgumentException("Receiver filter has no selected frequency");
                var key = entry.getKey();
                filters.put(Couple.create(key.getFirst(), key.getSecond()), Set.copyOf(entry.getValue()));
            }
            receiverFilters = Map.copyOf(filters);
        }
        public boolean includes(Couple<Frequency> key) { return all || frequencies.contains(key); }
        public boolean filtered(Couple<Frequency> key) { return receiverFilters.containsKey(key); }
        /** Selection only, never authorization. Unrestricted work includes PENDING receivers for withdrawal. */
        public boolean accepts(Couple<Frequency> key, Scope receiver) {
            if (!includes(key)) return false;
            var crafts = receiverFilters.get(key);
            return crafts == null || receiver instanceof Scope.Private privateScope && crafts.contains(privateScope.craft());
        }
        public boolean unrestrictedUnknown() {
            return all || frequencies.stream().anyMatch(key -> !receiverFilters.containsKey(key));
        }
        public boolean acceptsUnknown(Scope receiver) {
            return unrestrictedUnknown() || receiver instanceof Scope.Private privateScope
                && receiverFilters.values().stream().anyMatch(crafts -> crafts.contains(privateScope.craft()));
        }
    }
    public RefreshScope { levels = Map.copyOf(levels); }
    public static RefreshScope none() { return NONE; }
    public static RefreshScope level(ResourceKey<Level> dimension) {
        return new RefreshScope(Map.of(dimension, new Selection(true, Set.of())));
    }
    public static RefreshScope frequency(ResourceKey<Level> dimension, Couple<Frequency> key) {
        return new RefreshScope(Map.of(dimension, new Selection(false, Set.of(key))));
    }
    public static RefreshScope privateReceivers(ResourceKey<Level> dimension, Couple<Frequency> key, CraftKey craft) {
        if (!dimension.equals(craft.dimension())) throw new IllegalArgumentException("Receiver craft belongs to another dimension");
        return new RefreshScope(Map.of(dimension, new Selection(false, Set.of(key), Map.of(key, Set.of(craft)))));
    }
    public boolean isEmpty() { return levels.isEmpty(); }
    public boolean isFrequency(ResourceKey<Level> dimension, Couple<Frequency> key) {
        var selection = levels.get(dimension);
        return levels.size() == 1 && selection != null && !selection.all
            && selection.frequencies.size() == 1 && selection.includes(key);
    }
    public RefreshScope union(RefreshScope other) {
        if (other.isEmpty()) return this;
        if (isEmpty()) return other;
        var merged = new HashMap<>(levels);
        other.levels.forEach((dimension, selection) -> merged.merge(dimension, selection, (a, b) -> {
            if (a.all || b.all) return new Selection(true, Set.of());
            var keys = new HashSet<>(a.frequencies); keys.addAll(b.frequencies);
            Map<Couple<Frequency>, Set<CraftKey>> filters = new HashMap<>();
            for (var key : keys) {
                boolean inA = a.frequencies.contains(key), inB = b.frequencies.contains(key);
                // Missing in an operand is not the same as selected/unrestricted.
                if ((inA && !a.filtered(key)) || (inB && !b.filtered(key))) continue;
                var crafts = new HashSet<CraftKey>();
                if (inA) crafts.addAll(a.receiverFilters.get(key));
                if (inB) crafts.addAll(b.receiverFilters.get(key));
                filters.put(key, crafts);
            }
            return new Selection(false, keys, filters);
        }));
        return new RefreshScope(merged);
    }
}
