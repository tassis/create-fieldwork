package dev.tassis.createfieldwork.wireless;

import dev.tassis.createfieldwork.isolation.CraftKey;

import java.util.Objects;

/** Effective radio authorization, not craft membership or registry readiness. */
public sealed interface Scope {
    Scope WORLD = PublicWorld.INSTANCE;
    Scope PENDING = Pending.INSTANCE;

    /** Use for a confirmed private craft, including known crafts awaiting registry reconstruction. */
    static Scope privateCraft(CraftKey craft) {
        return new Private(craft);
    }

    enum PublicWorld implements Scope { INSTANCE }

    /** An unresolved endpoint/transition is not an empty registry or an authorized private craft. */
    enum Pending implements Scope { INSTANCE }

    record Private(CraftKey craft) implements Scope {
        public Private {
            Objects.requireNonNull(craft, "craft");
        }
    }
}
