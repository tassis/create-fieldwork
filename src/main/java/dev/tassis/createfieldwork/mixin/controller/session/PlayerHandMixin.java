package dev.tassis.createfieldwork.mixin.controller.session;

import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct hand replacement seam used by swaps, crafting and server-side item replacement. */
@Mixin(LivingEntity.class)
public abstract class PlayerHandMixin {
    @Inject(method = "setItemInHand", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$handChanged(InteractionHand hand, ItemStack stack, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player) || !player.server.isSameThread()) return;
        FieldworkStateAccess.get(player.server).controllers().reconcilePlayer(player.getUUID());
    }
}
