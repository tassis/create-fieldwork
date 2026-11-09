package dev.tassis.createfieldwork.mixin.isolator;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class ContainerLifecycleMixin {
    @Inject(method = "allocateSubLevel", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$allocated(UUID id, int x, int z, Pose3d pose, CallbackInfoReturnable<SubLevel> cir) {
        var craft = cir.getReturnValue();
        if (craft != null && craft.getLevel() instanceof ServerLevel level)
            FieldworkStateAccess.get(level.getServer()).transitions().allocated(craft);
    }

    @WrapMethod(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V")
    private void fieldwork$removed(int x, int z, SubLevelRemovalReason reason, Operation<Void> upstream) {
        var craft = ((SubLevelContainer) (Object) this).getSubLevel(x, z);
        if (!(craft instanceof ServerSubLevel loaded) || !(craft.getLevel() instanceof ServerLevel level) ||
            !level.getServer().isRunning()) {
            upstream.call(x, z, reason); return;
        }
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        var markers = IsolationRuntime.markers(loaded);
        coordinator.begin("remove", level, markers);
        for (var marker : markers) coordinator.expectMarkerDetached(level, marker);
        boolean success = false;
        try {
            coordinator.mutating();
            upstream.call(x, z, reason);
            success = ((SubLevelContainer) (Object) this).getSubLevel(loaded.getUniqueId()) == null;
        } finally { coordinator.end("remove", success); }
    }
}
