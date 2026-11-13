package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.ScribingPrintFaults;
import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;

@Mixin(value = ScribingChecklist.class, remap = false)
public abstract class ScribingPrintFaultMixin {
    @Inject(method = "print", at = @At("HEAD"))
    private void fieldwork$jobFault(ServerLevel level, BlockPos pos, ItemStack blueprint, ItemStack medium,
                                    int mode, boolean replaceEntities, CallbackInfoReturnable<ScribingChecklist.Result> cir) {
        ScribingPrintFaults.fire();
    }

    @Inject(method = "collectAvailable", at = @At("HEAD"))
    private static void fieldwork$nativeWorkFault(ServerLevel level, BlockPos pos, MaterialChecklist checklist, CallbackInfo ci) {
        ScribingPrintFaults.fireDuring();
    }

    @Inject(method = "print", at = @At("RETURN"))
    private void fieldwork$afterGeneration(ServerLevel level, BlockPos pos, ItemStack blueprint, ItemStack medium,
                                           int mode, boolean replaceEntities, CallbackInfoReturnable<ScribingChecklist.Result> cir) {
        ScribingPrintFaults.fireAfter();
    }
}
