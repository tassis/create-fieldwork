package dev.tassis.createfieldwork.bindingprobe;

import java.util.UUID;

/** Observation on the actual handled packet instance, not a static cross-server recorder. */
public interface InputIdentityAccess {
    UUID bindingProbe$observedDevice();
    boolean bindingProbe$handledItem();
}
