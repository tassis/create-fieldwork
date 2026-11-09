package dev.tassis.createfieldwork.wireless.refresh;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** One Create network receiver pass; Simulated discovery is reported to the owner. */
final class CreateReceiverRefreshPass {
    void run(ServerLevel level, RefreshScope.Selection selection,
             TransitionCoordinator.View view, WirelessRefresh owner) {
        Set<IRedstoneLinkable> endpoints = Collections.newSetFromMap(new IdentityHashMap<>());
        var networks = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level);
        if (selection.all()) {
            for (var bucket : new ArrayList<>(networks.values())) {
                endpoints.addAll(new ArrayList<>(bucket));
            }
        } else {
            for (var key : selection.frequencies()) {
                var bucket = networks.get(key);
                if (bucket != null) {
                    endpoints.addAll(new ArrayList<>(bucket));
                }
            }
        }

        for (var endpoint : endpoints) {
            if (!endpoint.isAlive()) {
                continue;
            }
            if (endpoint.isListening()) {
                var key = endpoint.getNetworkKey();
                boolean selected = selection.includes(key)
                    && (!selection.filtered(key)
                    || selection.accepts(key, ScopeResolver.endpoint(level, endpoint, view)));
                if (selected) {
                    owner.invoke(
                        endpoint,
                        view,
                        () -> Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, endpoint)
                    );
                }
            }
            if (endpoint instanceof LinkBehaviour behaviour
                && behaviour.blockEntity instanceof AbstractLinkedReceiverBlockEntity simulated) {
                owner.remember(simulated);
            }
        }
    }
}
