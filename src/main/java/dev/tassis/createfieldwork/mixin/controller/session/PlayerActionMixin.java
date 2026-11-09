package dev.tassis.createfieldwork.mixin.controller.session;

import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct selected-slot and drop packet writes that bypass Inventory mutators. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerActionMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCarriedItem", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$selected(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) { fieldwork$changed(); }

    @Inject(method = "handlePlayerAction", at = @At("RETURN"), require = 6, allow = 6)
    private void fieldwork$action(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        var action = packet.getAction();
        // Hand swaps are covered at each committed LivingEntity#setItemInHand;
        // do not reconcile the same endpoints a third time at packet return.
        if (action == ServerboundPlayerActionPacket.Action.DROP_ITEM ||
            action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) fieldwork$changed();
    }

    private void fieldwork$changed() {
        if (!player.server.isSameThread()) return;
        FieldworkStateAccess.get(player.server).controllers().reconcilePlayer(player.getUUID());
    }
}
