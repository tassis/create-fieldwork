package dev.tassis.createfieldwork.isolator.binding;

import dev.tassis.createfieldwork.platform.registry.FieldworkDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Component presence establishes managed mode; its value never encodes a target. */
public final class ControllerDevice {
    private ControllerDevice() {}
    public static UUID read(ItemStack stack) { return stack == null ? null : stack.get(FieldworkDataComponents.DEVICE.get()); }
    public static void write(ItemStack stack, UUID id) {
        if (id == null) stack.remove(FieldworkDataComponents.DEVICE.get()); else stack.set(FieldworkDataComponents.DEVICE.get(), id);
    }
}
