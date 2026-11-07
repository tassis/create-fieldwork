package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.RefreshScope;
import dev.tassis.createfieldwork.wireless.Scope;
import dev.tassis.createfieldwork.controller.ControllerEndpoint;
import dev.tassis.createfieldwork.controller.ControllerSources;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Mixin(value = LinkedControllerServerHandler.class, remap = false)
public abstract class ControllerHandlerMixin {
    @WrapOperation(method = "receivePressed", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/controller/LinkedControllerServerHandler$ManualFrequencyEntry;updatePosition(Lnet/minecraft/core/BlockPos;)V"), require = 1, allow = 1)
    private static void fieldwork$rebind(@Coerce Object entry, BlockPos pos, Operation<Void> upstream,
                                        LevelAccessor level, BlockPos position, UUID id, List<Couple<Frequency>> frequencies, boolean press) {
        if (!(level instanceof ServerLevel server) || !(entry instanceof ControllerEndpoint endpoint)) {
            upstream.call(entry, pos); return;
        }
        var state = FieldworkStateAccess.get(server.getServer());
        var link = (IRedstoneLinkable) entry;
        BlockPos previousPosition = link.getLocation().immutable();
        var key = link.getNetworkKey();
        var previousKey = Couple.create(key.getFirst(), key.getSecond());
        var previousActual = state.controllers().scope(server, endpoint, null);
        var previousPublished = endpoint.fieldwork$published();
        var previousSource = endpoint.fieldwork$source();
        boolean previouslyAlive = link.isAlive();
        state.controllers().capture(link, server);
        upstream.call(entry, pos);
        var currentActual = state.controllers().scope(server, endpoint, null);
        var currentSource = endpoint.fieldwork$source();
        key = link.getNetworkKey();
        var currentKey = Couple.create(key.getFirst(), key.getSecond());
        boolean alive = link.isAlive();
        if (state.controllers().reconcileEndpoint(server, link)) return; // full old/new scope/key withdrawal
        var currentPublished = endpoint.fieldwork$published();
        boolean restricted = previousActual != Scope.WORLD || currentActual != Scope.WORLD
            || previousPublished != Scope.WORLD || currentPublished != Scope.WORLD;
        if (!restricted) return; // pure WORLD position-only notification remains upstream behavior
        boolean positionChanged = !previousPosition.equals(link.getLocation());
        boolean sourceChanged = !ControllerSources.sameProvenance(previousSource, currentSource)
            || previouslyAlive != alive || !previousKey.equals(currentKey)
            || !Objects.equals(previousActual, currentActual) || !Objects.equals(previousPublished, currentPublished);
        if (!positionChanged && !sourceChanged) return;
        state.invalidateVisibility();
        if (positionChanged && !sourceChanged && previouslyAlive && alive
            && previousActual instanceof Scope.Private privateScope && previousActual.equals(currentActual)
            && previousActual.equals(previousPublished) && previousActual.equals(currentPublished)) {
            state.refresh().refresh(null, RefreshScope.privateReceivers(server.dimension(), currentKey, privateScope.craft()));
        } else state.refresh().refresh(null, RefreshScope.frequency(server.dimension(), previousKey)
            .union(RefreshScope.frequency(server.dimension(), currentKey)));
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 1, allow = 1)
    private static void fieldwork$reconcile(LevelAccessor level, CallbackInfo ci) {
        if (level instanceof ServerLevel server) FieldworkStateAccess.get(server.getServer()).controllers().reconcile(server);
    }
}
