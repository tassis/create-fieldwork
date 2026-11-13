package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.platform.CreateFieldwork;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreateFieldwork.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ScribingTableClientRegistration {
    private ScribingTableClientRegistration() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FieldworkContent.SCRIBING_TABLE_MENU.get(), ScribingTableScreen::new);
    }
}
