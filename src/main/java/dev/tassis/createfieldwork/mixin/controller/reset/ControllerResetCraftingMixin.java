package dev.tassis.createfieldwork.mixin.controller.reset;

import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Actual extraction, not matching/preview. Pending results stay blocked on a
 * failure while vanilla consumes the ingredient normally, including quick-move. */
@Mixin(ResultSlot.class)
public abstract class ControllerResetCraftingMixin {
    @Shadow @Final private Player player;
    @Inject(method = "checkTakeAchievements", at = @At("HEAD"), require = 1, allow = 1)
    private void fieldwork$reset(ItemStack result, CallbackInfo ci) {
        if (player instanceof ServerPlayer actor && ControllerReset.pending(result)) ControllerReset.completeCrafted(actor, result);
    }
}
