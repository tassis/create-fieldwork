package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.BindingFaults;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.refresh.WirelessRefresh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WirelessRefresh.class, remap = false)
public abstract class BindingRefreshFaultMixin {
    @Inject(method = "refresh(Ldev/tassis/createfieldwork/isolator/lifecycle/TransitionCoordinator$View;Ldev/tassis/createfieldwork/wireless/refresh/RefreshScope;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$refresh(TransitionCoordinator.View view, RefreshScope scope, CallbackInfo ci) {
        if (view != null) BindingFaults.hit("final-refresh");
        else BindingFaults.hit("initial-withdrawal");
    }
}
