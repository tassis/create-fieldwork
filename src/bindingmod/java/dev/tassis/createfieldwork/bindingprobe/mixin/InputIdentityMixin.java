package dev.tassis.createfieldwork.bindingprobe.mixin;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import dev.tassis.createfieldwork.bindingprobe.DeviceIdentity;
import dev.tassis.createfieldwork.bindingprobe.InputIdentityAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Observes native, validated item selection; does not cancel/alter/reroute input. */
@Mixin(value = LinkedControllerInputPacket.class, remap = false)
public abstract class InputIdentityMixin implements InputIdentityAccess {
    @Unique private UUID bindingProbe$device;
    @Unique private boolean bindingProbe$handled;
    @Override public UUID bindingProbe$observedDevice() { return bindingProbe$device; }
    @Override public boolean bindingProbe$handledItem() { return bindingProbe$handled; }

    @Inject(method = "handleItem", at = @At("HEAD"), require = 1, allow = 1)
    private void bindingProbe$observe(ServerPlayer actor, ItemStack controller, CallbackInfo ci) {
        bindingProbe$handled = true;
        bindingProbe$device = DeviceIdentity.read(controller);
    }
}
