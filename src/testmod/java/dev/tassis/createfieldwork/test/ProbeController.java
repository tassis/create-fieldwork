package dev.tassis.createfieldwork.test;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/** Test-only scoped server-call context; production must own this per server. */
public final class ProbeController {
    public static boolean enabled;
    public static Source current;
    public static Runnable refresh;
    public static int reconciliations;
    public static boolean naturalTrackingObserved;

    // A strong player reference is convenient for FakePlayer tests only. A
    // production endpoint must store server-owned identity, not retain players.
    public record Source(ServerPlayer player, BlockPos lectern) {
        public SubLevel owner(Level level) {
            if (lectern != null) return Sable.HELPER.getContaining(level, lectern);
            var tracking = Sable.HELPER.getTrackingSubLevel(player);
            return tracking != null && !tracking.isRemoved() && tracking.getLevel() == level ? tracking : null;
        }
        public boolean valid(Level level) {
            if (player.isRemoved() || player.isDeadOrDying() || player.level() != level) return false;
            if (ProbeMovement.guardControllerTransitions && (player.connection == null || !player.connection.isAcceptingMessages())) return false;
            var tracking = Sable.HELPER.getTrackingSubLevel(player);
            return lectern != null || tracking == null || (!tracking.isRemoved() && tracking.getLevel() == level);
        }
    }

    public static boolean unavailable(Level level, IRedstoneLinkable endpoint) {
        if (!enabled || !(endpoint instanceof ControllerEndpoint tagged) || tagged.probe$source() == null) return false;
        return (tagged.probe$restrictedHistory() || ProbeMovement.lastPrivateIsRestricted(tagged.probe$source(), level)
            || ProbeMovement.proposedPrivateIsUnconfirmed(tagged.probe$source(), level))
            && (!tagged.probe$source().valid(level) || ProbeMovement.pending(tagged.probe$source(), level));
    }

    public static SubLevel owner(Level level, IRedstoneLinkable endpoint) {
        if (enabled && endpoint instanceof ControllerEndpoint tagged && tagged.probe$source() != null) {
            var owner = tagged.probe$source().owner(level);
            if (tagged.probe$source().valid(level) && !ProbeMovement.pending(tagged.probe$source(), level)) {
                tagged.probe$restricted(owner != null && ProbeScope.isPrivate(owner));
                if (tagged.probe$source().lectern() == null) ProbeMovement.remember(tagged.probe$source().player(), owner);
            }
            return owner;
        }
        return Sable.HELPER.getContaining(level, endpoint.getLocation());
    }

    public static String key(Level level, IRedstoneLinkable endpoint) {
        if (unavailable(level, endpoint)) return "UNAVAILABLE";
        var owner = owner(level, endpoint);
        return owner != null && ProbeScope.isPrivate(owner) ? level.dimension().location() + "/" + owner.getUniqueId() : "WORLD";
    }

    public static void reconcile(Level level) {
        if (!enabled) return;
        boolean changed = false;
        for (Object collection : new ArrayList<>(LinkedControllerServerHandler.receivedInputs.get(level).values())) {
            for (Object object : (Collection<?>) collection) {
                IRedstoneLinkable endpoint = (IRedstoneLinkable) object;
                if (!(endpoint instanceof ControllerEndpoint tagged) || tagged.probe$source() == null) continue;
                String next = key(level, endpoint);
                if (!Objects.equals(next, tagged.probe$publishedScope())) {
                    tagged.probe$publishedScope(next);
                    changed = true;
                }
            }
        }
        if (changed) {
            reconciliations++;
            if (refresh != null) refresh.run();
        }
    }
    private ProbeController() {}
}
