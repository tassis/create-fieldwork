package dev.tassis.createfieldwork.controller;

import net.minecraft.world.phys.Vec3;

/** Read-only vanilla acknowledgement state, not client-supplied scope data. */
public interface ConnectionStateAccess {
    Vec3 fieldwork$awaitingPosition();
}
