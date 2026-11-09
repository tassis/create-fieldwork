package dev.tassis.createfieldwork.acceptance.mixin;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.tassis.createfieldwork.acceptance.AcceptanceFaults;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Non-shipping faults that exercise the production ownership wrappers. */
@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class NetworkLifecycleFaultMixin {
    @Inject(method = "addToNetwork", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$add(LevelAccessor level, IRedstoneLinkable endpoint, CallbackInfo ci) {
        if (AcceptanceFaults.throwNativeAdd) throw new IllegalStateException("native add fault");
    }

    @Inject(method = "removeFromNetwork", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$remove(LevelAccessor level, IRedstoneLinkable endpoint, CallbackInfo ci) {
        if (AcceptanceFaults.throwNativeRemove) throw new IllegalStateException("native remove fault");
    }

    @Inject(method = "onUnloadWorld(Lnet/minecraft/world/level/LevelAccessor;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$unload(LevelAccessor level, CallbackInfo ci) {
        if (AcceptanceFaults.throwNativeUnload) throw new IllegalStateException("native unload fault");
    }
}
