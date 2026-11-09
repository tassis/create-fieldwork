package dev.tassis.createfieldwork.schematic.input;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Native use priority only, never an authorization or destination decision. */
public final class ControllerUsePriority {
    private ControllerUsePriority() {}
    public static boolean nativeUse(ItemStack mainHand, BlockState target) {
        return mainHand.is(AllItems.LINKED_CONTROLLER.get()) || target != null && AllBlocks.LECTERN_CONTROLLER.has(target);
    }
}
