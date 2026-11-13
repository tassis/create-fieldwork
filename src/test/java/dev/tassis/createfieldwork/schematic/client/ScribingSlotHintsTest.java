package dev.tassis.createfieldwork.schematic.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScribingSlotHintsTest {
    @Test
    void onlyEmptyActiveInputsHaveVisualHints() {
        for (int index = 0; index < 39; index++) {
            assertEquals(index < 2, ScribingSlotHints.showsHint(index, true, false));
            assertFalse(ScribingSlotHints.showsHint(index, false, false));
            assertFalse(ScribingSlotHints.showsHint(index, true, true));
        }
        assertFalse(ScribingSlotHints.showsHint(-1, true, false));
    }
}
