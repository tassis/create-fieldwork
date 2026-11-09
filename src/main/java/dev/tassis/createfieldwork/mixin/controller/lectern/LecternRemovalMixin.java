package dev.tassis.createfieldwork.mixin.controller.lectern;

import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Universal block-entity removal seam, narrowed to indexed controller lecterns. */
@Mixin(BlockEntity.class)
public abstract class LecternRemovalMixin {
    @Inject(method = "setRemoved", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$lecternRemoved(CallbackInfo ci) {
        if (!((Object) this instanceof LecternControllerBlockEntity lectern) ||
            !(lectern.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        FieldworkStateAccess.get(level.getServer()).controllers()
            .reconcileLectern(level.dimension(), lectern.getBlockPos());
    }
}
