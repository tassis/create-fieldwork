package dev.tassis.createfieldwork.schematic.comparison;

import java.util.Queue;
import java.util.function.IntConsumer;

/** The production tick scheduler, independent of rendering/GL. Dirty and periodic
 * refresh share one allowance; discovery has its own. Read/outline permits also
 * enforce the aggregate bounds if a callback changes in the future. */
public final class ComparisonCycle {
    private final ComparisonBudget refresh = new ComparisonBudget(), discovery = new ComparisonBudget();
    private int reads, outlines;

    public void reset() {
        refresh.reset();
        discovery.reset();
        reads = 0;
        outlines = 0;
    }
    public void run(Queue<Integer> dirty, int cacheSize, int templateSize, IntConsumer revisit, IntConsumer discover) {
        reads = 0;
        outlines = 0;
        int priority = 0;
        while (!dirty.isEmpty() && priority < ComparisonBudget.REFRESH_PER_TICK) {
            revisit.accept(dirty.remove());
            priority++;
        }
        refresh.take(cacheSize, ComparisonBudget.REFRESH_PER_TICK - priority, revisit);
        discovery.take(templateSize, ComparisonBudget.DISCOVERY_PER_TICK, discover);
    }
    public boolean read() {
        if (reads >= ComparisonBudget.REFRESH_PER_TICK + ComparisonBudget.DISCOVERY_PER_TICK) return false;
        reads++;
        return true;
    }
    public boolean outline() {
        if (outlines >= ComparisonBudget.OUTLINES) return false;
        outlines++;
        return true;
    }
    public int reads() { return reads; }
    public int outlines() { return outlines; }
}
