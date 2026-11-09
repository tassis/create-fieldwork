package dev.tassis.createfieldwork.schematic.comparison;

import java.util.function.IntConsumer;

/** Engineering limits, not measured frame-time claims. No work happens in rendering. */
public final class ComparisonBudget {
    public static final int DISCOVERY_PER_TICK = 128;
    public static final int REFRESH_PER_TICK = 128;
    public static final int CACHE_POSITIONS = 4096;
    public static final int OUTLINES = 128;
    public static final int RANGE = 64;
    public static final int MAX_TEMPLATE_BLOCKS = 1_000_000;
    public static final int MAX_TEMPLATE_EXTENT = 512;
    public static final int MAX_CRAFT_BOUNDS = 256;
    public static final int MAX_RESULT_AGE = 40;
    private int cursor;
    public void reset() { cursor = 0; }
    public int take(int size, int budget, IntConsumer visitor) {
        if (size < 0 || budget < 0) throw new IllegalArgumentException("Negative comparison bound");
        int count = Math.min(size, budget);
        for (int i = 0; i < count; i++) {
            if (cursor >= size) cursor = 0;
            visitor.accept(cursor++);
        }
        return count;
    }
}
