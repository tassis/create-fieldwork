package dev.tassis.createfieldwork;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import dev.tassis.createfieldwork.registry.FieldworkContent;

@Mod(CreateFieldwork.MOD_ID)
public final class CreateFieldwork {
    public static final String MOD_ID = "create_fieldwork";

    public CreateFieldwork(IEventBus bus) {
        FieldworkContent.register(bus);
    }
}
