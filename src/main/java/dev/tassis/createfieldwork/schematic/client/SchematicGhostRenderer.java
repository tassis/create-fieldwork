package dev.tassis.createfieldwork.schematic.client;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;

import java.util.Set;

public interface SchematicGhostRenderer {
    void fieldwork$visibility(Set<Long> sourcePositions, Mirror variant, Vec3i size);
}
