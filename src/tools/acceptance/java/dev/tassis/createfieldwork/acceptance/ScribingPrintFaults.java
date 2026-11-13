package dev.tassis.createfieldwork.acceptance;

/** Non-shipping one-shot callback/fault seam; never supplies a material answer. */
public final class ScribingPrintFaults {
    private static Runnable next;
    private static Runnable during;
    private static Runnable after;

    static void arm(Runnable callback) {
        if (next != null) throw new IllegalStateException("Scribing fault already armed");
        next = callback;
    }

    public static void fire() {
        Runnable callback = next;
        next = null;
        if (callback != null) callback.run();
    }

    static void armDuring(Runnable callback) { during = callback; }
    static void armAfter(Runnable callback) { after = callback; }
    public static void fireDuring() {
        var callback = during;
        during = null;
        if (callback != null) callback.run();
    }
    public static void fireAfter() {
        var callback = after;
        after = null;
        if (callback != null) callback.run();
    }
    static void clear() { next = during = after = null; }
    private ScribingPrintFaults() {}
}
