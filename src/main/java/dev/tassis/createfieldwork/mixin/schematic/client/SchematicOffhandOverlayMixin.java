package dev.tassis.createfieldwork.mixin.schematic.client;

import com.simibubi.create.content.schematics.client.SchematicHotbarSlotOverlay;
import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Slot40 is a real inventory slot, not a hotbar x-coordinate. */
@Mixin(value = SchematicHotbarSlotOverlay.class, remap = false)
public abstract class SchematicOffhandOverlayMixin {
    @Inject(method = "renderOn", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void fieldwork$offhand(GuiGraphics graphics, int slot, CallbackInfo ci) {
        if (slot != BlueprintSelection.OFFHAND_SLOT) return;
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            int left = graphics.guiWidth() / 2 + (mc.player.getMainArm() == HumanoidArm.RIGHT ? -120 : 97);
            int top = graphics.guiHeight() - 23;
            graphics.fill(left, top, left + 23, top + 1, 0xFFD8BC76);
            graphics.fill(left, top + 22, left + 23, top + 23, 0xFFD8BC76);
            graphics.fill(left, top, left + 1, top + 23, 0xFFD8BC76);
            graphics.fill(left + 22, top, left + 23, top + 23, 0xFFD8BC76);
            graphics.drawString(mc.font, Component.translatable("gui.create_fieldwork.schematic.offhand"), left - 5, top - 12, 0xD8BC76);
        }
        ci.cancel();
    }
}
