package dev.tassis.createfieldwork.acceptance;

/** Non-shipping exception injection only. Never overrides scope, registry data or authorization. */
public final class AcceptanceFaults {
    public static boolean throwPublication;
    public static boolean publicationReached;
    public static boolean throwVisual;
    public static boolean visualReached;
    public static boolean throwLecternAssignment;
    public static boolean throwNativeAdd;
    public static boolean throwNativeRemove;
    public static boolean throwNativeUnload;
    private AcceptanceFaults() {}
}
