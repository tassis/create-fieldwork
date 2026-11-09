package dev.tassis.createfieldwork.bindingprobe.mixin;

import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.tassis.createfieldwork.bindingprobe.DeviceIdentity;
import dev.tassis.createfieldwork.bindingprobe.LecternIdentityAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Experimental persistence patch. Loaded only by bindingProbeServer. */
@Mixin(value = LecternControllerBlockEntity.class, remap = false)
public abstract class LecternIdentityMixin implements LecternIdentityAccess {
    @Unique private UUID bindingProbe$device;
    @Unique private boolean bindingProbe$enabled = true;

    @Override public void bindingProbe$patchEnabled(boolean enabled) { bindingProbe$enabled = enabled; }

    @Inject(method = "setController", at = @At("HEAD"), require = 1, allow = 1)
    private void bindingProbe$set(ItemStack stack, CallbackInfo ci) {
        if (bindingProbe$enabled && stack != null) bindingProbe$device = DeviceIdentity.read(stack);
    }

    @Inject(method = "dropController", at = @At("TAIL"), require = 1, allow = 1)
    private void bindingProbe$clearDropped(net.minecraft.world.level.block.state.BlockState state, CallbackInfo ci) {
        if (bindingProbe$enabled) bindingProbe$device = null;
    }

    @Inject(method = "createLinkedController", at = @At("RETURN"), require = 1, allow = 1)
    private void bindingProbe$restore(CallbackInfoReturnable<ItemStack> cir) {
        if (bindingProbe$enabled && bindingProbe$device != null)
            DeviceIdentity.write(cir.getReturnValue(), bindingProbe$device);
    }

    @Inject(method = "write", at = @At("TAIL"), require = 1, allow = 1)
    private void bindingProbe$write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && bindingProbe$enabled && bindingProbe$device != null)
            tag.putUUID(DeviceIdentity.KEY, bindingProbe$device);
    }

    @Inject(method = "writeSafe", at = @At("TAIL"), require = 1, allow = 1)
    private void bindingProbe$writeSafe(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (bindingProbe$enabled && bindingProbe$device != null) tag.putUUID(DeviceIdentity.KEY, bindingProbe$device);
    }

    @Inject(method = "read", at = @At("TAIL"), require = 1, allow = 1)
    private void bindingProbe$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && bindingProbe$enabled)
            bindingProbe$device = tag.hasUUID(DeviceIdentity.KEY) ? tag.getUUID(DeviceIdentity.KEY) : null;
    }
}
