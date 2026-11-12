package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded whole-cell selection, independent of block collision shapes and rendering. */
public final class ProjectionRaycast {
    private ProjectionRaycast() {}

    public static BlockPos nearest(Vec3 start, Vec3 end, Iterable<BlockPos> candidates, double obstructionDistanceSquared) {
        BlockPos selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        int inspected = 0;
        for (var pos : candidates) {
            if (inspected++ >= ComparisonBudget.CACHE_POSITIONS) break;
            var box = new AABB(pos);
            var hit = box.contains(start) ? java.util.Optional.of(start) : box.clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            // Real geometry wins ties: an occupied cell is not a visible ghost.
            if (distance + 1.0e-7 >= obstructionDistanceSquared || distance >= nearest) continue;
            selected = pos.immutable();
            nearest = distance;
        }
        return selected;
    }
}
