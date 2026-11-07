package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.tassis.createfieldwork.test.ProbeScope;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class ProbeNetworkMixin {
    @WrapOperation(
        method = "updateNetworkOf(Lnet/minecraft/world/level/LevelAccessor;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange(Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"),
        require = 2, expect = 2, allow = 2
    )
    private boolean probe$pair(IRedstoneLinkable a, IRedstoneLinkable b, Operation<Boolean> upstream,
                               LevelAccessor level, IRedstoneLinkable anchor) {
        boolean original = upstream.call(a, b);
        ProbeScope.networkCalls++;
        return original && ProbeScope.allows((Level) level, a, b);
    }
}
