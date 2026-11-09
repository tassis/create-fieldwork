package dev.tassis.createfieldwork.wireless.refresh;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

/** One exact-position Simulated receiver pass over the server-owned index. */
final class SimulatedReceiverRefreshPass {
    void run(ServerLevel level, Set<BlockPos> indexed, RefreshScope.Selection selection,
             TransitionCoordinator.View view, WirelessRefresh owner) {
        if (indexed == null) {
            return;
        }
        for (var pos : Set.copyOf(indexed)) {
            if (!level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof AbstractLinkedReceiverBlockEntity simulated)
                || simulated.isRemoved()) {
                indexed.remove(pos);
                continue;
            }

            var behaviour = simulated.getBehaviour(LinkBehaviour.TYPE);
            boolean matches;
            if (behaviour == null) {
                matches = selection.unrestrictedUnknown()
                    || selection.acceptsUnknown(ScopeResolver.at(level, pos, view));
            } else {
                var key = behaviour.getNetworkKey();
                matches = selection.includes(key)
                    && (!selection.filtered(key)
                    || selection.accepts(key, ScopeResolver.at(level, pos, view)));
            }
            if (matches) {
                owner.invoke(simulated, view, simulated::updateSignal);
            }
        }
    }
}
