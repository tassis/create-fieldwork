package dev.tassis.createfieldwork.isolator.smart;

import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import dev.tassis.createfieldwork.platform.IsolatorIntegrationBridge;
import dev.tassis.createfieldwork.isolator.binding.BindingRevocations;

public final class BrassIsolatorBlockEntity extends BlockEntity {
    private UUID instance;
    private final Map<UUID, Long> grants = new HashMap<>();
    public BrassIsolatorBlockEntity(BlockPos pos, BlockState state) { super(FieldworkContent.BRASS_ISOLATOR_BE.get(), pos, state); }
    @Override public void onLoad() {
        super.onLoad();
        IsolatorIntegrationBridge.smartLoaded(this);
    }
    @Override public void setRemoved() {
        IsolatorIntegrationBridge.smartRemoved(this);
        super.setRemoved();
    }

    public UUID instanceStamp() { return instance; }
    public Map<UUID, Long> grants() { return Map.copyOf(grants); }
    public void forEachGrant(java.util.function.BiConsumer<UUID, Long> consumer) { grants.forEach(consumer); }
    public long grantGeneration(UUID id) { return grants.getOrDefault(id, 0L); }
    public void pruneRevoked(BindingRevocations revocations) {
        if (grants.keySet().removeIf(revocations::revoked)) setChanged();
    }
    public boolean accepts(CraftKey craft, UUID existingDevice) {
        return !craft.id().equals(instance) || grants.size() < 4096 || existingDevice != null && grants.containsKey(existingDevice);
    }
    public CraftKey currentCraft() {
        return IsolatorIntegrationBridge.currentCraft(this);
    }
    public void grant(CraftKey craft, UUID device, long generation) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || !craft.equals(currentCraft()) || generation <= 0)
            throw new IllegalStateException("Invalid smart isolator grant context");
        if (!craft.id().equals(instance)) { grants.clear(); instance = craft.id(); }
        if (grants.size() >= 4096 && !grants.containsKey(device)) throw new IllegalStateException("Smart isolator grant limit reached");
        grants.put(device, generation); setChanged();
        IsolatorIntegrationBridge.smartLoaded(this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FieldworkGrantSchema", 1);
        if (instance != null) tag.putUUID("FieldworkInstance", instance);
        var entries = new ListTag();
        grants.forEach((id, generation) -> { var entry = new CompoundTag(); entry.putUUID("Device", id); entry.putLong("Generation", generation); entries.add(entry); });
        tag.put("FieldworkGrants", entries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadGrants(tag);
        if (level instanceof ServerLevel) IsolatorIntegrationBridge.smartLoaded(this);
    }
    private void loadGrants(CompoundTag tag) {
        grants.clear(); instance = null;
        if (tag.getInt("FieldworkGrantSchema") != 1 || !tag.hasUUID("FieldworkInstance")) return;
        var entries = tag.getList("FieldworkGrants", Tag.TAG_COMPOUND);
        if (entries.size() > 4096) return;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            if (!entry.hasUUID("Device") || !entry.contains("Generation", Tag.TAG_LONG) || entry.getLong("Generation") <= 0 ||
                grants.putIfAbsent(entry.getUUID("Device"), entry.getLong("Generation")) != null) { grants.clear(); return; }
        }
        instance = tag.getUUID("FieldworkInstance");
    }
}
