package dev.tassis.createfieldwork.acceptance;

/** Fault injection only; supplies no scope, grant or identity answers. */
public final class BindingFaults {
    public static String point = "";
    public static boolean reached;
    public static boolean preFrameObserved;
    private BindingFaults() {}
    public static void arm(String next) { point = next; reached = false; preFrameObserved = false; }
    public static void hit(String current) {
        if (!point.equals(current) && !point.equals(current + "-error")) return;
        boolean asError = point.endsWith("-error");
        point = ""; reached = true;
        if (asError) throw new AssertionError("binding acceptance error: " + current);
        throw new IllegalStateException("binding acceptance fault: " + current);
    }
}
