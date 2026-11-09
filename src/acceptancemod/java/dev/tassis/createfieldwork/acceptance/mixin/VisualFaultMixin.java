package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IsolationRuntime.class, remap = false)
public abstract class VisualFaultMixin {
    @Inject(method = "visual", at = @At("HEAD"), require = 1, allow = 1)
    private static void acceptance$fail(ServerLevel level, BlockPos pos, boolean active, CallbackInfo ci) {
        if (AcceptanceFaults.throwVisual) {
            AcceptanceFaults.throwVisual = false;
            AcceptanceFaults.visualReached = true;
            throw new IllegalStateException("acceptance visual synchronization failure");
        }
    }
}
