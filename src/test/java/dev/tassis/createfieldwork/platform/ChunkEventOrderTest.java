package dev.tassis.createfieldwork.platform;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChunkEventOrderTest {
    private final ChunkEventOrder order = new ChunkEventOrder();
    private final ChunkPos chunk = new ChunkPos(3, -5);

    @Test void onlyLatestEventForAChunkCanApply() {
        long old = order.begin(Level.OVERWORLD, chunk);
        long current = order.begin(Level.OVERWORLD, chunk);

        assertTrue(order.claim(Level.OVERWORLD, chunk, current));
        assertFalse(order.claim(Level.OVERWORLD, chunk, old));

        old = order.begin(Level.OVERWORLD, chunk);
        current = order.begin(Level.OVERWORLD, chunk);
        assertFalse(order.claim(Level.OVERWORLD, chunk, old));
        assertTrue(order.claim(Level.OVERWORLD, chunk, current));
    }

    @Test void chunksAndDimensionsAreOrderedIndependently() {
        var otherChunk = new ChunkPos(chunk.x + 1, chunk.z);
        long overworld = order.begin(Level.OVERWORLD, chunk);
        long nether = order.begin(Level.NETHER, chunk);
        long adjacent = order.begin(Level.OVERWORLD, otherChunk);

        assertTrue(order.claim(Level.OVERWORLD, chunk, overworld));
        assertTrue(order.claim(Level.NETHER, chunk, nether));
        assertTrue(order.claim(Level.OVERWORLD, otherChunk, adjacent));
    }

    @Test void clearInvalidatesQueuedWork() {
        long generation = order.begin(Level.OVERWORLD, chunk);

        order.clear();

        assertFalse(order.claim(Level.OVERWORLD, chunk, generation));
    }
}
