package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectionRaycastTest {
    private static final Vec3 START = new Vec3(0.5, 0.5, 0.5);
    private static final Vec3 END = new Vec3(10.5, 0.5, 0.5);

    @Test void selectsNearestCellRegardlessOfCandidateOrder() {
        assertEquals(new BlockPos(2, 0, 0), ProjectionRaycast.nearest(START, END,
            List.of(new BlockPos(5, 0, 0), new BlockPos(2, 0, 0)), Double.POSITIVE_INFINITY));
    }

    @Test void ignoresMissesAndCellsBeyondTheRay() {
        assertNull(ProjectionRaycast.nearest(START, END,
            List.of(new BlockPos(2, 1, 0), new BlockPos(12, 0, 0), new BlockPos(-2, 0, 0)), Double.POSITIVE_INFINITY));
    }

    @Test void realGeometryOccludesAndWinsEqualDistance() {
        var candidates = List.of(new BlockPos(2, 0, 0));
        assertNull(ProjectionRaycast.nearest(START, END, candidates, 1));
        assertNull(ProjectionRaycast.nearest(START, END, candidates, 2.25));
        assertEquals(candidates.getFirst(), ProjectionRaycast.nearest(START, END, candidates, 4));
    }

    @Test void cameraInsideGhostSelectsThatCell() {
        assertEquals(BlockPos.ZERO, ProjectionRaycast.nearest(START, END,
            List.of(new BlockPos(2, 0, 0), BlockPos.ZERO), Double.POSITIVE_INFINITY));
        assertNull(ProjectionRaycast.nearest(START, END, List.of(BlockPos.ZERO), 0));
    }

    @Test void negativeCoordinatesAndReverseRaysWork() {
        assertEquals(new BlockPos(-3, 0, 0), ProjectionRaycast.nearest(START, new Vec3(-10, 0.5, 0.5),
            List.of(new BlockPos(-7, 0, 0), new BlockPos(-3, 0, 0)), Double.POSITIVE_INFINITY));
    }

    @Test void capsCandidateWorkEvenIfCallerSuppliesTooMany() {
        var candidates = new ArrayList<BlockPos>();
        for (int i = 0; i < ComparisonBudget.CACHE_POSITIONS; i++) candidates.add(new BlockPos(i, 5, 0));
        candidates.add(new BlockPos(2, 0, 0));
        assertNull(ProjectionRaycast.nearest(START, END, candidates, Double.POSITIVE_INFINITY));
    }
}
