package dev.tassis.createfieldwork.server;

import net.minecraft.server.MinecraftServer;

/** Implemented by the server instance mixin; no static mutable storage. */
public interface FieldworkStateAccess {
    FieldworkServerState fieldwork$state();

    static FieldworkServerState get(MinecraftServer server) {
        return ((FieldworkStateAccess) server).fieldwork$state();
    }
}
