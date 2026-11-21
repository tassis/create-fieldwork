package dev.tassis.createfieldwork.schematic.table;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ScribingTableLayoutTest {
    @Test
    void thirtyNineWellsFitWithoutOverlapAndKeepSemanticIndices() {
        var points = new ArrayList<ScribingTableLayout.Point>();
        for (int index = 0; index < 3; index++) points.add(ScribingTableLayout.tableSlot(index));
        for (int index = 0; index < 36; index++) points.add(ScribingTableLayout.playerSlot(index));
        assertEquals(39, points.size());
        assertEquals(66, points.get(0).x());
        assertEquals(64, points.get(0).y());
        assertEquals(176, points.get(1).x());
        assertEquals(176, points.get(2).x());
        assertEquals(44, points.get(1).y());
        assertEquals(94, points.get(2).y());
        for (int index = 0; index < points.size(); index++) {
            var well = points.get(index).well();
            assertTrue(well.x() >= 0 && well.y() >= 0 && well.right() <= ScribingTableLayout.WIDTH
                && well.bottom() <= ScribingTableLayout.HEIGHT);
            for (int other = index + 1; other < points.size(); other++) {
                assertFalse(well.intersects(points.get(other).well()));
            }
        }
    }

    @Test
    void printActionDoesNotCoverWorkOrInventorySlots() {
        var button = ScribingTableLayout.PRINT;
        for (int index = 0; index < 3; index++) {
            assertFalse(button.intersects(ScribingTableLayout.tableSlot(index).well()));
        }
        for (int index = 0; index < 36; index++) {
            assertFalse(button.intersects(ScribingTableLayout.playerSlot(index).well()));
        }
    }

    @Test
    void standardInventorySpacingAndBoundsStayStable() {
        for (int row = 0; row < 4; row++) {
            for (int column = 1; column < 9; column++) {
                assertEquals(18, ScribingTableLayout.playerSlot(row * 9 + column).x()
                    - ScribingTableLayout.playerSlot(row * 9 + column - 1).x());
            }
        }
        assertEquals(172, ScribingTableLayout.playerSlot(0).y());
        assertEquals(230, ScribingTableLayout.playerSlot(27).y());
        assertThrows(IndexOutOfBoundsException.class, () -> ScribingTableLayout.tableSlot(3));
        assertThrows(IndexOutOfBoundsException.class, () -> ScribingTableLayout.playerSlot(36));
    }

    @Test
    void captionHasExplicitClearanceAndProgressIsBetweenVerticalInputAndOutput() {
        var frame = ScribingTableLayout.SCHEMATIC_FRAME;
        var label = ScribingTableLayout.SCHEMATIC_LABEL;
        var paper = ScribingTableLayout.BLUEPRINT_PAPER;
        assertTrue(label.y() - frame.bottom() >= 4);
        assertTrue(paper.bottom() - label.bottom() >= 4);
        var progress = ScribingTableLayout.PROGRESS;
        assertEquals(ScribingTableLayout.MEDIUM.x(), ScribingTableLayout.OUTPUT.x());
        assertTrue(progress.y() >= ScribingTableLayout.MEDIUM.well().bottom() + 3);
        assertTrue(ScribingTableLayout.OUTPUT.well().y() >= progress.bottom() + 3);
        assertEquals(ScribingTableLayout.MEDIUM.x() + 8, progress.x() + progress.width() / 2);
        assertTrue(ScribingTableLayout.HEIGHT <= ScribingTableLayout.TEXTURE_SIZE);
    }
}
