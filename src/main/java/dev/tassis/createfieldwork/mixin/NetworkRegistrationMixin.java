package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

/** Local visibility cache invalidation, including upstream's last-member removal early return. */
@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public abstract class NetworkRegistrationMixin {
    @WrapMethod(method = "addToNetwork")
    private void fieldwork$added(LevelAccessor level, IRedstoneLinkable endpoint, Operation<Void> upstream) {
        if (level instanceof ServerLevel server) {
            var state = FieldworkStateAccess.get(server.getServer());
            state.controllers().capture(endpoint, server);
            state.invalidateVisibility();
        }
        upstream.call(level, endpoint);
        if (level instanceof ServerLevel server && endpoint instanceof LinkBehaviour behaviour)
            FieldworkStateAccess.get(server.getServer()).publications().published(server, behaviour.blockEntity.getBlockPos());
    }

    @WrapMethod(method = "removeFromNetwork")
    private void fieldwork$removed(LevelAccessor level, IRedstoneLinkable endpoint, Operation<Void> upstream) {
        if (level instanceof ServerLevel server) FieldworkStateAccess.get(server.getServer()).invalidateVisibility();
        upstream.call(level, endpoint);
    }
}
