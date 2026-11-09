package dev.tassis.createfieldwork.isolator.binding;

import com.simibubi.create.AllItems;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import dev.ryanhcode.sable.Sable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class BindingInteractions {
    private BindingInteractions() {
    }

    public static boolean bind(ServerPlayer actor, InteractionHand hand, BrassIsolatorBlockEntity be) {
        var level = actor.serverLevel();
        var item = actor.getItemInHand(hand);
        if (!actor.mayBuild()
            || !actor.isShiftKeyDown()
            || !item.is(AllItems.LINKED_CONTROLLER.get())
            || be.getLevel() != level
            || actor.isRemoved()
            || actor.isDeadOrDying()
            || actor.connection == null
            || !actor.connection.isAcceptingMessages()) {
            return false;
        }

        double reach = actor.blockInteractionRange();
        double distance = Sable.HELPER.distanceSquaredWithSubLevels(
            level,
            actor.getEyePosition(),
            Vec3.atCenterOf(be.getBlockPos())
        );
        if (distance > reach * reach) {
            return false;
        }

        var state = FieldworkStateAccess.get(actor.server);
        var key = be.currentCraft();
        if (key == null || !(ScopeResolver.craft(level, key, null) instanceof Scope.Private)) {
            message(actor, "unavailable");
            return false;
        }

        var id = ControllerDevice.read(item);
        var ledger = state.bindingRevocations();
        if (id != null
            && !ledger.revoked(id)
            && !key.equals(state.bindings().target(id, level))) {
            message(actor, "unbind_first");
            return false;
        }
        if (!ledger.healthy()) {
            message(actor, "unavailable");
            return false;
        }
        UUID authorized = id != null && !ledger.revoked(id) ? id : null;
        if (!be.accepts(key, authorized)) {
            message(actor, "capacity");
            return false;
        }

        var devices = id == null ? java.util.Set.<UUID>of() : java.util.Set.of(id);
        var change = state.controllers().bindingChange(actor.getUUID(), devices);
        var coordinator = state.transitions();
        coordinator.beginBinding(level, java.util.List.of(be.getBlockPos()), change);
        boolean success = false;
        try {
            coordinator.bindingAffected(change);
            state.controllers().invalidatePlayer(actor);
            boolean issued = id == null || ledger.revoked(id);
            if (issued) {
                id = ledger.issue();
            }
            try {
                be.grant(key, id, ledger.generation(id));
            } catch (RuntimeException error) {
                if (issued) {
                    ledger.revoke(id);
                }
                state.bindings().rebuild();
                throw error;
            }
            ControllerDevice.write(item, id);
            actor.getInventory().setChanged();
            actor.containerMenu.broadcastChanges();
            success = true;
        } finally {
            coordinator.end("binding", success);
        }
        boolean committed = key.equals(state.bindings().target(id, level));
        message(actor, committed ? "bound" : "unavailable");
        return committed;
    }

    /** Commit a pending self-crafting result before it may become ordinary. */
    public static boolean reset(ServerPlayer actor, java.util.UUID id) {
        if (actor.isRemoved()
            || actor.isDeadOrDying()
            || actor.connection == null
            || !actor.connection.isAcceptingMessages()) {
            return false;
        }
        var state = FieldworkStateAccess.get(actor.server);
        if (id != null && !state.bindingRevocations().healthy()) {
            message(actor, "unavailable");
            return false;
        }
        // A failed result must not start overlapping retries, or become ordinary
        // merely because its actor moved away from the retained position barrier.
        if (!state.transitions().failedTokens().isEmpty()) {
            message(actor, "unavailable");
            return false;
        }
        if (state.transitions().blocked(actor.serverLevel(), actor.blockPosition(), null)) {
            message(actor, "unavailable");
            return false;
        }
        var devices = id == null ? java.util.Set.<java.util.UUID>of() : java.util.Set.of(id);
        var change = state.controllers().bindingChange(actor.getUUID(), devices);
        state.controllers().requireRelease(actor);
        var coordinator = state.transitions();
        coordinator.beginBinding(actor.serverLevel(), java.util.List.of(actor.blockPosition()), change);
        boolean success = false;
        try {
            coordinator.bindingAffected(change);
            if (id != null) {
                state.bindingRevocations().revoke(id);
                state.bindings().rebuild();
            }
            state.controllers().invalidatePlayer(actor);
            success = true;
        } finally {
            coordinator.end("binding", success);
        }
        if (coordinator.blocked(actor.serverLevel(), actor.blockPosition(), null)
            || id != null && !state.bindingRevocations().revoked(id)) {
            return false;
        }
        message(actor, "unbound");
        return true;
    }

    private static void message(ServerPlayer actor, String key) {
        actor.displayClientMessage(
            Component.translatable("message.create_fieldwork.binding." + key),
            true
        );
    }
}
