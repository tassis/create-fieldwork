package dev.tassis.createfieldwork.acceptance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** Real player 2x2 result-slot click. No reset service call substitutes for the hook. */
final class ControllerResetCrafting {
    private ControllerResetCrafting() {}
    static ItemStack craft(ServerPlayer actor, InteractionHand hand) {
        return craft(actor, hand, () -> {});
    }
    static ItemStack craft(ServerPlayer actor, InteractionHand hand, Runnable beforeTake) {
        var input = actor.getItemInHand(hand);
        actor.setItemInHand(hand, ItemStack.EMPTY);
        actor.closeContainer();
        var menu = actor.inventoryMenu; var grid = menu.getCraftSlots();
        if (!grid.isEmpty() || !menu.getCarried().isEmpty()) throw new IllegalStateException("Reset fixture requires empty grid/cursor");
        grid.setItem(0, input); menu.slotsChanged(grid);
        if (menu.getSlot(0).getItem().isEmpty()) throw new IllegalStateException("Native reset recipe produced no preview");
        beforeTake.run();
        menu.clicked(0, 0, ClickType.PICKUP, actor);
        var result = menu.getCarried(); menu.setCarried(ItemStack.EMPTY);
        if (!grid.isEmpty() || result.isEmpty()) throw new IllegalStateException("Native reset take did not consume exactly one input");
        actor.setItemInHand(hand, result);
        return result;
    }
}
