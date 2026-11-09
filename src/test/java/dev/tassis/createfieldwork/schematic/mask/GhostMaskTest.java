package dev.tassis.createfieldwork.schematic.mask;

import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;

import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GhostMaskTest {
    @Test void additionsCoalesceButInvalidatedMatchesAreRestoredBeforeNextAddition() {
        var mask = new GhostMask();
        mask.update(Set.of(1L), 19); assertTrue(mask.positions().isEmpty());
        mask.update(Set.of(1L), 20); assertEquals(Set.of(1L), mask.positions());
        var stable = mask.positions();
        mask.update(Set.of(1L, 2L), 21); assertSame(stable, mask.positions());
        mask.update(Set.of(2L), 22); assertTrue(mask.positions().isEmpty(), "Invalidated match must reappear, not wait for coalescing");
        mask.update(Set.of(2L), 40); assertEquals(Set.of(2L), mask.positions());
        mask.clear(); assertTrue(mask.positions().isEmpty());
    }
    @Test void boundsLimitTheAddedNativeMeshRebuildAndMaskMemory() {
        assertTrue(GhostMask.supported(new Vec3i(32, 32, 32)));
        assertFalse(GhostMask.supported(new Vec3i(33, 32, 32)));
        assertFalse(GhostMask.supported(new Vec3i(0, 32, 32)));
        assertFalse(GhostMask.supported(new Vec3i(1 << 22, 1 << 22, 1 << 22)), "Do not let volume arithmetic wrap");
        assertFalse(GhostMask.supported(new Vec3i(1, 4097, 1)), "Do not alias packed local Y coordinates");
        assertEquals(Long.MIN_VALUE, GhostMask.sourcePosition(new BlockPos(0, 4096, 0), Mirror.NONE, new Vec3i(1, 4097, 1)));
        assertEquals(Long.MIN_VALUE, GhostMask.sourcePosition(new BlockPos(-1, 0, 0), Mirror.NONE, new Vec3i(3, 2, 4)));
        var positions = new java.util.HashSet<Long>();
        for (long i = 0; i <= ComparisonBudget.CACHE_POSITIONS; i++) positions.add(i);
        assertThrows(IllegalArgumentException.class, () -> new GhostMask().update(positions, 20));
    }
}
