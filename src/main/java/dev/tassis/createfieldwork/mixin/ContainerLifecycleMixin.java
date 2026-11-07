package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
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
        upstream.call(x, z, reason);
        if (craft != null && craft.getLevel() instanceof ServerLevel level) {
            var state = FieldworkStateAccess.get(level.getServer());
            state.registry().forget(IsolationRuntime.key(craft));
            state.invalidateVisibility();
        }
    }
}
