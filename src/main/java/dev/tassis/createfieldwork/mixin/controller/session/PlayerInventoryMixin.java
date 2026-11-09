package dev.tassis.createfieldwork.mixin.controller.session;

import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact server inventory mutators; player movement is intentionally not controller authority. */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow public Player player;

    @Inject(method = "setItem", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$set(int slot, ItemStack stack, CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$remove(int slot, int count, CallbackInfoReturnable<ItemStack> cir) { fieldwork$changed(); }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$removeStack(ItemStack stack, CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 2, allow = 2)
    private void fieldwork$removeNoUpdate(int slot, CallbackInfoReturnable<ItemStack> cir) { fieldwork$changed(); }

    @Inject(method = "dropAll", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$dropAll(CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "clearContent", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$clear(CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "replaceWith", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$replace(Inventory other, CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "setChanged", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$setChanged(CallbackInfo ci) { fieldwork$changed(); }

    private void fieldwork$changed() {
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.server.isSameThread()) return;
        FieldworkStateAccess.get(serverPlayer.server).controllers().reconcilePlayer(serverPlayer.getUUID());
    }
}
