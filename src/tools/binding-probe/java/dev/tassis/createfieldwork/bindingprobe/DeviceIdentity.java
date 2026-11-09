package dev.tassis.createfieldwork.bindingprobe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** Experimental storage only. Production should select a versioned data component. */
public final class DeviceIdentity {
    public static final String KEY = "fieldwork_binding_probe_device";
    private DeviceIdentity() {}

    public static UUID read(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(KEY) ? tag.getUUID(KEY) : null;
    }

    public static void write(ItemStack stack, UUID id) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (id == null) tag.remove(KEY); else tag.putUUID(KEY, id);
        });
    }
}
