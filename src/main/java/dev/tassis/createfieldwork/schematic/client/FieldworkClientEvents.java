package dev.tassis.createfieldwork.schematic.client;

import com.simibubi.create.CreateClient;
import dev.tassis.createfieldwork.platform.CreateFieldwork;
import dev.tassis.createfieldwork.mixin.schematic.client.SchematicHandlerAccessor;
import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateFieldwork.MOD_ID, value = Dist.CLIENT)
public final class FieldworkClientEvents {
    private FieldworkClientEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(ClientTickEvent.Post event) { SchematicClientState.tick(CreateClient.SCHEMATIC_HANDLER); }
    @SubscribeEvent public static void chunk(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) SchematicClientState.unloaded(level, event.getChunk().getPos());
    }
    @SubscribeEvent public static void world(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) SchematicClientState.clear();
    }
    /** Flush pending native placement before vanilla's subsequent swap packet can
     * move or replace that slot. This preserves the same schematic's transform in
     * either hand without copying it to another same-name stack. No custom packet. */
    private static void flushBeforeInventoryAction() {
        var mc = Minecraft.getInstance(); var handler = CreateClient.SCHEMATIC_HANDLER;
        if (mc.player == null || mc.level == null || mc.screen != null || !handler.isActive()
            || !BlueprintSelection.valid(handler.getActiveSchematicItem())) return;
        var accessor = (SchematicHandlerAccessor) handler;
        // Create's cooldown only schedules its next periodic sync. Even at zero,
        // force one immediately before vanilla swaps the server-side slots so the
        // destination hand cannot initialize from an older item transform.
        handler.sync();
        accessor.fieldwork$syncCooldown(0);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void key(InputEvent.Key event) {
        var options = Minecraft.getInstance().options;
        if (event.getAction() == GLFW.GLFW_PRESS && (options.keySwapOffhand.matches(event.getKey(), event.getScanCode()) || options.keyInventory.matches(event.getKey(), event.getScanCode()))) flushBeforeInventoryAction();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void mouse(InputEvent.MouseButton.Pre event) {
        var options = Minecraft.getInstance().options;
        if (event.getAction() == GLFW.GLFW_PRESS && (options.keySwapOffhand.matchesMouse(event.getButton()) || options.keyInventory.matchesMouse(event.getButton()))) flushBeforeInventoryAction();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui || !OffhandProjectionBuffer.activeOffhand(CreateClient.SCHEMATIC_HANDLER) || !CreateClient.SCHEMATIC_HANDLER.isDeployed()) return;
        String status = SchematicClientState.status();
        var text = status.equals("partial") ? Component.translatable("gui.create_fieldwork.schematic.partial", SchematicClientState.cached(), SchematicClientState.total())
            : Component.translatable("gui.create_fieldwork.schematic." + status);
        if (status.equals("partial") && ((SchematicHandlerAccessor) CreateClient.SCHEMATIC_HANDLER).fieldwork$slot() == 40 && !SchematicClientState.supportsGhostHiding())
            text = text.copy().append(" — ").append(Component.translatable("gui.create_fieldwork.schematic.hide_budget"));
        event.getGuiGraphics().drawString(mc.font, text, 8, 8, 0xDAD6CD);
        var hovered = SchematicClientState.hoveredBlock(CreateClient.SCHEMATIC_HANDLER);
        if (hovered != null) {
            int iconWidth = hovered.icon().isEmpty() ? 0 : 20;
            int x = (event.getGuiGraphics().guiWidth() - mc.font.width(hovered.name()) - iconWidth) / 2;
            int y = event.getGuiGraphics().guiHeight() / 2 + 16;
            if (!hovered.icon().isEmpty()) event.getGuiGraphics().renderItem(hovered.icon(), x, y);
            event.getGuiGraphics().drawString(mc.font, hovered.name(), x + iconWidth, y + 4, 0xFFFFFF);
        }
    }
    @EventBusSubscriber(modid = CreateFieldwork.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> Minecraft.getInstance().execute(SchematicClientState::reload));
        }
    }
}
