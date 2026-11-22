package dev.tassis.createfieldwork.platform;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Per-server ordering for chunk callbacks that may cross onto the server thread. */
final class ChunkEventOrder {
    private record Key(ResourceKey<Level> dimension, int x, int z) {}

    private final Map<Key, Long> latest = new HashMap<>();
    private long next;

    synchronized long begin(ResourceKey<Level> dimension, ChunkPos chunk) {
        long generation = ++next;
        latest.put(new Key(dimension, chunk.x, chunk.z), generation);
        return generation;
    }

    synchronized boolean claim(ResourceKey<Level> dimension, ChunkPos chunk, long generation) {
        return latest.remove(new Key(dimension, chunk.x, chunk.z), generation);
    }

    synchronized void clear() {
        latest.clear();
    }
}
