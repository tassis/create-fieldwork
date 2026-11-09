package dev.tassis.createfieldwork.bindingprobe;

import dev.tassis.createfieldwork.isolator.state.CraftKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Cold rebuilt candidate model, NOT connected to production scope queries or source refresh.
 * Fixture explicitly calls publish/remove; automatic lifecycle invalidation is not implemented.
 */
final class BindingIndex implements AutoCloseable {
    enum Kind { ORDINARY, BLOCKED, AUTHORIZED }
    record Resolution(Kind kind, CraftKey target) {}
    record Location(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {}
    private record Grant(CraftKey target, Set<UUID> devices) {}
    private final MinecraftServer server;
    private final Map<Location, Grant> records = new HashMap<>();
    private final Map<UUID, Resolution> byDevice = new HashMap<>();
    private boolean closed;

    BindingIndex(MinecraftServer server) { this.server = server; }
    private void requireOwner() {
        if (closed || !server.isSameThread()) throw new IllegalStateException("Closed or foreign-thread experimental index");
    }
    boolean publish(AuthorizationBlockEntity block) {
        requireOwner();
        if (block.getLevel() == null || block.getLevel().getServer() != server)
            throw new IllegalArgumentException("Foreign-server block");
        var location = new Location(block.getLevel().dimension(), block.getBlockPos());
        var target = block.currentCraft();
        boolean valid = target != null && target.id().equals(block.stamp());
        if (valid) records.put(location, new Grant(target, block.devices())); else records.remove(location);
        rebuild();
        return valid;
    }
    void remove(AuthorizationBlockEntity block) {
        requireOwner();
        records.remove(new Location(block.getLevel().dimension(), block.getBlockPos()));
        rebuild();
    }
    private void rebuild() {
        byDevice.clear();
        for (var record : records.values()) for (var device : record.devices()) {
            var proposed = new Resolution(Kind.AUTHORIZED, record.target());
            byDevice.merge(device, proposed, (a, b) -> a.equals(b) ? a : new Resolution(Kind.BLOCKED, null));
        }
    }
    Resolution resolve(ItemStack controller) {
        requireOwner();
        var id = DeviceIdentity.read(controller);
        return id == null ? new Resolution(Kind.ORDINARY, null)
            : byDevice.getOrDefault(id, new Resolution(Kind.BLOCKED, null));
    }
    @Override public void close() { requireOwner(); records.clear(); byDevice.clear(); closed = true; }
}
