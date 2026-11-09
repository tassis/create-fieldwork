package dev.tassis.createfieldwork.acceptance.mixin;

import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LecternControllerBlockEntity.class, remap = false)
public abstract class LecternAssignmentFaultMixin {
    @Inject(method = "setController", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lcom/simibubi/create/content/redstone/link/controller/LecternControllerBlockEntity;controllerData:Lnet/minecraft/world/item/component/ItemContainerContents;",
        shift = At.Shift.AFTER), require = 1, allow = 1)
    private void acceptance$assignment(ItemStack item, CallbackInfo ci) {
        if (AcceptanceFaults.throwLecternAssignment) {
            AcceptanceFaults.throwLecternAssignment = false;
            throw new IllegalStateException("lectern assignment fault");
        }
    }
}
