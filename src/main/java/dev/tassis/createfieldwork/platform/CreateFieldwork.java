package dev.tassis.createfieldwork.platform;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.registry.FieldworkDataComponents;
import dev.tassis.createfieldwork.integration.OptionalIntegrationBootstrap;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CreateFieldwork.MOD_ID)
public final class CreateFieldwork {
    public static final String MOD_ID = "create_fieldwork";

    public CreateFieldwork(IEventBus bus) {
        FieldworkContent.register(bus);
        FieldworkDataComponents.register(bus);
        bus.addListener(IntegrationHandshake::registerPayloads);
        bus.addListener(IntegrationHandshake::registerTask);
        NeoForge.EVENT_BUS.addListener(WorldIntegrationGuard::aboutToStart);
        if (AeronauticsIntegration.active()) OptionalIntegrationBootstrap.register(bus);
    }
}
