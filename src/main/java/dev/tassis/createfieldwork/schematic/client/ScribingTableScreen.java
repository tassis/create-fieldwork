package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout;
import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout.Rect;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Text-free original skin; all labels and slot items are rendered natively on the client. */
public final class ScribingTableScreen extends AbstractContainerScreen<ScribingTableMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
        "create_fieldwork", "textures/gui/schematic_scribing_table.png");
    private static final int INK = 0x43382b;
    private ScribingPaperButton print;
    private ScribingSlotHints slotHints;

    public ScribingTableScreen(ScribingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ScribingTableLayout.WIDTH;
        imageHeight = ScribingTableLayout.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        slotHints = new ScribingSlotHints();
        print = button(ScribingTableLayout.PRINT, tr("print"), button -> send(ScribingTableMenu.PRINT), 0x395334);
        print.setTooltip(Tooltip.create(tr("overwrite")));
        refreshButtons();
    }

    private ScribingPaperButton button(Rect bounds, Component label, net.minecraft.client.gui.components.Button.OnPress action, int color) {
        return addRenderableWidget(new ScribingPaperButton(leftPos + bounds.x(), topPos + bounds.y(),
            bounds.width(), bounds.height(), label, action, color));
    }

    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void refreshButtons() {
        print.active = !menu.isPrinting() && menu.getSlot(0).hasItem()
            && menu.getSlot(1).hasItem() && !menu.getSlot(2).hasItem();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight,
            ScribingTableLayout.TEXTURE_SIZE, ScribingTableLayout.TEXTURE_SIZE);
        slotHints.render(graphics, menu, leftPos, topPos);
        Rect progress = ScribingTableLayout.PROGRESS;
        int filled = (int) ((progress.height() - 2) * menu.progress());
        if (filled > 0) {
            graphics.fill(leftPos + progress.x() + 1, topPos + progress.y() + 1,
                leftPos + progress.right() - 1, topPos + progress.y() + 1 + filled, 0xff769358);
        }
        int color = switch (menu.status()) {
                case PARTIAL -> 0xffb18439;
                case PRINTED, PRINTING -> 0xff4a7e4a;
                case IDLE, COOLDOWN -> 0xff577987;
                default -> 0xffa24d3e;
        };
        graphics.fill(leftPos + 14, topPos + 125, leftPos + 17, topPos + 128, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        centered(graphics, title, ScribingTableLayout.TITLE, INK);
        centered(graphics, tr("scribing_area"), ScribingTableLayout.FLOW_LABEL, INK);
        centered(graphics, tr("blueprint"), ScribingTableLayout.SCHEMATIC_LABEL, 0xeef1d8);
        label(graphics, shortStatus(), ScribingTableLayout.STATUS_TEXT, INK, false);
        label(graphics, playerInventoryTitle, ScribingTableLayout.INVENTORY_LABEL, INK, false);
    }

    private void centered(GuiGraphics graphics, Component text, Rect bounds, int color) {
        label(graphics, text, bounds, color, true);
    }

    private void label(GuiGraphics graphics, Component text, Rect bounds, int color, boolean centered) {
        var fitted = ScribingTextFit.fit(text, bounds.width(), font::width, font::substrByWidth);
        var ordered = Language.getInstance().getVisualOrder(fitted);
        int x = bounds.x() + (centered ? (bounds.width() - font.width(ordered)) / 2 : 0);
        graphics.drawString(font, ordered, x, bounds.y() + (bounds.height() - font.lineHeight) / 2, color, false);
    }

    private Component shortStatus() {
        if (menu.status() != ScribingChecklist.Status.IDLE) {
            return tr("short." + menu.status().name().toLowerCase(java.util.Locale.ROOT));
        }
        if (menu.getSlot(2).hasItem()) return tr("short.output_full");
        if (!menu.getSlot(0).hasItem()) return tr("hint.schematic");
        if (!menu.getSlot(1).hasItem()) return tr("hint.medium");
        return tr("hint.ready");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 3) {
            String key = switch (hoveredSlot.index) {
                case 0 -> "slot.schematic";
                case 1 -> "slot.medium";
                default -> "slot.output";
            };
            graphics.renderTooltip(font, tr(key), mouseX, mouseY);
        }
        if (isHovering(ScribingTableLayout.STATUS.x(), ScribingTableLayout.STATUS.y(),
            ScribingTableLayout.STATUS.width(), ScribingTableLayout.STATUS.height(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(menu.status().translationKey()), mouseX, mouseY);
        }
    }

    private static Component tr(String suffix) {
        return Component.translatable("gui.create_fieldwork.scribing." + suffix);
    }
}
