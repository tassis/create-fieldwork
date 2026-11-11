package dev.tassis.createfieldwork.integration.schematic;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;
import dev.tassis.createfieldwork.schematic.comparison.WorldTargetSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Sable-aware client exclusion snapshot installed only for the complete integration bundle. */
public final class SableWorldTargetSupport {
    private record Snapshot(Level level, SubLevelContainer container, List<AABB> bounds, boolean ready)
        implements WorldTargetSupport.Snapshot {
        private Snapshot { bounds = List.copyOf(bounds); }

        @Override
        public boolean comparable(BlockPos pos) {
            if (!ready || !level.hasChunkAt(pos) || container.inBounds(pos)) return false;
            for (var box : bounds) {
                if (box.intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1d, pos.getY() + 1d, pos.getZ() + 1d)) return false;
            }
            return true;
        }
    }

    private SableWorldTargetSupport() {}

    public static WorldTargetSupport.Snapshot capture(Level level) {
        if (level == null) return new Snapshot(null, null, List.of(), false);
        try {
            var container = SubLevelContainer.getContainer(level);
            var crafts = container.getAllSubLevels();
            if (crafts.size() > ComparisonBudget.MAX_CRAFT_BOUNDS) return new Snapshot(level, container, List.of(), false);
            var boxes = new ArrayList<AABB>();
            for (var craft : crafts) {
                if (craft.isRemoved()) continue;
                var local = craft.getPlot().getBoundingBox();
                if (local == null || local.maxX() < local.minX() || local.maxY() < local.minY() || local.maxZ() < local.minZ()) {
                    return new Snapshot(level, container, List.of(), false);
                }
                var pose = craft.logicalPose();
                var previous = craft.lastPose();
                boolean stationary = pose.position().equals(previous.position()) && pose.orientation().equals(previous.orientation())
                    && pose.scale().equals(previous.scale()) && pose.rotationPoint().equals(previous.rotationPoint());
                AABB box;
                if (stationary) {
                    var projected = new BoundingBox3d(local.minX(), local.minY(), local.minZ(), local.maxX() + 1d,
                        local.maxY() + 1d, local.maxZ() + 1d).transform(pose);
                    box = new AABB(projected.minX(), projected.minY(), projected.minZ(), projected.maxX(), projected.maxY(), projected.maxZ());
                } else {
                    double dx = 0, dy = 0, dz = 0, scale = 0;
                    for (var p : List.of(pose, previous)) {
                        dx = Math.max(dx, Math.max(Math.abs(local.minX() - p.rotationPoint().x()), Math.abs(local.maxX() + 1d - p.rotationPoint().x())));
                        dy = Math.max(dy, Math.max(Math.abs(local.minY() - p.rotationPoint().y()), Math.abs(local.maxY() + 1d - p.rotationPoint().y())));
                        dz = Math.max(dz, Math.max(Math.abs(local.minZ() - p.rotationPoint().z()), Math.abs(local.maxZ() + 1d - p.rotationPoint().z())));
                        scale = Math.max(scale, Math.max(Math.abs(p.scale().x()), Math.max(Math.abs(p.scale().y()), Math.abs(p.scale().z()))));
                    }
                    double radius = Math.sqrt(dx * dx + dy * dy + dz * dz) * scale;
                    var a = pose.position();
                    var b = previous.position();
                    box = new AABB(Math.min(a.x(), b.x()) - radius, Math.min(a.y(), b.y()) - radius, Math.min(a.z(), b.z()) - radius,
                        Math.max(a.x(), b.x()) + radius, Math.max(a.y(), b.y()) + radius, Math.max(a.z(), b.z()) + radius);
                }
                if (!finite(box)) return new Snapshot(level, container, List.of(), false);
                boxes.add(box);
            }
            return new Snapshot(level, container, boxes, true);
        } catch (RuntimeException unavailable) {
            return new Snapshot(level, null, List.of(), false);
        }
    }

    private static boolean finite(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
            && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }
}
