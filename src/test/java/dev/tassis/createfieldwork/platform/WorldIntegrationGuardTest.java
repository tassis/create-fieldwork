package dev.tassis.createfieldwork.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class WorldIntegrationGuardTest {
    @TempDir Path temporary;

    @Test void baseWorldDoesNotAcquireMarker() {
        var data = temporary.resolve("data");
        WorldIntegrationGuard.verify(data, false);
        assertFalse(Files.exists(data));
    }

    @Test void integratedWorldIsMarkedAtomicallyAndReloads() throws Exception {
        var data = temporary.resolve("data");
        WorldIntegrationGuard.verify(data, true);
        assertEquals("schema=1\n", Files.readString(data.resolve("create_fieldwork_integration_required")));
        assertDoesNotThrow(() -> WorldIntegrationGuard.verify(data, true));
        assertFalse(Files.exists(data.resolve("create_fieldwork_integration_required.tmp")));
    }

    @Test void markedWorldAllowsBaseStartup() {
        var data = temporary.resolve("data");
        WorldIntegrationGuard.verify(data, true);
        assertDoesNotThrow(() -> WorldIntegrationGuard.verify(data, false));
    }
}
