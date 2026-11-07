package dev.tassis.createfieldwork.acceptance;

/** Non-shipping exception injection only. Never overrides scope, registry data or authorization. */
public final class AcceptanceFaults {
    public static boolean throwPublication;
    public static boolean publicationReached;
    public static boolean throwVisual;
    public static boolean visualReached;
    private AcceptanceFaults() {}
}
