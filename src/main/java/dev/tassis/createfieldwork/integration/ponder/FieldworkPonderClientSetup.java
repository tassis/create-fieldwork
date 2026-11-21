package dev.tassis.createfieldwork.integration.ponder;

import dev.tassis.createfieldwork.platform.CreateFieldwork;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateFieldwork.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FieldworkPonderClientSetup {
    private FieldworkPonderClientSetup() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new FieldworkPonderPlugin()));
    }
}
