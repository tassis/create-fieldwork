package dev.tassis.createfieldwork.schematic.table;

import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ScribingTableMenu extends AbstractContainerMenu {
    public static final int PRINT = 0;
    private final Container inventory;
    private final ContainerData data;
    private final ScribingTableBlockEntity table;
    private final BlockPos position;

    public ScribingTableMenu(int id, Inventory player, RegistryFriendlyByteBuf buffer) {
        this(id, player, new SimpleContainer(3), new SimpleContainerData(4), null, buffer.readBlockPos());
    }

    public ScribingTableMenu(int id, Inventory player, ScribingTableBlockEntity table) {
        this(id, player, table, table.menuData(), table, table.getBlockPos());
    }

    private ScribingTableMenu(int id, Inventory player, Container inventory, ContainerData data,
                              ScribingTableBlockEntity table, BlockPos position) {
        super(FieldworkContent.SCRIBING_TABLE_MENU.get(), id);
        checkContainerSize(inventory, 3);
        checkContainerDataCount(data, 4);
        this.inventory = inventory;
        this.data = data;
        this.table = table;
        this.position = position;
        var schematic = ScribingTableLayout.SCHEMATIC;
        var medium = ScribingTableLayout.MEDIUM;
        var output = ScribingTableLayout.OUTPUT;
        addSlot(new Slot(inventory, 0, schematic.x(), schematic.y()) {
            @Override public boolean mayPlace(ItemStack stack) { return ScribingChecklist.isBlueprint(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inventory, 1, medium.x(), medium.y()) {
            @Override public boolean mayPlace(ItemStack stack) { return ScribingChecklist.isMedium(stack); }
        });
        addSlot(new Slot(inventory, 2, output.x(), output.y()) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                var point = ScribingTableLayout.playerSlot(row * 9 + column);
                addSlot(new Slot(player, column + row * 9 + 9, point.x(), point.y()));
            }
        }
        for (int column = 0; column < 9; column++) {
            var point = ScribingTableLayout.playerSlot(27 + column);
            addSlot(new Slot(player, column, point.x(), point.y()));
        }
        addDataSlots(data);
    }

    public ScribingTableBlockEntity table() { return table; }
    public BlockPos position() { return position; }
    public ScribingChecklist.Status status() { return ScribingChecklist.Status.byId(data.get(0)); }
    public int remainingTicks() { return Math.max(0, data.get(1)); }
    public boolean isPrinting() { return data.get(3) != 0; }
    public int progressTicks() { return Math.max(0, Math.min(ScribingTableBlockEntity.PRINT_TICKS, data.get(2))); }
    public float progress() { return progressTicks() / (float) ScribingTableBlockEntity.PRINT_TICKS; }

    @Override public boolean stillValid(Player player) { return inventory.stillValid(player); }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (table == null || player.level().isClientSide || player.containerMenu != this
            || player.isSpectator() || !stillValid(player)) {
            return false;
        }
        if (button != PRINT) return false;
        table.printChecklist();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ScribingChecklist.isBlueprint(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (ScribingChecklist.isMedium(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (slotIndex < 30) {
            if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 3, 30, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
