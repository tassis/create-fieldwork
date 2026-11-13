package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout;
import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout.Rect;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** Text-free original skin; all labels and slot items are rendered natively on the client. */
public final class ScribingTableScreen extends AbstractContainerScreen<ScribingTableMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
        "create_fieldwork", "textures/gui/schematic_scribing_table.png");
    private static final int INK = 0x43382b;
    private ScribingPaperButton options;
    private ScribingPaperButton print;
    private ScribingPaperButton mode;
    private ScribingPaperButton blockEntities;
    private boolean settingsOpen;
    private int lastPresentationState = -1;
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
        options = button(ScribingTableLayout.OPTIONS, Component.empty(), button -> toggleSettings(), INK);
        print = button(ScribingTableLayout.PRINT, tr("print"), button -> send(ScribingTableMenu.PRINT), 0x395334);
        print.setTooltip(Tooltip.create(tr("overwrite")));
        mode = button(ScribingTableLayout.MODE, Component.empty(), button -> send(ScribingTableMenu.CYCLE_MODE), INK);
        blockEntities = button(ScribingTableLayout.BLOCK_ENTITIES, Component.empty(),
            button -> send(ScribingTableMenu.TOGGLE_BLOCK_ENTITIES), INK);
        lastPresentationState = -1;
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

    private void toggleSettings() {
        if (!settingsOpen && !menu.getCarried().isEmpty()) {
            return;
        }
        settingsOpen = !settingsOpen;
        hoveredSlot = null;
        refreshButtons();
        setFocused(settingsOpen ? mode : options);
    }

    private void refreshButtons() {
        mode.visible = blockEntities.visible = settingsOpen;
        options.active = settingsOpen || menu.getCarried().isEmpty();
        int state = menu.replaceMode() | (menu.replaceBlockEntities() ? 4 : 0)
            | (settingsOpen ? 8 : 0) | (!options.active ? 16 : 0);
        if (state != lastPresentationState) {
            Component modeLabel = tr("mode." + menu.replaceMode());
            Component entityLabel = tr("block_entities." + menu.replaceBlockEntities());
            mode.setMessage(modeLabel);
            mode.setTooltip(Tooltip.create(modeLabel));
            blockEntities.setMessage(entityLabel);
            blockEntities.setTooltip(Tooltip.create(entityLabel));
            options.setMessage(settingsOpen ? CommonComponents.GUI_DONE : tr("settings"));
            options.setTooltip(Tooltip.create(!options.active ? tr("put_down_item")
                : modeLabel.copy().append("\n").append(entityLabel)));
            lastPresentationState = state;
        }
        print.active = !settingsOpen && !menu.isPrinting() && menu.getSlot(0).hasItem()
            && menu.getSlot(1).hasItem() && !menu.getSlot(2).hasItem();
        menu.setWorkSlotsVisible(!settingsOpen);
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
        if (!settingsOpen && filled > 0) {
            graphics.fill(leftPos + progress.x() + 1, topPos + progress.y() + 1,
                leftPos + progress.right() - 1, topPos + progress.y() + 1 + filled, 0xff769358);
        }
        if (settingsOpen) {
            Rect pane = ScribingTableLayout.SETTINGS;
            int x = leftPos + pane.x();
            int y = topPos + pane.y();
            graphics.fill(x, y, x + pane.width(), y + pane.height(), 0xff998060);
            graphics.fill(x + 1, y + 1, x + pane.width() - 1, y + pane.height() - 1, 0xfff1efdd);
            graphics.fill(x + 2, y + 2, x + pane.width() - 2, y + 3, 0xffcebea2);
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
        if (settingsOpen) {
            centered(graphics, tr("settings"), ScribingTableLayout.SETTINGS_TITLE, INK);
        } else {
            centered(graphics, tr("scribing_area"), ScribingTableLayout.FLOW_LABEL, INK);
            centered(graphics, tr("blueprint"), ScribingTableLayout.SCHEMATIC_LABEL, 0xeef1d8);
        }
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
        if (!settingsOpen && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 3) {
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

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && settingsOpen) {
            toggleSettings();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void removed() {
        menu.setWorkSlotsVisible(true);
        super.removed();
    }

    private static Component tr(String suffix) {
        return Component.translatable("gui.create_fieldwork.scribing." + suffix);
    }
}
