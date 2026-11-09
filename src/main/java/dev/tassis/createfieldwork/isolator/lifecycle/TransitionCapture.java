package dev.tassis.createfieldwork.isolator.lifecycle;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.isolator.state.IsolationRegistry;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Captures immutable pre-mutation footprints before barriers or callbacks run. */
final class TransitionCapture {
    record Footprint(Set<ResourceKey<Level>> levels, Set<CraftKey> crafts,
                     Set<TransitionCoordinator.Position> positions, RefreshScope refreshScope) {
        Footprint {
            levels = Set.copyOf(levels);
            crafts = Set.copyOf(crafts);
            positions = Set.copyOf(positions);
        }
    }

    private final FieldworkServerState state;
    private final TransitionCoordinator coordinator;

    TransitionCapture(FieldworkServerState state, TransitionCoordinator coordinator) {
        this.state = state;
        this.coordinator = coordinator;
    }

    void capture(TransitionCoordinator.Transaction transaction, ServerLevel level,
                 Collection<BlockPos> input) {
        Footprint footprint = footprint(transaction, level, input);
        transaction.levels.addAll(footprint.levels());
        transaction.refreshScope = transaction.refreshScope.union(footprint.refreshScope());
    }

    void binding(TransitionCoordinator.Transaction transaction,
                 ControllerSources.BindingChange change) {
        transaction.devices.addAll(change.devices());
        transaction.refreshScope = transaction.refreshScope.union(change.networks());
        for (var key : change.crafts()) {
            var level = state.server().getLevel(key.dimension());
            if (level != null
                && SubLevelContainer.getContainer(level).getSubLevel(key.id()) != null
                && state.registry().snapshot(key).status() == IsolationRegistry.Status.READY) {
                coordinator.touch(transaction, key);
            }
        }
    }

    void controllers(TransitionCoordinator.Transaction transaction) {
        var selection = state.controllers().select(
            transaction.devices,
            transaction.crafts,
            transaction.positions
        );
        transaction.controllerEndpoints.addAll(selection.endpoints());
        transaction.refreshScope = transaction.refreshScope.union(selection.networks());
    }

    RefreshScope stageControllers(TransitionCoordinator.Transaction transaction,
                                  TransitionCoordinator.View view) {
        var selection = new ControllerSources.Selection(
            new ArrayList<>(transaction.controllerEndpoints),
            RefreshScope.none()
        );
        return state.controllers().stage(selection, view);
    }

    private Footprint footprint(TransitionCoordinator.Transaction transaction,
                                ServerLevel level, Collection<BlockPos> input) {
        Set<ResourceKey<Level>> levels = new HashSet<>(transaction.levels);
        levels.add(level.dimension());
        Set<CraftKey> crafts = new HashSet<>(transaction.crafts);
        Set<TransitionCoordinator.Position> positions = new HashSet<>(transaction.positions);
        for (var pos : input) {
            positions.add(new TransitionCoordinator.Position(level.dimension(), pos));
            var owner = Sable.HELPER.getContaining(level, pos);
            if (owner != null) {
                crafts.add(IsolationRuntime.key(owner));
            }
        }

        RefreshScope refreshScope = state.refresh().affected(levels, crafts, positions);
        if (transaction.unknownSources) {
            for (var dimension : levels) {
                refreshScope = refreshScope.union(RefreshScope.level(dimension));
            }
        }
        return new Footprint(levels, crafts, positions, refreshScope);
    }

    static List<BlockPos> copy(Iterable<BlockPos> input) {
        var result = new ArrayList<BlockPos>();
        for (var pos : input) {
            result.add(pos.immutable());
        }
        return result;
    }
}
