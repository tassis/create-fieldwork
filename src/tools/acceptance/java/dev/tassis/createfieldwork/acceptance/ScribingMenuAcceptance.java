package dev.tassis.createfieldwork.acceptance;

import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BiConsumer;

/** Actual native container transfer and stable indices after the presentation re-layout. */
final class ScribingMenuAcceptance {
    static void run(ScribingFixtures fixtures, BiConsumer<String, Boolean> check) {
        var table = fixtures.table(ScribingFixtures.TABLE.north(24));
        var actor = fixtures.actor(table.getBlockPos());
        var menu = fixtures.menu(table, actor);
        for (int index = 0; index < 39; index++) {
            var point = index < 3 ? ScribingTableLayout.tableSlot(index) : ScribingTableLayout.playerSlot(index - 3);
            var slot = menu.getSlot(index);
            check.accept("production menu slot matches shared UI geometry " + index, slot.x == point.x() && slot.y == point.y());
        }
        menu.setWorkSlotsVisible(false);
        check.accept("foldout presentation hides only the three work slots", menu.slots.subList(0, 3).stream().noneMatch(net.minecraft.world.inventory.Slot::isActive)
            && menu.slots.subList(3, 39).stream().allMatch(net.minecraft.world.inventory.Slot::isActive));
        menu.setWorkSlotsVisible(true);
        check.accept("closing presentation foldout restores schematic without changing content", menu.getSlot(0).isActive() && table.isEmpty());

        actor.getInventory().setItem(9, fixtures.blueprint(ScribingFixtures.ANCHOR));
        actor.getInventory().setItem(10, new ItemStack(Items.BOOK, 2));
        quickMove(actor, menu, 3);
        quickMove(actor, menu, 4);
        check.accept("native quick-move packets route schematic and book to unchanged semantic slots", table.getItem(0).getCount() == 1
            && table.getItem(1).is(Items.BOOK) && table.getItem(1).getCount() == 2 && table.getItem(2).isEmpty());
        menu.clickMenuButton(actor, ScribingTableMenu.PRINT);
        check.accept("quick-move workflow starts a real job without consuming inputs", table.isPrinting()
            && table.getItem(1).getCount() == 2 && table.getItem(2).isEmpty());
        ScribingFixtures.complete(table);
        check.accept("re-layout still prints one result without consuming schematic", table.getItem(0).getCount() == 1
            && table.getItem(1).getCount() == 1 && table.getItem(2).is(Items.WRITTEN_BOOK));
        quickMove(actor, menu, 2);
        check.accept("native quick-move retrieves output without duplication", table.getItem(2).isEmpty()
            && actor.getInventory().items.stream().filter(stack -> stack.is(Items.WRITTEN_BOOK)).mapToInt(ItemStack::getCount).sum() == 1);
        quickMove(actor, menu, 0);
        quickMove(actor, menu, 1);
        check.accept("quick-move returns both inputs and leaves table empty", table.isEmpty()
            && actor.getInventory().items.stream().filter(stack -> stack.is(Items.BOOK)).mapToInt(ItemStack::getCount).sum() == 1
            && actor.getInventory().items.stream().filter(stack -> com.simibubi.create.AllItems.SCHEMATIC.isIn(stack))
                .mapToInt(ItemStack::getCount).sum() == 1);
    }

    static void quickMove(net.minecraft.server.level.ServerPlayer actor, ScribingTableMenu menu, int slot) {
        actor.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, 0,
            ClickType.QUICK_MOVE, ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
    }

    private ScribingMenuAcceptance() {}
}
