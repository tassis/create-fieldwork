package dev.tassis.createfieldwork.acceptance.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Throws only after native removal has completed, so acceptance never strands a Sable physics body. */
@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class SubLevelRemovalFaultMixin {
    @Inject(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V",
        at = @At("RETURN"), require = 1, allow = 1)
    private void acceptance$afterRemoval(int x, int z, SubLevelRemovalReason reason, CallbackInfo ci) {
        if (AcceptanceFaults.throwSubLevelRemovalAfter) {
            AcceptanceFaults.throwSubLevelRemovalAfter = false;
            throw new IllegalStateException("acceptance allocation rollback failure");
        }
    }
}
