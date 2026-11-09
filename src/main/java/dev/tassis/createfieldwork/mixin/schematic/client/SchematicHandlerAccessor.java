package dev.tassis.createfieldwork.mixin.schematic.client;

import com.simibubi.create.content.schematics.client.SchematicHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicHandler.class, remap = false)
public interface SchematicHandlerAccessor {
    @Accessor("activeHotbarSlot") int fieldwork$slot();
    @Accessor("syncCooldown") int fieldwork$syncCooldown();
    @Accessor("syncCooldown") void fieldwork$syncCooldown(int cooldown);
}
