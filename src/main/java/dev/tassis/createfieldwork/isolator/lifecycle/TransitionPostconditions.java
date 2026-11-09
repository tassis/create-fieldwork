package dev.tassis.createfieldwork.isolator.lifecycle;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.isolator.state.IsolatorBlocks;
import dev.tassis.createfieldwork.platform.FieldworkServerState;

/** Read-only marker-ledger and named-repair precondition checks. */
final class TransitionPostconditions {
    private final FieldworkServerState state;

    TransitionPostconditions(FieldworkServerState state) {
        this.state = state;
    }

    boolean validLedger(TransitionCoordinator.Transaction transaction) {
        for (var intent : transaction.markerIntent.entrySet()) {
            var level = state.server().getLevel(intent.getKey().dimension());
            if (level == null
                || IsolatorBlocks.marker(level.getBlockState(intent.getKey().pos())) != intent.getValue()) {
                return false;
            }
        }
        for (var move : transaction.ledger) {
            var destination = state.server().getLevel(move.to().dimension());
            var source = state.server().getLevel(move.from().dimension());
            if (destination == null
                || source == null
                || !IsolatorBlocks.marker(destination.getBlockState(move.to().pos()))
                || !move.from().equals(move.to())
                && IsolatorBlocks.marker(source.getBlockState(move.from().pos()))) {
                return false;
            }
        }
        return true;
    }

    boolean repairable(TransitionCoordinator.Transaction transaction) {
        if (!validLedger(transaction)) {
            return false;
        }
        if (transaction.mutated && transaction.markerIntent.isEmpty()) {
            return false;
        }
        if (transaction.mutated) {
            return true;
        }
        if (!transaction.before.keySet().containsAll(transaction.crafts)) {
            return false;
        }
        for (var entry : transaction.before.entrySet()) {
            var level = state.server().getLevel(entry.getKey().dimension());
            var craft = level == null
                ? null
                : SubLevelContainer.getContainer(level).getSubLevel(entry.getKey().id());
            if (!(craft instanceof ServerSubLevel loaded)
                || !IsolationRuntime.markers(loaded).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
