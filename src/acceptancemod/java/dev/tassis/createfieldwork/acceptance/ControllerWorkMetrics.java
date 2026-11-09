package dev.tassis.createfieldwork.acceptance;

/** Test-only Fieldwork controller work counts; excludes Create's native TTL loop. */
public final class ControllerWorkMetrics {
    public record Snapshot(int endpointReconciliations, int fullSweeps) {}

    private static boolean enabled;
    private static int endpointReconciliations;
    private static int fullSweeps;

    private ControllerWorkMetrics() {}

    public static void recordEndpoint() {
        if (enabled) endpointReconciliations++;
    }

    public static void recordFullSweep() {
        if (enabled) fullSweeps++;
    }

    public static Snapshot measure(Runnable operation) {
        endpointReconciliations = 0;
        fullSweeps = 0;
        enabled = true;
        try {
            operation.run();
            return snapshot();
        } finally {
            enabled = false;
        }
    }

    public static Snapshot snapshot() {
        return new Snapshot(endpointReconciliations, fullSweeps);
    }
}
