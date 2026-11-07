package dev.tassis.createfieldwork.test;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Non-shipping D02 candidate. Membership is derived, never supplied by a train oracle. */
public final class ProbeTrain {
    public static boolean enabled;
    public static long scopeEpoch;
    public static final UUID session = UUID.randomUUID();
    public static Prepared prepared;
    public static long scans, filteredCalls, pendingDeferrals;
    public static final List<Map<String, Object>> observations = new ArrayList<>();
    private record PrivateHint(Membership scope, boolean projectedCarrier, UUID craft, net.minecraft.resources.ResourceKey<Level> dimension) {}
    private static final Map<Carriage, PrivateHint> rememberedPrivate = new WeakHashMap<>();
    public enum Power { PRESENT, ABSENT, PENDING }
    public record Membership(String key, boolean pending) {
        public static Membership world() { return new Membership("WORLD", false); }
        public static Membership pending(String reason) { return new Membership("PENDING:" + reason, true); }
    }
    public record Prepared(Couple<Frequency> frequency, Power result) {}

    public static void invalidate() { if (enabled) scopeEpoch++; }

    private static Membership owner(SubLevel craft) {
        if (craft == null) return Membership.world();
        if (craft.isRemoved() || !ProbeScope.isReady(craft)) return Membership.pending("craft-not-ready");
        return ProbeScope.isPrivate(craft)
            ? new Membership(craft.getLevel().dimension().location() + "/" + craft.getUniqueId(), false) : Membership.world();
    }

    private static Membership point(ServerLevel level, Vec3 position) {
        if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z))
            return Membership.pending("missing-position");
        BlockPos pos = BlockPos.containing(position);
        if (ProbeLifecycle.quarantined(level, pos)) return Membership.pending("transfer-position");
        var craft = Sable.HELPER.getContaining(level, pos);
        if (craft == null && SubLevelContainer.getContainer(level).inBounds(pos)) return Membership.pending("unowned-reserved-plot");
        return owner(craft);
    }

    private static Membership combine(Membership a, Membership b) {
        if (a == null) return b;
        if (a.pending || b.pending) return Membership.pending("incomplete-membership");
        return a.key.equals(b.key) ? a : Membership.pending("mixed-membership");
    }

    public static Membership membership(ServerLevel caller, Train train) {
        if (train == null || train.invalid || train.carriages == null || train.carriages.isEmpty())
            return Membership.pending("invalid-train");
        Membership all = null;
        MinecraftServer server = caller.getServer();
        for (int index = 0; index < train.carriages.size(); index++) {
            Carriage carriage = train.carriages.get(index);
            if (carriage == null || carriage.train != train) return Membership.pending("carriage-binding");
            var dimensions = carriage.getPresentDimensions();
            if (dimensions.isEmpty()) return Membership.pending("no-dimensional-state");
            Membership car = null;
            for (var dimension : dimensions) {
                ServerLevel level = server.getLevel(dimension);
                if (level == null) return Membership.pending("missing-level");
                var state = carriage.getDimensionalIfPresent(dimension); // observational; never creates entries
                if (state == null || state.rotationAnchors == null) return Membership.pending("missing-anchors");
                Membership references = combine(point(level, state.positionAnchor), point(level, state.rotationAnchors.getFirst()));
                references = combine(references, point(level, state.rotationAnchors.getSecond()));
                if (references.pending) return references;
                var entity = state.entity == null ? null : state.entity.get();
                Membership current = references;
                if (entity != null) {
                    var visible = level.getEntity(entity.getUUID());
                    // Vanilla's UUID lookup is visibility-filtered: a valid
                    // carriage in an inactive chunk can be absent there. The
                    // real carriage binding + complete anchors remain required.
                    if (!entity.isAlive() || entity.level() != level || (visible != null && visible != entity)
                        || entity.getCarriage() != carriage || !train.id.equals(entity.trainId) || entity.carriageIndex != index)
                        return Membership.pending("untrusted-live-binding");
                    var direct = Sable.HELPER.getContaining(level, entity.blockPosition());
                    var tracking = Sable.HELPER.getTrackingSubLevel(entity);
                    if (tracking != null && (tracking.isRemoved() || tracking.getLevel() != level))
                        return Membership.pending("stale-tracking");
                    current = owner(direct != null ? direct : tracking);
                    // A tracked carrier may use projected world anchors, like players;
                    // a *stored plot* anchor must not contradict the entity's owner.
                    if (!references.key.equals("WORLD") && !references.key.equals(current.key))
                        return Membership.pending("entity-anchor-conflict");
                    var actualOwner = direct != null ? direct : tracking;
                    if (!current.pending && !current.key.equals("WORLD")) {
                        rememberedPrivate.put(carriage, new PrivateHint(current, references.key.equals("WORLD"), actualOwner.getUniqueId(), level.dimension()));
                    } else {
                        var hint = rememberedPrivate.get(carriage);
                        if (hint != null && references.key.equals("WORLD") && hint.projectedCarrier && !constraintRemoved(server, hint))
                            return Membership.pending("lost-carrier-association");
                        // Coherent server rail references can confirm a stored-plot
                        // transition, and READY(empty) must release old private state.
                        rememberedPrivate.remove(carriage);
                    }
                } else if (references.key.equals("WORLD")) {
                    var hint = rememberedPrivate.get(carriage);
                    if (hint != null && hint.projectedCarrier && !constraintRemoved(server, hint))
                        return Membership.pending("offline-carrier-association");
                    rememberedPrivate.remove(carriage);
                }
                car = combine(car, current);
            }
            all = combine(all, car);
        }
        return all == null ? Membership.pending("empty-membership") : all;
    }

    private static boolean constraintRemoved(MinecraftServer server, PrivateHint hint) {
        var level = server.getLevel(hint.dimension);
        if (level == null) return false;
        var craft = SubLevelContainer.getContainer(level).getSubLevel(hint.craft);
        return craft != null && ProbeScope.isReady(craft) && !ProbeScope.isPrivate(craft);
    }

    public static Power query(ServerLevel caller, Membership consumer, Couple<Frequency> frequency) {
        scans++;
        if (consumer.pending) return Power.PENDING;
        boolean pending = ProbeLifecycle.depth > 0; // unknown not-yet-registered sources during active transition
        for (ServerLevel level : caller.getServer().getAllLevels()) {
            var bucket = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(frequency);
            if (bucket == null) continue;
            for (IRedstoneLinkable source : List.copyOf(bucket)) {
                if (source.getTransmittedStrength() <= 0) continue;
                Membership scope;
                if (ProbeController.unavailable(level, source)) { pending = true; continue; }
                var sourceOwner = ProbeController.owner(level, source);
                scope = ProbeLifecycle.quarantined(level, source.getLocation()) ? Membership.pending("source-transfer") : owner(sourceOwner);
                if (sourceOwner == null && SubLevelContainer.getContainer(level).inBounds(source.getLocation()))
                    scope = Membership.pending("source-unowned-plot");
                if (scope.pending) { if (source.isAlive()) pending = true; continue; }
                if (!scope.key.equals(consumer.key)) continue;
                // Preserve upstream WORLD cached-strength semantics; restricted consumers
                // must not accept a dead cached source merely through plot reuse.
                if (!consumer.key.equals("WORLD") && !source.isAlive()) continue;
                return Power.PRESENT;
            }
        }
        return pending ? Power.PENDING : Power.ABSENT;
    }
    private ProbeTrain() {}
}
