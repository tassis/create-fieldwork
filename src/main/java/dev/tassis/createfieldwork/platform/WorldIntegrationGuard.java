package dev.tassis.createfieldwork.platform;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Records integration use and warns, without blocking, when that bundle is later absent. */
public final class WorldIntegrationGuard {
    private static final String MARKER = "create_fieldwork_integration_required";
    private static final Logger LOGGER = LogUtils.getLogger();

    private WorldIntegrationGuard() {}

    public static void aboutToStart(ServerAboutToStartEvent event) {
        var dataDirectory = event.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
        verify(dataDirectory, AeronauticsIntegration.active());
    }

    static void verify(java.nio.file.Path dataDirectory, boolean integrationActive) {
        var marker = dataDirectory.resolve(MARKER);
        if (!integrationActive) {
            if (requiresIntegration(dataDirectory)) {
                LOGGER.warn("This world previously used the Create: Fieldwork Aeronautics integration bundle, but the bundle is absent. Continuing at the player's request; back up the world because missing optional-mod content may be lost when saved.");
            }
            return;
        }

        if (Files.isRegularFile(marker)) return;
        try {
            Files.createDirectories(dataDirectory);
            var temporary = dataDirectory.resolve(MARKER + ".tmp");
            Files.writeString(temporary, "schema=1\n", StandardCharsets.UTF_8);
            Files.move(temporary, marker, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot establish the Fieldwork integration world marker", failure);
        }
    }

    static boolean requiresIntegration(java.nio.file.Path dataDirectory) {
        return Files.isRegularFile(dataDirectory.resolve(MARKER));
    }
}
