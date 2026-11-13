package dev.tassis.createfieldwork.schematic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

/** Original ruled-paper skin with explicit shadow-free native-font text and native input/narration. */
public final class ScribingPaperButton extends Button {
    private final int textColor;

    public ScribingPaperButton(int x, int y, int width, int height, Component label, OnPress action, int textColor) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        this.textColor = textColor;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean highlighted = active && isHoveredOrFocused();
        int fill = !active ? 0xffdddccf : highlighted ? 0xfffffbed : 0xfff7f8ea;
        int rule = highlighted ? 0xff977949 : 0xffb7a489;
        graphics.fill(x, y, x + w, y + h, 0xff413024);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        graphics.fill(x + 2, y + 2, x + w - 2, y + 3, rule);
        graphics.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, rule);
        var font = Minecraft.getInstance().font;
        var fitted = ScribingTextFit.fit(getMessage(), Math.max(0, w - 8), font::width, font::substrByWidth);
        var ordered = Language.getInstance().getVisualOrder(fitted);
        graphics.drawString(font, ordered, x + (w - font.width(ordered)) / 2,
            y + (h - font.lineHeight) / 2, active ? textColor : 0xff8a887c, false);
    }
}
