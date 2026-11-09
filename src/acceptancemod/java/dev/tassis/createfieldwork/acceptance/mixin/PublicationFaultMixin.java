package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.publication.SourcePublications;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SourcePublications.class, remap = false)
public abstract class PublicationFaultMixin {
    @Shadow @Final private dev.tassis.createfieldwork.platform.FieldworkServerState state;
    @Inject(method = "reclassify(Ldev/tassis/createfieldwork/isolator/lifecycle/TransitionCoordinator$View;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$fail(TransitionCoordinator.View view, CallbackInfo ci) {
        if (view == null && dev.tassis.createfieldwork.acceptance.BindingFaults.point.equals("pre-frame-publication")) {
            dev.tassis.createfieldwork.acceptance.BindingFaults.preFrameObserved = !state.transitions().active();
            dev.tassis.createfieldwork.acceptance.BindingFaults.hit("pre-frame-publication");
        }
        if (view != null && AcceptanceFaults.throwPublication) {
            AcceptanceFaults.throwPublication = false;
            AcceptanceFaults.publicationReached = true;
            throw new IllegalStateException("acceptance final publication failure");
        }
    }
}
