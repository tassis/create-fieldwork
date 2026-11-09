package dev.tassis.createfieldwork.mixin.controller.lectern;

import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.mojang.logging.LogUtils;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.platform.registry.FieldworkDataComponents;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Preserve only Fieldwork device identity alongside Create's native frequency data. */
@Mixin(value = LecternControllerBlockEntity.class, remap = false)
public abstract class LecternControllerIdentityMixin {
    @Shadow private net.minecraft.world.item.component.ItemContainerContents controllerData;
    @Unique private UUID fieldwork$device;
    @Unique private boolean fieldwork$resetPending;
    @WrapMethod(method = "setController")
    private void fieldwork$set(ItemStack stack, Operation<Void> upstream) {
        var oldData = controllerData; var oldDevice = fieldwork$device; boolean oldPending = fieldwork$resetPending;
        try { upstream.call(stack); }
        catch (RuntimeException | Error error) {
            controllerData = oldData; fieldwork$device = oldDevice; fieldwork$resetPending = oldPending;
            throw error;
        }
        // Native null means no-op; EMPTY really assigns an empty controller.
        if (stack != null) {
            fieldwork$device = ControllerDevice.read(stack);
            fieldwork$resetPending = ControllerReset.pending(stack);
        }
        var be = (LecternControllerBlockEntity) (Object) this;
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            try {
                FieldworkStateAccess.get(level.getServer()).controllers()
                    .reconcileLectern(level.dimension(), be.getBlockPos());
            } catch (RuntimeException | Error error) {
                // Native assignment has committed and its caller may already have
                // moved the item. Keep that coherent state; ControllerSources
                // retains the failed refresh scope for the next server tick.
                LogUtils.getLogger().error("Fieldwork lectern assignment committed with controller refresh pending", error);
            }
        }
    }
    @Inject(method = "startUsing", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$started(Player player, CallbackInfo ci) { fieldwork$reconcileLectern(); }
    @Inject(method = "stopUsing", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$stopped(Player player, CallbackInfo ci) { fieldwork$reconcileLectern(); }
    @Inject(method = "createLinkedController", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$create(CallbackInfoReturnable<ItemStack> cir) {
        if (fieldwork$device != null) ControllerDevice.write(cir.getReturnValue(), fieldwork$device);
        if (fieldwork$resetPending) cir.getReturnValue().set(FieldworkDataComponents.RESET_PENDING.get(), true);
    }
    @Inject(method = "dropController", at = @At("TAIL"), require = 1, allow = 1)
    private void fieldwork$drop(BlockState state, CallbackInfo ci) {
        fieldwork$device = null; fieldwork$resetPending = false; fieldwork$reconcileLectern();
    }
    @Inject(method = "write", at = @At("TAIL"), require = 1, allow = 1)
    private void fieldwork$write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        fieldwork$store(tag);
    }
    @Inject(method = "writeSafe", at = @At("TAIL"), require = 1, allow = 1)
    private void fieldwork$safe(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) { fieldwork$store(tag); }
    @Unique private void fieldwork$store(CompoundTag tag) {
        if (fieldwork$device != null) tag.putUUID("FieldworkDevice", fieldwork$device); else tag.remove("FieldworkDevice");
        if (fieldwork$resetPending) tag.putBoolean("FieldworkResetPending", true); else tag.remove("FieldworkResetPending");
    }
    @Inject(method = "read", at = @At("TAIL"), require = 1, allow = 1)
    private void fieldwork$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        fieldwork$device = tag.hasUUID("FieldworkDevice") ? tag.getUUID("FieldworkDevice") : null;
        fieldwork$resetPending = tag.getBoolean("FieldworkResetPending");
        fieldwork$reconcileLectern();
    }
    @Unique private void fieldwork$reconcileLectern() {
        var be = (LecternControllerBlockEntity) (Object) this;
        if (!(be.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        try {
            FieldworkStateAccess.get(level.getServer()).controllers()
                .reconcileLectern(level.dimension(), be.getBlockPos());
        } catch (RuntimeException | Error error) {
            LogUtils.getLogger().error("Fieldwork lectern controller refresh will retry", error);
        }
    }
}
