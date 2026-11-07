package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SubLevelHeatMapManager.class, remap = false)
public abstract class ProbeSplitMixin {
    @Shadow @Final private ServerSubLevel subLevel;

    @WrapMethod(method = "split")
    private void probe$split(Operation<Void> original) {
        if (!ProbeLifecycle.enabled) { original.call(); return; }
        ProbeLifecycle.begin("split", subLevel.getLevel(), ProbeLifecycle.blocks(subLevel));
        boolean success = false;
        try { original.call(); success = true; }
        finally { ProbeLifecycle.end("split", subLevel.getLevel(), success); }
    }
}
