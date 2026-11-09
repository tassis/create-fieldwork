package dev.tassis.createfieldwork.mixin.wireless;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.controller.routing.ControllerSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

/** Local visibility cache invalidation, including upstream's last-member removal early return. */
@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class NetworkRegistrationMixin {
    @WrapMethod(method = "addToNetwork")
    private void fieldwork$added(LevelAccessor level, IRedstoneLinkable endpoint, Operation<Void> upstream) {
        boolean present = fieldwork$present(level, endpoint);
        ControllerSources.NativeAddSnapshot snapshot = null;
        if (level instanceof ServerLevel server) {
            var state = FieldworkStateAccess.get(server.getServer());
            snapshot = state.controllers().beginNativeAdd(endpoint, server);
            state.invalidateVisibility();
        }
        try { upstream.call(level, endpoint); }
        catch (RuntimeException | Error error) {
            if (!present && fieldwork$present(level, endpoint)) try {
                com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint);
            } catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
            if (level instanceof ServerLevel server && snapshot != null) try {
                FieldworkStateAccess.get(server.getServer()).controllers().nativeAddFailed(snapshot);
            } catch (RuntimeException | Error rollback) { error.addSuppressed(rollback); }
            throw error;
        }
        if (level instanceof ServerLevel server) try {
            FieldworkStateAccess.get(server.getServer()).controllers().nativeAdded(server, endpoint);
        } catch (RuntimeException | Error error) {
            if (!present) try { com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint); }
            catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
            if (snapshot != null) try { FieldworkStateAccess.get(server.getServer()).controllers().nativeAddFailed(snapshot); }
            catch (RuntimeException | Error rollback) { error.addSuppressed(rollback); }
            throw error;
        }
        if (level instanceof ServerLevel server && endpoint instanceof LinkBehaviour behaviour)
            FieldworkStateAccess.get(server.getServer()).publications().published(server, behaviour.blockEntity.getBlockPos());
    }

    @WrapMethod(method = "removeFromNetwork")
    private void fieldwork$removed(LevelAccessor level, IRedstoneLinkable endpoint, Operation<Void> upstream) {
        if (level instanceof ServerLevel server) FieldworkStateAccess.get(server.getServer()).invalidateVisibility();
        try { upstream.call(level, endpoint); }
        catch (RuntimeException | Error error) {
            if (level instanceof ServerLevel server)
                FieldworkStateAccess.get(server.getServer()).controllers().nativeRemovalFailed(server, endpoint);
            throw error;
        }
        if (level instanceof ServerLevel server)
            FieldworkStateAccess.get(server.getServer()).controllers().nativeRemoved(server, endpoint);
    }

    @WrapMethod(method = "onUnloadWorld(Lnet/minecraft/world/level/LevelAccessor;)V")
    private void fieldwork$unloaded(LevelAccessor level, Operation<Void> upstream) {
        upstream.call(level);
        if (level instanceof ServerLevel server)
            FieldworkStateAccess.get(server.getServer()).controllers().levelUnloaded(server);
    }

    private static boolean fieldwork$present(LevelAccessor level, IRedstoneLinkable endpoint) {
        var bucket = com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(endpoint.getNetworkKey());
        if (bucket == null) return false;
        for (var candidate : bucket) if (candidate == endpoint) return true;
        return false;
    }
}
