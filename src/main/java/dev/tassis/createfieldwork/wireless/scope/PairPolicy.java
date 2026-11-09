package dev.tassis.createfieldwork.wireless.scope;

import java.util.Objects;

/** Pure, symmetric policy. Both scopes must come from the same logical server context. */
public final class PairPolicy {
    private PairPolicy() {}

    public static boolean allows(Scope source, Scope target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (source == Scope.PENDING || target == Scope.PENDING) return false;
        return source.equals(target);
    }

    /** Never grants frequency/range permission denied by the upstream Create/Sable operation. */
    public static boolean allows(Scope source, Scope target, boolean upstreamResult) {
        return allows(source, target) && upstreamResult;
    }
}
