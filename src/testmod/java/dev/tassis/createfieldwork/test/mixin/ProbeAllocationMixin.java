package dev.tassis.createfieldwork.test.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class ProbeAllocationMixin {
    @Inject(method = "allocateSubLevel", at = @At("RETURN"), require = 1, allow = 1)
    private void probe$allocated(UUID id, int x, int z, Pose3d pose, CallbackInfoReturnable<SubLevel> cir) {
        if (cir.getReturnValue() != null) ProbeLifecycle.allocated(cir.getReturnValue());
    }
}
