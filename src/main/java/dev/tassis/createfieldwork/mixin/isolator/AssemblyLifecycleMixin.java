package dev.tassis.createfieldwork.mixin.isolator;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;

@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class AssemblyLifecycleMixin {
    @WrapMethod(method = "assembleBlocks")
    private static ServerSubLevel fieldwork$assemble(ServerLevel level, BlockPos anchor, Iterable<BlockPos> positions,
                                                     BoundingBox3ic bounds, Operation<ServerSubLevel> upstream) {
        var copy = TransitionCoordinator.copy(positions);
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        coordinator.begin("assemble", level, copy);
        boolean success = false;
        try {
            coordinator.mutating();
            var craft = upstream.call(level, anchor, copy, bounds);
            success = craft != null;
            return craft;
        } finally { coordinator.end("assemble", success); }
    }

    @WrapMethod(method = "moveBlocks")
    private static void fieldwork$move(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform,
                                       Iterable<BlockPos> positions, Operation<Void> upstream) {
        var copy = TransitionCoordinator.copy(positions);
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        coordinator.begin("move", level, copy);
        boolean success = false;
        try {
            var destinations = new ArrayList<BlockPos>();
            for (var from : copy) {
                var to = transform.apply(from);
                destinations.add(to);
                coordinator.markerMove(level, from, transform.getLevel(), to);
            }
            coordinator.destinations(transform.getLevel(), destinations);
            coordinator.mutating();
            upstream.call(level, transform, copy);
            success = true;
        } finally { coordinator.end("move", success); }
    }
}
