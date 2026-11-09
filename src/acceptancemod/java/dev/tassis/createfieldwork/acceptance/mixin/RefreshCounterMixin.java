package dev.tassis.createfieldwork.acceptance.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.tassis.createfieldwork.acceptance.RefreshMetrics;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class RefreshCounterMixin {
    @Inject(method = "updateNetworkOf", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$count(LevelAccessor world, IRedstoneLinkable anchor, CallbackInfo ci) {
        if (world instanceof ServerLevel level) RefreshMetrics.record(level.dimension(), anchor.getNetworkKey(), false, anchor.getLocation(),
            FieldworkStateAccess.get(level.getServer()).refresh().targeted());
    }
}
