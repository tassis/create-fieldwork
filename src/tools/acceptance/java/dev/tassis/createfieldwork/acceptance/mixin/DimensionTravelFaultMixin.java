package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.DimensionTravelFaults;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Acceptance-only controls for the production dimension wrapper's cancel/throw finally paths. */
@Mixin(ServerPlayer.class)
public abstract class DimensionTravelFaultMixin {
    @Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void acceptance$travelFault(DimensionTransition destination, CallbackInfoReturnable<Entity> cir) {
        if (DimensionTravelFaults.mode == DimensionTravelFaults.Mode.CANCEL)
            cir.setReturnValue((ServerPlayer) (Object) this);
        else if (DimensionTravelFaults.mode == DimensionTravelFaults.Mode.THROW)
            throw new IllegalStateException("acceptance dimension transfer failure");
    }
}
