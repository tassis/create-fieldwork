package dev.tassis.createfieldwork.test;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Disposable test oracle, not a production registry. Reset between server runs. */
public final class ProbeScope {
    public static boolean enabled;
    public static long networkCalls;
    public static long simulatedCalls;
    private static final Map<UUID, Boolean> PRIVATE = new HashMap<>();

    public static void reset() {
        PRIVATE.clear();
        enabled = false;
        networkCalls = 0;
        simulatedCalls = 0;
    }

    public static void ready(SubLevel craft, boolean isolated) {
        Boolean previous = PRIVATE.put(craft.getUniqueId(), isolated);
        if (!java.util.Objects.equals(previous, isolated)) ProbeTrain.invalidate();
    }

    public static void initializing(SubLevel craft) { initializing(craft.getUniqueId()); }
    public static void initializing(UUID id) { if (PRIVATE.remove(id) != null) ProbeTrain.invalidate(); }
    public static boolean isPrivate(SubLevel craft) { return ProbeLifecycle.craftPending(craft) || PRIVATE.getOrDefault(craft.getUniqueId(), true); }
    public static boolean isReady(SubLevel craft) { return !ProbeLifecycle.craftPending(craft) && PRIVATE.containsKey(craft.getUniqueId()); }

    public static boolean allows(Level level, BlockPos a, BlockPos b) {
        if (!enabled) return true;
        if (ProbeLifecycle.quarantined(level, a) || ProbeLifecycle.quarantined(level, b)) return false;
        SubLevel first = Sable.HELPER.getContaining(level, a);
        SubLevel second = Sable.HELPER.getContaining(level, b);
        boolean privateA = first != null && isPrivate(first);
        boolean privateB = second != null && isPrivate(second);
        return (!privateA && !privateB) || (privateA && privateB && first.getUniqueId().equals(second.getUniqueId()));
    }

    public static boolean allows(Level level, IRedstoneLinkable a, IRedstoneLinkable b) {
        if (!enabled) return true;
        if (ProbeController.unavailable(level, a) || ProbeController.unavailable(level, b)) return false;
        if (ProbeLifecycle.quarantined(level, a.getLocation()) || ProbeLifecycle.quarantined(level, b.getLocation())) return false;
        return pair(ProbeController.owner(level, a), ProbeController.owner(level, b));
    }

    public static boolean allows(Level level, IRedstoneLinkable sender, BlockPos receiver) {
        if (!enabled) return true;
        if (ProbeController.unavailable(level, sender)) return false;
        if (ProbeLifecycle.quarantined(level, sender.getLocation()) || ProbeLifecycle.quarantined(level, receiver)) return false;
        return pair(ProbeController.owner(level, sender), Sable.HELPER.getContaining(level, receiver));
    }

    private static boolean pair(SubLevel a, SubLevel b) {
        boolean pa = a != null && isPrivate(a), pb = b != null && isPrivate(b);
        return (!pa && !pb) || (pa && pb && a.getUniqueId().equals(b.getUniqueId()));
    }

    private ProbeScope() {}
}
