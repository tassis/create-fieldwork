package dev.tassis.createfieldwork.acceptance;

import net.minecraft.core.BlockPos;

/** Test-only no-op, exception and nested block mutation inputs for marker fast-path validation. */
public final class MarkerWriteFaults {
    public static final int NONE = 0, NO_OP = 1, THROW = 2, AFTER_WRITE = 3;
    public static int mode;
    public static BlockPos target;
    public static boolean reached;
    public static Runnable action = () -> {};
    public static void arm(int nextMode, BlockPos position, Runnable callback) {
        mode = nextMode; target = position.immutable(); action = callback; reached = false;
    }
    public static void clear() { mode = NONE; target = null; action = () -> {}; }
    private MarkerWriteFaults() {}
}
