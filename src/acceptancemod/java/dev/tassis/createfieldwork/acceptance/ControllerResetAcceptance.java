package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.controller.reset.ControllerResetRecipe;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Native player2x2/table3x3 extraction, including the quick-move copy path. */
final class ControllerResetAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    ControllerResetAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    void run() {
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ResetMatrix"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        var table = new BlockPos(512, 120, 32);
        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        actor.setPos(513.5, 120, 32.5); level.addNewPlayer(actor);
        var ledger = FieldworkStateAccess.get(level.getServer()).bindingRevocations();
        try {
            for (int width : new int[]{2, 3}) for (var click : new ClickType[]{ClickType.PICKUP, ClickType.QUICK_MOVE}) for (boolean managed : new boolean[]{false, true}) {
                actor.closeContainer(); actor.getInventory().clearContent();
                AbstractContainerMenu menu = width == 2 ? actor.inventoryMenu : new CraftingMenu(5, actor.getInventory(), ContainerLevelAccess.create(level, table));
                actor.containerMenu = menu;
                var grid = (CraftingContainer) menu.getSlot(1).container;
                var item = controller(); var id = managed ? ledger.issue() : null;
                ControllerDevice.write(item, id);
                var oldProgram = item.get(AllDataComponents.LINKED_CONTROLLER_ITEMS);
                grid.setItem(width * width - 1, item); menu.slotsChanged(grid);
                String label = width + "x" + width + "/" + click + "/" + managed;
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), level);
                if (!managed) {
                    check.accept(label + " ordinary controller has no pointless unbind recipe", recipe.isEmpty() && menu.getSlot(0).getItem().isEmpty());
                    menu.clicked(0, 0, click, actor);
                    check.accept(label + " no-op attempt keeps its ingredient and channels", !grid.isEmpty() && oldProgram.equals(item.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)) && !ControllerReset.pending(item));
                    menu.removed(actor);
                    continue;
                }
                check.accept(label + " selects registered reset serializer via native recipe manager", recipe.isPresent() && recipe.get().value() instanceof ControllerResetRecipe);
                var preview = menu.getSlot(0).getItem();
                check.accept(label + " preview is pending and preserves old managed ID", ControllerReset.pending(preview) && java.util.Objects.equals(id, ControllerDevice.read(preview)));
                check.accept(label + " preview preserves channels and never mutates ingredient", preview.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY).equals(oldProgram) &&
                    oldProgram.equals(item.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)) && (id == null || ledger.permits(id, 1)));
                menu.clicked(0, 0, click, actor);
                ItemStack result = menu.getCarried();
                int resultCount = result.isEmpty() ? 0 : result.getCount();
                for (int i = 0; i < actor.getInventory().getContainerSize(); i++) {
                    var found = actor.getInventory().getItem(i);
                    if (found.is(AllItems.LINKED_CONTROLLER.get())) { result = found; resultCount += found.getCount(); }
                }
                check.accept(label + " consumes exactly one ingredient and produces exactly one result", grid.isEmpty() && resultCount == 1);
                check.accept(label + " real extracted output has no binding or pending marker and keeps channels", !result.isEmpty() && ControllerDevice.read(result) == null && !ControllerReset.pending(result) &&
                    result.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY).equals(oldProgram));
                check.accept(label + " reset preserves unrelated custom name", result.getHoverName().getString().equals("Named controller"));
                check.accept(label + " reset permanently revokes the old device", id == null || ledger.revoked(id));
                menu.setCarried(ItemStack.EMPTY);
            }
            actor.closeContainer(); actor.getInventory().clearContent();
            {
                var copyMenu = actor.inventoryMenu; actor.containerMenu = copyMenu;
                var copyGrid = copyMenu.getCraftSlots(); var source = controller(); var copyId = ledger.issue(); ControllerDevice.write(source, copyId);
                var stale = source.copy(); var samePending = ControllerReset.preview(source);
                var otherSource = controller(); var otherId = ledger.issue(); ControllerDevice.write(otherSource, otherId);
                var otherPending = ControllerReset.preview(otherSource);
                actor.getInventory().setItem(0, stale); actor.getInventory().setItem(1, samePending); actor.getInventory().setItem(2, otherPending);
                copyMenu.setCarried(ControllerReset.preview(source));
                copyGrid.setItem(0, source); copyMenu.slotsChanged(copyGrid);
                copyMenu.clicked(0, 0, ClickType.QUICK_MOVE, actor);
                check.accept("unbind leaves stale non-pending same-ID copy managed rather than making it ordinary", copyId.equals(ControllerDevice.read(stale)) && !ControllerReset.pending(stale) && ledger.revoked(copyId));
                check.accept("unbind clears matching pending inventory and cursor copies only after veto", !ControllerReset.pending(samePending) && ControllerDevice.read(samePending) == null &&
                    !ControllerReset.pending(copyMenu.getCarried()) && ControllerDevice.read(copyMenu.getCarried()) == null);
                check.accept("unbind leaves another device's pending copy and generation untouched", ControllerReset.pending(otherPending) && otherId.equals(ControllerDevice.read(otherPending)) && ledger.permits(otherId, 1));
                copyMenu.setCarried(ItemStack.EMPTY); actor.getInventory().clearContent();
            }
            {
                // A real shift-take must consume one input even if revocation
                // throws after output has moved into the player's inventory.
                var failedMenu = actor.inventoryMenu; actor.containerMenu = failedMenu;
                var failedGrid = failedMenu.getCraftSlots(); var failedInput = controller();
                var failedId = ledger.issue(); ControllerDevice.write(failedInput, failedId);
                failedGrid.setItem(0, failedInput); failedMenu.slotsChanged(failedGrid);
                BindingFaults.arm("revocation");
                try { failedMenu.clicked(0, 0, ClickType.QUICK_MOVE, actor); }
                finally { BindingFaults.point = ""; }
                ItemStack pending = ItemStack.EMPTY; int count = 0, slot = -1;
                for (int i = 0; i < actor.getInventory().getContainerSize(); i++) {
                    var found = actor.getInventory().getItem(i);
                    if (found.is(AllItems.LINKED_CONTROLLER.get())) { pending = found; count += found.getCount(); slot = i; }
                }
                check.accept("failed native shift take consumes one input without duplicating controllers", failedGrid.isEmpty() && count == 1);
                check.accept("failed shift output remains pending with old ID and channels", ControllerReset.pending(pending) && failedId.equals(ControllerDevice.read(pending)) &&
                    controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS).equals(pending.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
                var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
                check.accept("failed shift reset keeps one repair token", coordinator.failedTokens().size() == 1);
                var unrelatedInput = controller(); var unrelatedId = ledger.issue(); ControllerDevice.write(unrelatedInput, unrelatedId);
                failedGrid.setItem(0, unrelatedInput); failedMenu.slotsChanged(failedGrid);
                failedMenu.clicked(0, 0, ClickType.PICKUP, actor);
                check.accept("unrelated device unbind waits without mutating generation or adding tokens", ControllerReset.pending(failedMenu.getCarried()) &&
                    unrelatedId.equals(ControllerDevice.read(failedMenu.getCarried())) && ledger.permits(unrelatedId, 1) && coordinator.failedTokens().size() == 1);
                long token = coordinator.failedTokens().iterator().next();
                check.accept("failed shift token repairs normally", coordinator.repair(token));
                actor.getInventory().setItem(slot, ItemStack.EMPTY); actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pending);
                pending.getItem().onItemUseFirst(pending, new net.minecraft.world.item.context.UseOnContext(actor, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(table), net.minecraft.core.Direction.UP, table, false)));
                check.accept("native use-first finishes repaired pending shift output with channels kept", !ControllerReset.pending(pending) && ControllerDevice.read(pending) == null &&
                    controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS).equals(pending.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
                check.accept("finishing one device does not clear unrelated pending cursor item", ControllerReset.pending(failedMenu.getCarried()) && unrelatedId.equals(ControllerDevice.read(failedMenu.getCarried())));
                failedMenu.setCarried(ItemStack.EMPTY);
                actor.getInventory().clearContent();
            }
            var menu = actor.inventoryMenu; actor.containerMenu = menu;
            var grid = menu.getCraftSlots(); var input = controller(); var cancelId = ledger.issue(); ControllerDevice.write(input, cancelId);
            var program = input.get(AllDataComponents.LINKED_CONTROLLER_ITEMS);
            grid.setItem(0, input); menu.slotsChanged(grid);
            check.accept("cancel fixture has real reset preview", ControllerReset.pending(menu.getSlot(0).getItem()));
            menu.removed(actor);
            ItemStack returned = ItemStack.EMPTY;
            for (int i = 0; i < actor.getInventory().getContainerSize(); i++) if (actor.getInventory().getItem(i).is(AllItems.LINKED_CONTROLLER.get())) returned = actor.getInventory().getItem(i);
            check.accept("closing preview returns unmodified ingredient without revoking device", cancelId.equals(ControllerDevice.read(returned)) &&
                program.equals(returned.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)) && !ControllerReset.pending(returned) && ledger.permits(cancelId, 1));
            check.accept("reset matrix leaves no failed transactions", FieldworkStateAccess.get(level.getServer()).transitions().failedTokens().isEmpty());
        } finally {
            actor.closeContainer(); level.removePlayerImmediately(actor, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            transport.embedded.finishAndReleaseAll();
        }
    }
    private ItemStack controller() {
        var item = AllItems.LINKED_CONTROLLER.asStack();
        item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Named controller"));
        return item;
    }
}
