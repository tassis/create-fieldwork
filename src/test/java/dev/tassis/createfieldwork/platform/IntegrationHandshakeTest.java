package dev.tassis.createfieldwork.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntegrationHandshakeTest {
    @Test void acceptsOnlyEqualActivationModes() {
        assertTrue(IntegrationHandshake.agrees(false, false));
        assertTrue(IntegrationHandshake.agrees(true, true));
        assertFalse(IntegrationHandshake.agrees(true, false));
        assertFalse(IntegrationHandshake.agrees(false, true));
    }
}
