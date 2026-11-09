package dev.tassis.createfieldwork.isolator.binding;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent veto only. Positive numbers are issued generations, negative revoked.
 * Never stores destinations or revives an old ID. Unknown/malformed state denies.
 */
public final class BindingRevocations extends SavedData {
    public static final Factory<BindingRevocations> FACTORY = new Factory<>(BindingRevocations::new, BindingRevocations::load, null);
    private final Map<UUID, Long> generations = new HashMap<>();
    private boolean healthy = true;
    private dev.tassis.createfieldwork.platform.FieldworkServerState owner;
    public void attach(dev.tassis.createfieldwork.platform.FieldworkServerState state) {
        state.checkThread();
        if (owner != null && owner != state) throw new IllegalStateException("Binding ledger cannot cross server sessions");
        owner = state;
    }
    private void checkOwner() { if (owner != null) owner.checkThread(); }
    public boolean healthy() { checkOwner(); return healthy; }
    public long generation(UUID id) { checkOwner(); return healthy ? generations.getOrDefault(id, 0L) : 0; }
    public boolean permits(UUID id, long generation) { checkOwner(); return generation > 0 && generation(id) == generation; }
    public boolean revoked(UUID id) { checkOwner(); return healthy && generations.getOrDefault(id, 0L) < 0; }

    public UUID issue() {
        requireHealthy();
        UUID id;
        do { id = UUID.randomUUID(); } while (generations.containsKey(id));
        generations.put(id, 1L); setDirty();
        return id;
    }
    public void revoke(UUID id) {
        requireHealthy();
        long previous = generations.getOrDefault(id, 0L);
        if (previous < 0) return;
        generations.put(id, previous == Long.MAX_VALUE ? Long.MIN_VALUE : -(previous + 1));
        setDirty();
    }
    private void requireHealthy() { checkOwner(); if (!healthy) throw new IllegalStateException("Binding revocation data is invalid; managed devices remain disabled"); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        checkOwner();
        tag.putInt("Schema", 1); tag.putBoolean("Invalid", !healthy);
        var entries = new ListTag();
        generations.forEach((id, generation) -> { var entry = new CompoundTag(); entry.putUUID("Id", id); entry.putLong("Generation", generation); entries.add(entry); });
        tag.put("Entries", entries);
        return tag;
    }
    public static BindingRevocations load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new BindingRevocations();
        if (tag.getInt("Schema") != 1 || tag.getBoolean("Invalid") || !tag.contains("Entries", Tag.TAG_LIST)) {
            result.healthy = false; return result;
        }
        var entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            if (!entry.hasUUID("Id") || !entry.contains("Generation", Tag.TAG_LONG) || entry.getLong("Generation") == 0 ||
                result.generations.putIfAbsent(entry.getUUID("Id"), entry.getLong("Generation")) != null) { result.healthy = false; break; }
        }
        return result;
    }
}
