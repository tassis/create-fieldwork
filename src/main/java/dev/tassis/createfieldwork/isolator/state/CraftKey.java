package dev.tassis.createfieldwork.isolator.state;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

/** Craft identity within one logical server's state; never a plot coordinate or live craft reference. */
public record CraftKey(ResourceKey<Level> dimension, UUID id) {
    public CraftKey {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(id, "id");
    }
}
