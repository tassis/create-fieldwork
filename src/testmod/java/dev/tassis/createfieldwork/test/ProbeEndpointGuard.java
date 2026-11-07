package dev.tassis.createfieldwork.test;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Candidate safety guard for direct consumers, enabled only by edge fixtures. */
public final class ProbeEndpointGuard {
    public static boolean enabled;
    public static boolean accept(Level level, IRedstoneLinkable sender, BlockPos receiver) {
        if (!enabled) return true;
        var a = ProbeController.owner(level, sender);
        var b = Sable.HELPER.getContaining(level, receiver);
        boolean restricted = (a != null && ProbeScope.isPrivate(a)) || (b != null && ProbeScope.isPrivate(b))
            || ProbeLifecycle.quarantined(level, sender.getLocation()) || ProbeLifecycle.quarantined(level, receiver);
        return !restricted || sender.isAlive();
    }
    private ProbeEndpointGuard() {}
}
