package dev.tassis.createfieldwork.platform;

import net.neoforged.fml.ModList;

/** Base-safe activation check for the indivisible Aeronautics integration bundle. */
public final class AeronauticsIntegration {
    private static final String[] REQUIRED_MODS = {"sable", "aeronautics", "simulated"};

    private AeronauticsIntegration() {}

    public static boolean active() {
        var mods = ModList.get();
        for (var mod : REQUIRED_MODS) {
            if (!mods.isLoaded(mod)) return false;
        }
        return true;
    }
}
