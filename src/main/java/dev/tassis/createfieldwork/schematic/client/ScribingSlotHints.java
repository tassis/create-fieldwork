package dev.tassis.createfieldwork.schematic.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Local visual hints only: real native models, no container mutation, copied sprites or fake counts. */
public final class ScribingSlotHints {
    private final ItemStack schematic = AllItems.SCHEMATIC.asStack();
    private final ItemStack book = new ItemStack(Items.BOOK);
    private final ItemStack clipboard = AllBlocks.CLIPBOARD.asStack();

    public static boolean showsHint(int index, boolean active, boolean occupied) {
        return active && !occupied && (index == 0 || index == 1);
    }

    public void render(GuiGraphics graphics, ScribingTableMenu menu, int left, int top) {
        for (int index = 0; index < 2; index++) {
            var slot = menu.getSlot(index);
            if (!showsHint(index, slot.isActive(), slot.hasItem())) continue;
            ItemStack hint = index == 0 ? schematic : (Util.getMillis() / 2000 % 2 == 0 ? book : clipboard);
            int x = left + slot.x;
            int y = top + slot.y;
            graphics.renderFakeItem(hint, x, y);
            graphics.flush();
            // Native model is washed toward the same well color. The overlay is
            // explicitly in front of the GUI item, confined to its16px well.
            graphics.fill(x, y, x + 16, y + 16, 200, 0xc2a6a79a);
        }
    }
}
