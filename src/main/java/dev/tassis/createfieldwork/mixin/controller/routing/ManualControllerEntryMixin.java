package dev.tassis.createfieldwork.mixin.controller.routing;

import dev.tassis.createfieldwork.controller.routing.ControllerEndpoint;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler$ManualFrequencyEntry", remap = false)
public abstract class ManualControllerEntryMixin implements ControllerEndpoint {
    @Unique private ControllerSources.Source fieldwork$source;
    @Unique private Scope fieldwork$published;
    @Unique private boolean fieldwork$restricted;
    @Unique private CraftKey fieldwork$lastPrivate;
    @Unique private RefreshScope fieldwork$publishedNetwork;

    @Override public ControllerSources.Source fieldwork$source() { return fieldwork$source; }
    @Override public void fieldwork$source(ControllerSources.Source source) { fieldwork$source = source; }
    @Override public Scope fieldwork$published() { return fieldwork$published; }
    @Override public void fieldwork$published(Scope scope) { fieldwork$published = scope; }
    @Override public boolean fieldwork$restricted() { return fieldwork$restricted; }
    @Override public void fieldwork$restricted(boolean value) { fieldwork$restricted = value; }
    @Override public CraftKey fieldwork$lastPrivate() { return fieldwork$lastPrivate; }
    @Override public void fieldwork$lastPrivate(CraftKey key) { fieldwork$lastPrivate = key; }
    @Override public RefreshScope fieldwork$publishedNetwork() { return fieldwork$publishedNetwork; }
    @Override public void fieldwork$publishedNetwork(RefreshScope network) { fieldwork$publishedNetwork = network; }
}
