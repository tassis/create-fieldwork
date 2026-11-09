package dev.tassis.createfieldwork.bindingprobe;

import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Experimental block-owned list and UUID stamp. No static/server-global authorization map. */
public final class AuthorizationBlockEntity extends BlockEntity {
    private UUID stamp;
    private final Set<UUID> devices = new LinkedHashSet<>();

    public AuthorizationBlockEntity(BlockPos pos, BlockState state) { super(BindingContent.ENTITY.get(), pos, state); }
    public UUID stamp() { return stamp; }
    public Set<UUID> devices() { return Set.copyOf(devices); }

    public CraftKey currentCraft() {
        if (!(level instanceof ServerLevel server) || isRemoved() || !server.hasChunkAt(worldPosition) ||
                server.getBlockEntity(worldPosition) != this) return null;
        var craft = Sable.HELPER.getContaining(server, worldPosition);
        return craft == null || craft.isRemoved() ? null : IsolationRuntime.key(craft);
    }

    /** Driver calls this server-side; UI/permissions and rebind gestures deliberately absent. */
    public UUID authorize(ItemStack controller) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())
            throw new IllegalStateException("Authorization must run on the owning server thread");
        var key = currentCraft();
        if (key == null || (stamp != null && !stamp.equals(key.id())))
            throw new IllegalStateException("Missing craft or explicit reauthorization required");
        UUID device = DeviceIdentity.read(controller);
        if (device == null) device = UUID.randomUUID();
        DeviceIdentity.write(controller, device);
        stamp = key.id();
        devices.add(device);
        setChanged();
        return device;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (stamp != null) tag.putUUID("InstanceStamp", stamp);
        var list = new ListTag();
        for (var device : devices) {
            var entry = new CompoundTag(); entry.putUUID("Device", device); list.add(entry);
        }
        tag.put("Devices", list);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stamp = tag.hasUUID("InstanceStamp") ? tag.getUUID("InstanceStamp") : null;
        devices.clear();
        var list = tag.getList("Devices", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            if (entry.hasUUID("Device")) devices.add(entry.getUUID("Device"));
        }
    }
}
