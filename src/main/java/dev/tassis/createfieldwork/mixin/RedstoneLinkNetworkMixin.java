package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.llamalad7.mixinextras.sugar.Local;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.ScopeResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class RedstoneLinkNetworkMixin {
    @WrapMethod(method = "updateNetworkOf")
    private void fieldwork$call(LevelAccessor level, IRedstoneLinkable anchor, Operation<Void> upstream) {
        if (!(level instanceof ServerLevel server)) { upstream.call(level, anchor); return; }
        var refresh = FieldworkStateAccess.get(server.getServer()).refresh();
        refresh.enter(server, anchor);
        try { upstream.call(level, anchor); }
        finally { refresh.exit(); }
    }

    @WrapOperation(method = "updateNetworkOf", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange(Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z", ordinal = 0),
        require = 1, expect = 1, allow = 1)
    private boolean fieldwork$sources(IRedstoneLinkable a, IRedstoneLinkable b, Operation<Boolean> upstream,
                                   LevelAccessor level, IRedstoneLinkable anchor) {
        boolean range = upstream.call(a, b); // preserve the audited Sable redirect exactly once
        if (!(level instanceof ServerLevel server)) return range;
        var state = FieldworkStateAccess.get(server.getServer());
        return range && ScopeResolver.pair(server, a, b, state.refresh().currentView());
    }

    @WrapOperation(method = "updateNetworkOf", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange(Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z", ordinal = 1),
        require = 1, expect = 1, allow = 1)
    private boolean fieldwork$receivers(IRedstoneLinkable a, IRedstoneLinkable b, Operation<Boolean> upstream,
                                        LevelAccessor level, IRedstoneLinkable anchor) {
        boolean range = upstream.call(a, b);
        if (!(level instanceof ServerLevel server)) return range;
        var refresh = FieldworkStateAccess.get(server.getServer()).refresh();
        // A cold refresh evaluates every receiver separately. Broadcasting this
        // anchor's aggregate would give nearby receivers out-of-range sources.
        return range && !refresh.targeted() && ScopeResolver.pair(server, a, b, refresh.currentView());
    }

    @Inject(method = "updateNetworkOf", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$customReceiver(LevelAccessor level, IRedstoneLinkable anchor, CallbackInfo ci,
                                          @Local(ordinal = 0) int power) {
        if (level instanceof ServerLevel server && !(anchor instanceof LinkBehaviour) && anchor.isListening()
            && FieldworkStateAccess.get(server.getServer()).refresh().targeted()) anchor.setReceivedStrength(power);
    }
}
