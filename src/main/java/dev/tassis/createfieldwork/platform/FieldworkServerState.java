package dev.tassis.createfieldwork.platform;

import dev.tassis.createfieldwork.isolator.state.IsolationRegistry;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.wireless.refresh.WirelessRefresh;
import dev.tassis.createfieldwork.wireless.train.TrainConditionQuery;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import dev.tassis.createfieldwork.controller.session.PlayerSessions;
import dev.tassis.createfieldwork.wireless.publication.SourcePublications;
import dev.tassis.createfieldwork.isolator.lifecycle.VisualUpdates;
import dev.tassis.createfieldwork.isolator.binding.BindingAuthority;
import dev.tassis.createfieldwork.isolator.binding.BindingRevocations;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.UUID;

/** Owned by exactly one MinecraftServer instance, not a process-wide active-server registry. */
public final class FieldworkServerState {
    private final Runnable threadCheck;
    private MinecraftServer server;
    private TransitionCoordinator transitions;
    private WirelessRefresh refresh;
    private TrainConditionQuery trainQuery;
    private PlayerSessions playerSessions;
    private ControllerSources controllers;
    private SourcePublications publications;
    private VisualUpdates visuals;
    private BindingRevocations bindingRevocations;
    private BindingAuthority bindings;
    private final UUID session = UUID.randomUUID();
    private final IsolationRegistry registry;
    private long visibilityEpoch;
    private boolean closed;

    public FieldworkServerState(MinecraftServer server) {
        this(() -> {
            if (!server.isSameThread()) {
                throw new IllegalStateException("Fieldwork state requires the server thread");
            }
        });
        Objects.requireNonNull(server, "server");
        this.server = server;
    }

    FieldworkServerState(Runnable threadCheck) {
        this.threadCheck = Objects.requireNonNull(threadCheck, "threadCheck");
        registry = new IsolationRegistry(this::checkThread, this::invalidateVisibility);
    }

    public void checkThread() {
        threadCheck.run();
        if (closed) {
            throw new IllegalStateException("Fieldwork server session is closed");
        }
    }

    public IsolationRegistry registry() {
        checkThread();
        return registry;
    }

    public UUID session() {
        checkThread();
        return session;
    }

    public long visibilityEpoch() {
        checkThread();
        return visibilityEpoch;
    }

    public void invalidateVisibility() {
        checkThread();
        visibilityEpoch++;
        if (publications != null) {
            publications.reclassify();
        }
    }
    public SourcePublications publications() {
        checkThread();
        if (publications == null) {
            publications = new SourcePublications(this);
        }
        return publications;
    }
    /** Non-callback commit stamp: all throwable reclassification must precede barrier release. */
    public void publicationCommitted() {
        checkThread();
        visibilityEpoch++;
    }
    public VisualUpdates visuals() {
        checkThread();
        if (visuals == null) {
            visuals = new VisualUpdates(this);
        }
        return visuals;
    }
    public MinecraftServer server() {
        checkThread();
        if (server == null) {
            throw new IllegalStateException("No runtime server in this state");
        }
        return server;
    }
    public TransitionCoordinator transitions() {
        checkThread();
        if (transitions == null) {
            transitions = new TransitionCoordinator(this);
        }
        return transitions;
    }
    public WirelessRefresh refresh() {
        checkThread();
        if (refresh == null) {
            refresh = new WirelessRefresh(this);
        }
        return refresh;
    }
    public TrainConditionQuery trainQuery() {
        checkThread();
        if (trainQuery == null) {
            trainQuery = new TrainConditionQuery(this);
        }
        return trainQuery;
    }
    public PlayerSessions playerSessions() {
        checkThread();
        if (playerSessions == null) {
            playerSessions = new PlayerSessions(this);
        }
        return playerSessions;
    }
    public ControllerSources controllers() {
        checkThread();
        if (controllers == null) {
            controllers = new ControllerSources(this);
        }
        return controllers;
    }

    public BindingRevocations bindingRevocations() {
        return bindingRevocations(null);
    }
    public BindingRevocations bindingRevocations(net.minecraft.server.level.ServerLevel constructingLevel) {
        checkThread();
        if (bindingRevocations == null) {
            var storageLevel = server().overworld();
            if (storageLevel == null
                && constructingLevel != null
                && constructingLevel.getServer() == server()
                && constructingLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                storageLevel = constructingLevel;
            }
            if (storageLevel == null) {
                throw new IllegalStateException("Binding revocation storage is not initialized");
            }
            bindingRevocations = storageLevel.getDataStorage().computeIfAbsent(
                BindingRevocations.FACTORY, "fieldwork_binding_revocations");
            bindingRevocations.attach(this);
        }
        return bindingRevocations;
    }
    public BindingAuthority bindings() {
        checkThread();
        if (bindings == null) {
            bindings = new BindingAuthority(this);
        }
        return bindings;
    }

    public void close() {
        threadCheck.run();
        if (closed) {
            return;
        }
        registry.clear();
        if (transitions != null) {
            transitions.clear();
        }
        if (playerSessions != null) {
            playerSessions.clear();
        }
        if (controllers != null) {
            controllers.clear();
        }
        if (refresh != null) {
            refresh.clear();
        }
        if (publications != null) {
            publications.clear();
        }
        if (visuals != null) {
            visuals.clear();
        }
        if (bindings != null) {
            bindings.clear();
        }
        closed = true;
    }
}
