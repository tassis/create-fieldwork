package dev.tassis.createfieldwork.controller;

import dev.tassis.createfieldwork.wireless.Scope;
import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.wireless.RefreshScope;

/** Added only to Create's ManualFrequencyEntry; its world-space getLocation is unchanged. */
public interface ControllerEndpoint {
    ControllerSources.Source fieldwork$source();
    void fieldwork$source(ControllerSources.Source source);
    Scope fieldwork$published();
    void fieldwork$published(Scope scope);
    boolean fieldwork$restricted();
    void fieldwork$restricted(boolean value);
    CraftKey fieldwork$lastPrivate();
    void fieldwork$lastPrivate(CraftKey key);
    RefreshScope fieldwork$publishedNetwork();
    void fieldwork$publishedNetwork(RefreshScope network);
}
