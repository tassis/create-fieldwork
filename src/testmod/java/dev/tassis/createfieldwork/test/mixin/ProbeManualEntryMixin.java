package dev.tassis.createfieldwork.test.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.tassis.createfieldwork.test.ControllerEndpoint;
import dev.tassis.createfieldwork.test.ProbeController;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler$ManualFrequencyEntry", remap = false)
public abstract class ProbeManualEntryMixin implements ControllerEndpoint {
    @Unique private ProbeController.Source probe$source;
    @Unique private String probe$published;
    @Unique private boolean probe$restricted;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
    private void probe$created(BlockPos pos, Couple<Frequency> key, CallbackInfo ci) {
        probe$source = ProbeController.current;
        if (probe$source != null) probe$published = ProbeController.key(probe$source.player().level(), (IRedstoneLinkable) this);
    }
    @Inject(method = "updatePosition", at = @At("HEAD"), require = 1, allow = 1)
    private void probe$updated(BlockPos pos, CallbackInfo ci) { probe$source = ProbeController.current; }
    @Override public ProbeController.Source probe$source() { return probe$source; }
    @Override public void probe$capture(ProbeController.Source source) { probe$source = source; }
    @Override public String probe$publishedScope() { return probe$published; }
    @Override public void probe$publishedScope(String value) { probe$published = value; }
    @Override public boolean probe$restrictedHistory() { return probe$restricted; }
    @Override public void probe$markRestricted() { probe$restricted = true; }
    @Override public void probe$restricted(boolean value) { probe$restricted = value; }
}
