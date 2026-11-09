package dev.tassis.createfieldwork.acceptance.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.tassis.createfieldwork.acceptance.ControllerWorkMetrics;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ControllerSources.class, remap = false)
public abstract class ControllerWorkCounterMixin {
    @Inject(method = "reconcileOneView", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$endpoint(ServerLevel level, IRedstoneLinkable endpoint,
                                     TransitionCoordinator.View view,
                                     CallbackInfoReturnable<RefreshScope> cir) {
        ControllerWorkMetrics.recordEndpoint();
    }

    @Inject(method = "safetySweep", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$fullSweep(CallbackInfo ci) {
        ControllerWorkMetrics.recordFullSweep();
    }
}
