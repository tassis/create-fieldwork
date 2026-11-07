package dev.tassis.createfieldwork.test;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod("fieldwork_probe")
public final class ScopeProbeMod {
    private int ticks;
    private boolean finished;
    private ScopeProbeAssertions suite;
    private RestartProbe restart;
    private MinecraftServer server;
    private final String phase = System.getProperty("fieldwork.probe.phase", "suite");

    public ScopeProbeMod() {
        ProbeScope.reset();
        ProbeScope.enabled = !phase.equals("suite");
        ProbeLifecycle.enabled = !phase.equals("suite");
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void started(ServerStartedEvent event) {
        ticks = 0;
        finished = false;
        server = event.getServer();
        suite = new ScopeProbeAssertions(server.overworld());
        if (!phase.equals("suite")) restart = new RestartProbe(server.overworld(), suite, phase);
    }

    private void tick(ServerTickEvent.Post event) {
        if (finished || ++ticks < 20) return;
        try {
            if (restart != null) {
                if (!restart.tick(ticks)) return;
            } else suite.run();
            finish(null);
        } catch (Throwable failure) { finish(failure); }
    }

    private void finish(Throwable failure) {
        finished = true;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("phase", phase);
        result.put("jvmStartMillis", java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
        result.put("minecraft", "1.21.1");
        result.put("stack", "NeoForge 21.1.257 / Create 6.0.10 / Sable 2.0.5 / Aeronautics 1.3.2");
        try {
            result.put("status", failure == null ? "passed" : "failed");
            if (failure != null) {
                LogUtils.getLogger().error("FIELDWORK PROBE FAILED", failure);
                result.put("failure", failure.toString());
            }
        } finally {
            result.put("assertions", suite.results());
            result.put("networkHookCalls", ProbeScope.networkCalls);
            result.put("simulatedHookCalls", ProbeScope.simulatedCalls);
            result.put("lifecycleEvents", ProbeLifecycle.events);
            result.put("lifecycleViolations", ProbeLifecycle.violations);
            result.put("packedNbtInputs", ProbeLifecycle.packedNbtInputs);
            result.put("packedPositionsObserved", ProbeLifecycle.packedPositionsObserved);
            result.put("initializingChunkChecks", ProbeLifecycle.initializingChunkChecks);
            result.put("controllerReconciliations", ProbeController.reconciliations);
            result.put("naturalPlayerTrackingObserved", ProbeController.naturalTrackingObserved);
            result.put("withheldCommits", ProbeLifecycle.withheldCommits);
            result.put("movementEvents", ProbeMovement.events);
            result.put("pendingTransactionTokens", ProbeLifecycle.pendingTokens());
            result.put("expectedUnsafeObservation", phase.equals("reload-failure"));
            result.put("trainQueries", ProbeTrain.scans);
            result.put("trainFilteredCalls", ProbeTrain.filteredCalls);
            result.put("trainPendingDeferrals", ProbeTrain.pendingDeferrals);
            result.put("trainScopeEpoch", ProbeTrain.scopeEpoch);
            result.put("trainObservations", ProbeTrain.observations);
            result.put("mainImplementationLoaded", net.neoforged.fml.ModList.get().isLoaded("create_fieldwork"));
            try {
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(result);
                Files.writeString(Path.of("scope-probe-result.json"), json);
                Files.writeString(Path.of("scope-probe-result-" + phase + ".json"), json);
            } catch (Exception error) {
                LogUtils.getLogger().error("Cannot write probe result", error);
            }
            server.halt(false);
        }
    }
}
