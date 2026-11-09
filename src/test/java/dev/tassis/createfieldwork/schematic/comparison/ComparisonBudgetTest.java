package dev.tassis.createfieldwork.schematic.comparison;

import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ComparisonBudgetTest {
    @Test void discoveryAndRefreshStayInsideWorldReadBudget() {
        for (int dirtyCount : new int[]{0, 63, 128, 4096}) {
            var cycle = new ComparisonCycle(); var dirty = new ArrayDeque<Integer>();
            for (int i = 0; i < dirtyCount; i++) dirty.add(i);
            var refresh = new AtomicInteger(); var discovery = new AtomicInteger();
            cycle.run(dirty, 4096, 1_000_000, index -> {
                assertTrue(index >= 0 && index < 4096); refresh.incrementAndGet(); assertTrue(cycle.read());
            }, index -> { discovery.incrementAndGet(); assertTrue(cycle.read()); });
            assertEquals(128, refresh.get()); assertEquals(128, discovery.get());
            assertEquals(256, cycle.reads()); assertFalse(cycle.read());
            assertEquals(Math.max(0, dirtyCount - 128), dirty.size());
            int submitted = 0;
            for (int i = 0; i < 4096; i++) if (cycle.outline()) submitted++;
            assertEquals(128, submitted); assertEquals(128, cycle.outlines());
            assertEquals(256, cycle.reads(), "presenting cached outlines consumes no block reads");
            cycle.run(dirty, 4096, 1_000_000, index -> {}, index -> {});
            assertEquals(0, cycle.reads()); assertEquals(0, cycle.outlines());
        }
    }
    @Test void cursorEventuallyCoversLargeTemplateWithoutPerTickFullScan() {
        var cursor = new ComparisonBudget(); var visited = new HashSet<Integer>();
        for (int i = 0; i < 32; i++) assertEquals(128, cursor.take(4096, 128, index -> assertTrue(visited.add(index))));
        assertEquals(4096, visited.size());
        cursor.take(4096, 1, index -> assertEquals(0, index));
    }
    @Test void emptyAndChangingSizesNeverReadOutsideInput() {
        var cursor = new ComparisonBudget();
        assertEquals(0, cursor.take(0, 128, ignored -> fail()));
        cursor.take(1000, 128, ignored -> {});
        assertEquals(3, cursor.take(3, 128, index -> assertTrue(index >= 0 && index < 3)));
        assertThrows(IllegalArgumentException.class, () -> cursor.take(-1, 128, ignored -> {}));
    }
    @Test void selectionUsesActualOffhandInventoryIndex() {
        assertEquals(3, BlueprintSelection.slot(true, 3, true));
        assertEquals(40, BlueprintSelection.slot(false, 3, true));
        assertEquals(-1, BlueprintSelection.slot(false, 3, false));
    }
}
