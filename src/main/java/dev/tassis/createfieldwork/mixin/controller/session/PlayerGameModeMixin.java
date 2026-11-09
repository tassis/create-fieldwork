package dev.tassis.createfieldwork.mixin.controller.session;

import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;

/** Game-mode authority changes invalidate only this player's controller endpoints. */
@Mixin(ServerPlayer.class)
public abstract class PlayerGameModeMixin {
    @WrapMethod(method = "setGameMode")
    private boolean fieldwork$gameMode(GameType mode, Operation<Boolean> upstream) {
        boolean changed = upstream.call(mode);
        if (changed) {
            var player = (ServerPlayer) (Object) this;
            FieldworkStateAccess.get(player.server).controllers().reconcilePlayer(player.getUUID());
        }
        return changed;
    }
}
