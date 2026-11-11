package dev.tassis.createfieldwork.integration;

import dev.tassis.createfieldwork.platform.FieldworkChunkEvents;
import dev.tassis.createfieldwork.platform.FieldworkCommands;
import dev.tassis.createfieldwork.platform.IsolatorIntegrationBridge;
import dev.tassis.createfieldwork.platform.registry.OptionalFieldworkContent;
import dev.tassis.createfieldwork.integration.schematic.SableWorldTargetSupport;
import dev.tassis.createfieldwork.schematic.comparison.WorldTargetSupport;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class OptionalIntegrationBootstrap {
    private OptionalIntegrationBootstrap() {}

    public static void register(IEventBus modBus) {
        OptionalFieldworkContent.register(modBus);
        IsolatorIntegrationBridge.install(new FieldworkIntegrationRuntime());
        WorldTargetSupport.install(SableWorldTargetSupport::capture);

        var gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(FieldworkChunkEvents::started);
        gameBus.addListener(FieldworkChunkEvents::loaded);
        gameBus.addListener(FieldworkChunkEvents::tick);
        gameBus.addListener(FieldworkChunkEvents::changedDimension);
        gameBus.addListener(FieldworkChunkEvents::cloned);
        gameBus.addListener(FieldworkChunkEvents::loggedOut);
        gameBus.addListener(FieldworkChunkEvents::unloaded);
        gameBus.addListener(FieldworkCommands::commands);
    }
}
