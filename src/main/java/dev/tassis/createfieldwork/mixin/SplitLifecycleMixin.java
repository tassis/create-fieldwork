package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SubLevelHeatMapManager.class, remap = false)
public abstract class SplitLifecycleMixin {
    @Shadow @Final private ServerSubLevel subLevel;

    @WrapMethod(method = "split")
    private void fieldwork$split(Operation<Void> upstream) {
        var coordinator = FieldworkStateAccess.get(subLevel.getLevel().getServer()).transitions();
        coordinator.begin("split", subLevel.getLevel(), IsolationRuntime.blocks(subLevel));
        boolean success = false;
        try { coordinator.mutating(); upstream.call(); success = true; }
        finally { coordinator.end("split", success); }
    }
}
