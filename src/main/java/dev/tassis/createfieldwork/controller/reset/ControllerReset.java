package dev.tassis.createfieldwork.controller.reset;

import com.simibubi.create.AllItems;
import dev.tassis.createfieldwork.platform.registry.FieldworkDataComponents;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.isolator.binding.BindingInteractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Preview is pure and blocked until revocation commits. Failure consumes one
 * ingredient into one pending result, not an aborted/duplicated quick-craft. */
public final class ControllerReset {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ControllerReset.class);
    private ControllerReset() {}
    public static boolean pending(ItemStack item) {
        return item != null && item.is(AllItems.LINKED_CONTROLLER.get()) && item.getOrDefault(FieldworkDataComponents.RESET_PENDING.get(), false);
    }
    public static ItemStack preview(ItemStack input) {
        var result = input.copyWithCount(1);
        result.set(FieldworkDataComponents.RESET_PENDING.get(), true);
        return result;
    }
    public static boolean complete(ServerPlayer actor, ItemStack result) {
        return complete(actor, result, true);
    }
    public static boolean completeCrafted(ServerPlayer actor, ItemStack result) {
        return complete(actor, result, false);
    }
    private static boolean complete(ServerPlayer actor, ItemStack result, boolean synchronize) {
        if (!pending(result)) return true;
        var id = ControllerDevice.read(result);
        try {
            if (!BindingInteractions.reset(actor, id)) return false;
            clear(result);
            // Quick-craft may pass a stats copy after moving actual output into
            // inventory. Never clear the unreset originals/copies of this ID.
            for (int i = 0; i < actor.getInventory().getContainerSize(); i++) {
                var item = actor.getInventory().getItem(i);
                if (pending(item) && Objects.equals(id, ControllerDevice.read(item))) clear(item);
            }
            var carried = actor.containerMenu.getCarried();
            if (pending(carried) && Objects.equals(id, ControllerDevice.read(carried))) clear(carried);
            actor.getInventory().setChanged();
            // Vanilla synchronizes extraction after consuming the ingredient;
            // avoid broadcasting a transient grid+cursor duplicate mid-craft.
            if (synchronize) actor.containerMenu.broadcastChanges();
            return true;
        } catch (RuntimeException | Error error) {
            LOGGER.error("Controller reset remains pending after failure; repair the retained Fieldwork transaction before retrying", error);
            actor.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.create_fieldwork.binding.unavailable"), true);
            return false;
        }
    }
    private static void clear(ItemStack item) {
        ControllerDevice.write(item, null);
        item.remove(FieldworkDataComponents.RESET_PENDING.get());
    }
}
