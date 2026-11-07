package dev.tassis.createfieldwork.acceptance;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.server.FieldworkServerState;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Driver only: all authorization, registry and future isolator behavior is supplied by main. */
@Mod("fieldwork_acceptance")
public final class AcceptanceSuite {
    private MinecraftServer server;
    private FieldworkServerState state;
    private int ticks;
    private boolean finished;
    private boolean coreDone;
    private RestartAcceptance restart;
    private final List<Map<String, Object>> assertions = new ArrayList<>();
    private final String phase = System.getProperty("fieldwork.acceptance.phase", "core");

    public AcceptanceSuite() {
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopped);
    }

    private void started(ServerStartedEvent event) {
        server = event.getServer();
        if (phase.equals("seed") || phase.equals("reload")) restart = new RestartAcceptance(server.overworld(), this::check, phase);
    }

    private void tick(ServerTickEvent.Post event) {
        if (finished || ++ticks < 20) return;
        Throwable failure = null;
        try {
            if (!coreDone) { core(); coreDone = true; }
            if (restart != null && !restart.tick(ticks)) return;
            if (phase.equals("network")) {
                new NetworkAcceptance(server.overworld(), this::check).run();
                new ControllerAcceptance(server.overworld(), this::check).run();
            }
            if (phase.equals("lifecycle")) new LifecycleAcceptance(server.overworld(), this::check).run();
            if (phase.equals("packed")) new PackedAcceptance(server.overworld(), this::check).run();
            if (phase.equals("refresh")) new ScopedRefreshAcceptance(server.overworld(), this::check).run();
            if (phase.equals("movement")) new ControllerMovementAcceptance(server.overworld(), this::check).run();
        } catch (Throwable error) { failure = error; }
        finished = true;
        var result = new LinkedHashMap<String, Object>();
        result.put("timestamp", Instant.now().toString());
        result.put("phase", phase);
        result.put("status", failure == null ? "passed" : "failed");
        if (failure != null) {
            result.put("failure", failure.toString());
            LogUtils.getLogger().error("FIELDWORK MAIN ACCEPTANCE FAILED", failure);
        }
        result.put("assertions", assertions);
        try {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(result);
            Files.writeString(Path.of("result.json"), json);
            Files.writeString(Path.of("result-" + phase + ".json"), json);
        }
        catch (Exception error) { throw new IllegalStateException(error); }
        finally { server.halt(false); }
    }

    private void core() {
        check("research behavior mod is not loaded", !ModList.get().isLoaded("fieldwork_probe"));
        check("server instance implements main holder", server instanceof FieldworkStateAccess);
        state = FieldworkStateAccess.get(server);
        check("same server owns stable state instance", state == FieldworkStateAccess.get(server));
        var key = new CraftKey(Level.OVERWORLD, UUID.randomUUID());
        long before = state.visibilityEpoch();
        check("missing known craft is initially private", state.registry().scope(key).equals(Scope.privateCraft(key)));
        state.registry().publish(key, List.of(BlockPos.ZERO));
        check("published marker set remains private", state.registry().scope(key).equals(Scope.privateCraft(key)));
        state.registry().marker(key, BlockPos.ZERO, false);
        check("last marker removal releases public scope", state.registry().scope(key) == Scope.WORLD);
        check("visibility epoch tracks changes", state.visibilityEpoch() == before + 2);
        state.registry().forget(key);
        check("forget does not leave public scope for stale key", state.registry().scope(key).equals(Scope.privateCraft(key)));
    }

    private void stopped(ServerStoppedEvent event) {
        if (state == null) return;
        try { state.registry(); }
        catch (IllegalStateException expected) {
            if (!expected.getMessage().equals("Fieldwork server session is closed")) throw expected;
            try { Files.writeString(Path.of("state-closed.txt"), "main server state closed after normal save\n"); }
            catch (Exception error) { throw new IllegalStateException(error); }
            return;
        }
        throw new AssertionError("Main server state was not closed");
    }

    private void check(String name, boolean passed) {
        assertions.add(Map.of("name", name, "passed", passed));
        if (!passed) throw new AssertionError(name);
    }
}
