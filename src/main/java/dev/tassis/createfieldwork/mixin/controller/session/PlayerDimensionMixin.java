package dev.tassis.createfieldwork.mixin.controller.session;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;

/** Withdraw old-level controller output around the authoritative dimension transfer call. */
@Mixin(ServerPlayer.class)
public abstract class PlayerDimensionMixin {
    @WrapMethod(method = "changeDimension")
    private Entity fieldwork$dimension(DimensionTransition destination, Operation<Entity> upstream) {
        var actor = (ServerPlayer) (Object) this;
        if (!actor.server.isSameThread()) return upstream.call(destination);
        var state = FieldworkStateAccess.get(actor.server);
        var oldLevel = actor.serverLevel();
        var transition = state.controllers().beginPlayerTransition(actor);
        Entity result = null;
        boolean completed = false;
        try {
            result = upstream.call(destination);
            completed = result instanceof ServerPlayer moved && moved.serverLevel() != oldLevel;
            return result;
        } finally {
            if (completed) state.controllers().completePlayerTransition(transition, (ServerPlayer) result);
            else state.controllers().cancelPlayerTransition(transition);
        }
    }
}
