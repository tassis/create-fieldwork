package dev.tassis.createfieldwork.schematic.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScribingPlacementPolicyTest {
    @Test
    void fourNativeModesRespectAirAndBothExistingHalves() {
        for (int bits = 0; bits < 16; bits++) {
            boolean air = (bits & 1) != 0;
            boolean normal = (bits & 2) != 0;
            boolean current = (bits & 4) != 0;
            boolean other = (bits & 8) != 0;
            assertEquals(!air && !current && !other,
                ScribingPlacementPolicy.replacementAllowed(0, air, normal, current, other));
            assertEquals(!air && (normal || !current && !other),
                ScribingPlacementPolicy.replacementAllowed(1, air, normal, current, other));
            assertEquals(!air, ScribingPlacementPolicy.replacementAllowed(2, air, normal, current, other));
            assertTrue(ScribingPlacementPolicy.replacementAllowed(3, air, normal, current, other));
        }
    }

    @Test
    void unknownModesFailClosedAndUnknownStatusDoesNotLookSuccessful() {
        assertFalse(ScribingPlacementPolicy.replacementAllowed(-1, false, true, false, false));
        assertFalse(ScribingPlacementPolicy.replacementAllowed(4, false, true, false, false));
        assertEquals(ScribingChecklist.Status.FAILED, ScribingChecklist.Status.byId(-1));
        assertEquals(ScribingChecklist.Status.FAILED, ScribingChecklist.Status.byId(999));
    }
}
