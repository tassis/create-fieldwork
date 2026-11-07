package dev.tassis.createfieldwork.test.mixin;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.test.ProbeController;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LinkedControllerServerHandler.class, remap = false)
public abstract class ProbeControllerTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 1, allow = 1)
    private static void probe$reconcile(LevelAccessor level, CallbackInfo ci) { ProbeController.reconcile((Level) level); }
}
