package dev.tassis.createfieldwork.schematic.mask;

import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;

import java.util.Set;

/** Bounded source-coordinate mask. Removals restore unknown/mismatched ghosts on
 * the next tick; additions coalesce to avoid rebuilding a native mesh each frame. */
public final class GhostMask {
    public static final int MAX_REBUILD_VOLUME = 32_768;
    public static final int ADDITION_INTERVAL = 20;
    private Set<Long> committed = Set.of();
    private int lastAddition;

    public Set<Long> positions() { return committed; }
    public void clear() {
        committed = Set.of();
        lastAddition = 0;
    }
    public void update(Set<Long> confirmed, int tick) {
        if (committed.equals(confirmed)) return;
        if (!confirmed.containsAll(committed)) {
            // Only remove on this path: new matches must still obey coalescing.
            var retained = new java.util.HashSet<>(committed);
            retained.retainAll(confirmed);
            committed = Set.copyOf(retained);
        }
        if (tick - lastAddition >= ADDITION_INTERVAL) {
            if (confirmed.size() > ComparisonBudget.CACHE_POSITIONS) throw new IllegalArgumentException("Unbounded ghost mask");
            committed = Set.copyOf(confirmed);
            lastAddition = tick;
        }
    }
    public static boolean supported(Vec3i size) {
        return size.getX() > 0 && size.getY() > 0 && size.getZ() > 0 &&
            size.getX() <= ComparisonBudget.MAX_TEMPLATE_EXTENT && size.getY() <= ComparisonBudget.MAX_TEMPLATE_EXTENT &&
            size.getZ() <= ComparisonBudget.MAX_TEMPLATE_EXTENT &&
            (long) size.getX() * size.getY() * size.getZ() <= MAX_REBUILD_VOLUME;
    }
    /** Create builds its mirrored virtual levels with size-1 offsets. Undo that
     * variant here; world rotation/anchor were already handled by comparison. */
    public static long sourcePosition(BlockPos local, Mirror variant, Vec3i size) {
        // Match the native-template extent gate before packing: besides bounding
        // multiplication, this prevents BlockPos's 12-bit Y from aliasing cells.
        if (!supported(size) || local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 ||
            local.getX() >= size.getX() || local.getY() >= size.getY() || local.getZ() >= size.getZ()) return Long.MIN_VALUE;
        return BlockPos.asLong(variant == Mirror.FRONT_BACK ? size.getX() - 1 - local.getX() : local.getX(), local.getY(),
            variant == Mirror.LEFT_RIGHT ? size.getZ() - 1 - local.getZ() : local.getZ());
    }
}
