package dev.tassis.createfieldwork.schematic.input;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared selection policy; no client renderer references, also exercised headlessly. */
public final class BlueprintSelection {
    public static final int OFFHAND_SLOT = 40;
    public record Selected(ItemStack stack, int slot) {}
    private BlueprintSelection() {}
    public static boolean valid(ItemStack stack) { return stack != null && AllItems.SCHEMATIC.isIn(stack) && stack.has(AllDataComponents.SCHEMATIC_FILE); }
    public static int slot(boolean mainValid, int selectedHotbarSlot, boolean offValid) {
        return mainValid ? selectedHotbarSlot : offValid ? OFFHAND_SLOT : -1;
    }
    public static Selected select(Player player) {
        int slot = slot(valid(player.getMainHandItem()), player.getInventory().selected, valid(player.getOffhandItem()));
        return slot < 0 ? null : new Selected(player.getInventory().getItem(slot), slot);
    }
}
