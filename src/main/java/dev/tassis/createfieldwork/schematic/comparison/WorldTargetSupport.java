package dev.tassis.createfieldwork.schematic.comparison;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** Client guidance only. The base provider compares loaded ordinary-world positions. */
public final class WorldTargetSupport {
    public interface Snapshot {
        boolean ready();
        boolean comparable(BlockPos pos);
    }

    @FunctionalInterface
    public interface Provider {
        Snapshot capture(Level level);
    }

    private record OrdinarySnapshot(Level level, boolean ready) implements Snapshot {
        @Override public boolean comparable(BlockPos pos) { return ready && level.hasChunkAt(pos); }
    }

    private static volatile Provider provider = level ->
        new OrdinarySnapshot(level, level != null);

    private WorldTargetSupport() {}

    public static void install(Provider implementation) {
        provider = Objects.requireNonNull(implementation, "implementation");
    }

    public static boolean comparable(Level level, BlockPos pos) { return capture(level).comparable(pos); }
    public static Snapshot capture(Level level) { return provider.capture(level); }
}
