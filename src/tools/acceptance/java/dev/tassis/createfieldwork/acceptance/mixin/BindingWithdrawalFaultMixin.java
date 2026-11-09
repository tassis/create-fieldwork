package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.BindingFaults;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TransitionCoordinator.class, remap = false)
public abstract class BindingWithdrawalFaultMixin {
    @Inject(method = "bindingAffected", at = @At("TAIL"), require = 1, allow = 1)
    private void acceptance$withdrawal(ControllerSources.BindingChange change, CallbackInfo ci) { BindingFaults.hit("withdrawal"); }
}
