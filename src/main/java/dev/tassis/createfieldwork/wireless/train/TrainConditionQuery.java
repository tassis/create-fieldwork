package dev.tassis.createfieldwork.wireless.train;

import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import net.createmod.catnip.data.Couple;

import java.util.List;

/** V1 train condition is deliberately a public WORLD observer, irrespective of Train location. */
public final class TrainConditionQuery {
    public enum Power { PRESENT, ABSENT, PENDING }
    public record Prepared(Couple<Frequency> frequency, Power power) {}
    private final FieldworkServerState state;
    private Prepared prepared;

    public TrainConditionQuery(FieldworkServerState state) { this.state = state; }

    public Power query(Couple<Frequency> frequency) {
        state.checkThread();
        // Includes not-yet-registered sources during a content transaction. Do not
        // infer absence from an empty bucket before reconstruction has completed.
        boolean pending = state.transitions().unknownPublicationPending() || state.publications().pendingWorld();
        for (var level : state.server().getAllLevels()) {
            var bucket = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(frequency);
            if (bucket == null) continue;
            for (var source : List.copyOf(bucket)) {
                if (source.getTransmittedStrength() <= 0) continue;
                var scope = ScopeResolver.endpoint(level, source, null);
                if (scope == Scope.WORLD) return Power.PRESENT;
                if (scope == Scope.PENDING && source.isAlive()) pending = true;
            }
        }
        return pending ? Power.PENDING : Power.ABSENT;
    }

    public Prepared prepared() { state.checkThread(); return prepared; }
    public void prepared(Prepared value) { state.checkThread(); prepared = value; }
}
