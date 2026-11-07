package dev.tassis.createfieldwork.wireless;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.controller.ControllerEndpoint;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Constant-time endpoint ownership/registry lookups; never reconstructs a craft. */
public final class ScopeResolver {
    private ScopeResolver() {}

    public static Scope at(ServerLevel level, BlockPos pos, TransitionCoordinator.View view) {
        var state = FieldworkStateAccess.get(level.getServer());
        if (state.transitions().blocked(level, pos, view)) return Scope.PENDING;
        var craft = Sable.HELPER.getContaining(level, pos);
        if (craft == null) return SubLevelContainer.getContainer(level).inBounds(pos) ? Scope.PENDING : Scope.WORLD;
        if (craft.isRemoved()) return Scope.PENDING;
        var key = IsolationRuntime.key(craft);
        return craft(level, key, view);
    }

    public static Scope craft(ServerLevel level, CraftKey key, TransitionCoordinator.View view) {
        var state = FieldworkStateAccess.get(level.getServer());
        if (!key.dimension().equals(level.dimension())) return Scope.PENDING;
        var owner = SubLevelContainer.getContainer(level).getSubLevel(key.id());
        if (owner == null || owner.isRemoved()) return Scope.PENDING;
        if (state.transitions().blocked(key, view)) return Scope.PENDING;
        if (view != null && view.candidates().containsKey(key))
            return view.candidates().get(key).isEmpty() ? Scope.WORLD : Scope.privateCraft(key);
        return state.registry().scope(key);
    }

    public static Scope endpoint(ServerLevel level, IRedstoneLinkable endpoint, TransitionCoordinator.View view) {
        if (endpoint instanceof ControllerEndpoint tagged)
            return FieldworkStateAccess.get(level.getServer()).controllers().scope(level, tagged, view);
        return at(level, endpoint.getLocation(), view);
    }

    public static boolean pair(ServerLevel level, IRedstoneLinkable a, IRedstoneLinkable b, TransitionCoordinator.View view) {
        return PairPolicy.allows(endpoint(level, a, view), endpoint(level, b, view));
    }

    public static boolean direct(ServerLevel level, IRedstoneLinkable source, BlockPos receiver, TransitionCoordinator.View view) {
        Scope from = endpoint(level, source, view), to = at(level, receiver, view);
        boolean restricted = from != Scope.WORLD || to != Scope.WORLD;
        return PairPolicy.allows(from, to) && (!restricted || source.isAlive());
    }
}
