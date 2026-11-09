package dev.tassis.createfieldwork.mixin.controller.lectern;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import dev.tassis.createfieldwork.isolator.binding.BindingInteractions;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.state.IsolatorBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LinkedControllerItem.class, remap = false)
public abstract class ControllerInteractionMixin {
    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void fieldwork$bind(ItemStack stack, UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        var player = context.getPlayer();
        if (player instanceof ServerPlayer actor && ControllerReset.pending(stack) && !ControllerReset.complete(actor, stack)) {
            cir.setReturnValue(InteractionResult.FAIL); return;
        }
        if (player == null || !player.mayBuild() || !player.isShiftKeyDown() || !IsolatorBlocks.smart(context.getLevel().getBlockState(context.getClickedPos()))) return;
        if (player instanceof ServerPlayer actor && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BrassIsolatorBlockEntity be)
            BindingInteractions.bind(actor, context.getHand(), be);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void fieldwork$pendingReset(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer actor && ControllerReset.pending(stack) && !ControllerReset.complete(actor, stack))
            cir.setReturnValue(InteractionResultHolder.fail(stack));
    }
}
