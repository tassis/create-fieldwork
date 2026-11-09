package dev.tassis.createfieldwork.controller.routing;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Objects;

/** Read-only provenance and publication-scope decisions for controller endpoints. */
final class ControllerSourceValidation {
    private final FieldworkServerState state;

    ControllerSourceValidation(FieldworkServerState state) {
        this.state = state;
    }

    static boolean sameProvenance(ControllerSources.Source before, ControllerSources.Source after) {
        if (before == null || after == null) {
            return before == after;
        }
        return before.player().equals(after.player())
            && before.session().equals(after.session())
            && before.dimension().equals(after.dimension())
            && Objects.equals(before.lectern(), after.lectern())
            && Objects.equals(before.device(), after.device())
            && before.bindingRevision() == after.bindingRevision()
            && before.program().equals(after.program())
            && before.hand() == after.hand()
            && (before.lectern() == null || before.lecternEntity().get() == after.lecternEntity().get());
    }

    Scope scope(ServerLevel level, ControllerEndpoint endpoint, TransitionCoordinator.View view) {
        var source = endpoint.fieldwork$source();
        if (source == null) {
            return Scope.PENDING;
        }

        boolean managed = source.device() != null;
        boolean ordinaryHand = !managed && source.lectern() == null;
        boolean historyRestricted = endpoint.fieldwork$published() instanceof Scope.Private;
        // Ordinary handheld inputs are public sources even while their operator is
        // aboard a private craft. Only a physical lectern or a managed binding may
        // select a private scope.
        boolean restricted = managed || (!ordinaryHand && historyRestricted);
        var actor = state.playerSessions().actor(source.player(), source.session());

        if (source.lectern() != null
            && level.getBlockEntity(source.lectern()) instanceof LecternControllerBlockEntity lectern
            && lectern == source.lecternEntity().get()
            && (lectern.isRemoved() || !lectern.hasUser())) {
            return Scope.PENDING;
        }

        boolean valid = actor != null
            && !actor.isRemoved()
            && !actor.isDeadOrDying()
            && actor.level() == level
            && actor.connection != null
            && actor.connection.isAcceptingMessages()
            && source.dimension().equals(level.dimension());
        if (!valid) {
            // Preserve upstream WORLD timeout semantics for unrestricted entries.
            return restricted ? Scope.PENDING : Scope.WORLD;
        }

        if (pendingReset(level, actor, source)) {
            return Scope.PENDING;
        }
        if (!managed && becameManaged(level, actor, source)) {
            return Scope.PENDING;
        }
        if (managed) {
            return managedScope(level, actor, source, view);
        }
        if (ordinaryHand) {
            return Scope.WORLD;
        }

        var block = level.getBlockEntity(source.lectern());
        if (!(block instanceof LecternControllerBlockEntity lectern)
            || block != source.lecternEntity().get()) {
            return restricted ? Scope.PENDING : Scope.WORLD;
        }
        // A known native stop is authoritative. Invalid actor compatibility above
        // remains WORLD-only until Create's native timeout.
        if (block.isRemoved() || !lectern.isUsedBy(actor)) {
            return Scope.PENDING;
        }
        return ScopeResolver.at(level, source.lectern(), view);
    }

    private static boolean pendingReset(ServerLevel level, net.minecraft.server.level.ServerPlayer actor,
                                        ControllerSources.Source source) {
        if (source.lectern() == null) {
            var selected = source.hand() == null ? ItemStack.EMPTY : actor.getItemInHand(source.hand());
            return ControllerReset.pending(selected);
        }
        return level.getBlockEntity(source.lectern()) instanceof LecternControllerBlockEntity lectern
            && ControllerReset.pending(lectern.getController());
    }

    private static boolean becameManaged(ServerLevel level, net.minecraft.server.level.ServerPlayer actor,
                                         ControllerSources.Source source) {
        if (source.lectern() == null) {
            var selected = source.hand() == null ? ItemStack.EMPTY : actor.getItemInHand(source.hand());
            return ControllerDevice.read(selected) != null;
        }
        return level.getBlockEntity(source.lectern()) instanceof LecternControllerBlockEntity lectern
            && ControllerDevice.read(lectern.getController()) != null;
    }

    private Scope managedScope(ServerLevel level, net.minecraft.server.level.ServerPlayer actor,
                               ControllerSources.Source source, TransitionCoordinator.View view) {
        if (actor.isSpectator()) {
            return Scope.PENDING;
        }

        ItemStack actual;
        if (source.lectern() == null) {
            if (source.hand() == null
                || source.hand() == InteractionHand.OFF_HAND
                && actor.getMainHandItem().is(AllItems.LINKED_CONTROLLER.get())) {
                return Scope.PENDING;
            }
            actual = actor.getItemInHand(source.hand());
        } else {
            var entity = level.getBlockEntity(source.lectern());
            if (!(entity instanceof LecternControllerBlockEntity lectern)
                || entity != source.lecternEntity().get()
                || entity.isRemoved()
                || !lectern.isUsedBy(actor)) {
                return Scope.PENDING;
            }
            actual = lectern.getController();
        }

        boolean sameDevice = source.device().equals(ControllerDevice.read(actual));
        ItemContainerContents actualProgram = actual.getOrDefault(
            AllDataComponents.LINKED_CONTROLLER_ITEMS,
            ItemContainerContents.EMPTY
        );
        if (!actual.is(AllItems.LINKED_CONTROLLER.get())
            || !sameDevice
            || !source.program().equals(actualProgram)) {
            return Scope.PENDING;
        }
        return state.bindings().scope(source.device(), level, view);
    }
}
