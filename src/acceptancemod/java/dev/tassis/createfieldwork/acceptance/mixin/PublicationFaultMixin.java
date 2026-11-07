package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import dev.tassis.createfieldwork.isolation.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.SourcePublications;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SourcePublications.class, remap = false)
public abstract class PublicationFaultMixin {
    @Inject(method = "reclassify(Ldev/tassis/createfieldwork/isolation/TransitionCoordinator$View;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$fail(TransitionCoordinator.View view, CallbackInfo ci) {
        if (view != null && AcceptanceFaults.throwPublication) {
            AcceptanceFaults.throwPublication = false;
            AcceptanceFaults.publicationReached = true;
            throw new IllegalStateException("acceptance final publication failure");
        }
    }
}
