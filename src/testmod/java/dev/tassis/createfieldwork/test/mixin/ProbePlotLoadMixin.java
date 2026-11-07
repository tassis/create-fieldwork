package dev.tassis.createfieldwork.test.mixin;

import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import dev.tassis.createfieldwork.test.ProbeScope;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerLevelPlot.class, remap = false)
public abstract class ProbePlotLoadMixin {
    @Inject(method = "load", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/LevelChunk;registerAllBlockEntitiesAfterLevelLoad()V",
        shift = At.Shift.AFTER), require = 1, allow = 1)
    private void probe$chunk(CompoundTag tag, CallbackInfo ci) {
        if (ProbeLifecycle.enabled) ProbeLifecycle.checkpoint("loaded-chunk");
        if (ProbeLifecycle.enabled && System.getProperty("fieldwork.probe.phase", "").endsWith("-multi")) {
            var craft = ((ServerLevelPlot) (Object) this).getSubLevel();
            ProbeLifecycle.initializingChunkChecks++;
            if (ProbeScope.isReady(craft)) ProbeLifecycle.violations.add("chunk published READY before full load");
            for (var holder : craft.getPlot().getLoadedChunks()) {
                var chunk = holder.getChunk();
                ProbeLifecycle.packedPositionsObserved += chunk.getBlockEntitiesPos().size() - chunk.getBlockEntities().size();
            }
        }
    }
}
