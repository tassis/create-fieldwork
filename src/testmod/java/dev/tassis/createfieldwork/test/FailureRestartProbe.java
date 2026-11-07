package dev.tassis.createfieldwork.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Expected-unsafe negative control: no recovery journal is installed or loaded. */
final class FailureRestartProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private final boolean seed;
    private ServerSubLevel craft;
    private BlockPos stored;
    private JsonObject identity;
    FailureRestartProbe(ServerLevel level, ScopeProbeAssertions checks, String phase) {
        this.level = level; this.checks = checks; seed = phase.equals("seed-failure");
    }
    boolean tick(int tick) throws Exception {
        if (tick == 20 && seed) {
            var fixture = RecoveryProbe.failed(level, checks, new BlockPos(0, 120, 0), 1, false);
            craft = fixture.craft(); stored = fixture.stored();
            identity = new JsonObject();
            identity.addProperty("uuid", craft.getUniqueId().toString());
            identity.addProperty("stored", stored.asLong());
            identity.addProperty("jvmStartMillis", ManagementFactory.getRuntimeMXBean().getStartTime());
            checks.check("failed destination and cleared source lose marker identity", ProbeLifecycle.markers(craft).isEmpty()
                && level.getBlockState(new BlockPos(1, 120, 0)).isAir());
        } else if (tick == 20) {
            identity = JsonParser.parseString(Files.readString(Path.of("failure-restart-fixture.json"))).getAsJsonObject();
            stored = BlockPos.of(identity.get("stored").getAsLong());
            checks.check("lost-marker restart is an independent JVM", identity.get("jvmStartMillis").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
        }
        if (!seed && craft == null) {
            craft = (ServerSubLevel) SubLevelContainer.getContainer(level).getSubLevel(UUID.fromString(identity.get("uuid").getAsString()));
            if (craft == null && tick < 100) return false;
            checks.check("failed-transfer craft auto-loads from real save", craft != null);
        }
        if (tick < 60) return false;
        var source = checks.initialized(stored);
        AllBlocks.REDSTONE_LINK.get().updateTransmittedSignal(source.getBlockState(), level, stored);
        checks.check("lost-marker source really still has power", source.getSignal() == 15);
        checks.check("lost marker remains absent", ProbeLifecycle.markers(craft).isEmpty());
        checks.refresh();
        if (seed) {
            checks.check("in-process failed token retains private state", !ProbeScope.isReady(craft) && ProbeScope.isPrivate(craft) && !ProbeLifecycle.pendingTokens().isEmpty());
            checks.signal("in-process lost-marker quarantine blocks world", checks.initialized(new BlockPos(0, 120, 8)), 0);
            Files.writeString(Path.of("failure-restart-fixture.json"), identity.toString());
        } else {
            checks.check("negative restart has no process-local failed token", ProbeLifecycle.pendingTokens().isEmpty());
            checks.check("negative identity-only load publishes WORLD", ProbeScope.isReady(craft) && !ProbeScope.isPrivate(craft));
            checks.signal("EXPECTED UNSAFE lost-marker restart exposes world", checks.initialized(new BlockPos(0, 120, 8)), 15);
        }
        return true;
    }
}
