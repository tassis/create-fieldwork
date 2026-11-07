package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import dev.tassis.createfieldwork.controller.ConnectionStateAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerMovementMixin implements ConnectionStateAccess {
    @Shadow public ServerPlayer player;
    @Accessor("awaitingPositionFromClient") public abstract Vec3 fieldwork$awaitingPosition();

    @WrapMethod(method = "handleMovePlayer")
    private void fieldwork$movement(ServerboundMovePlayerPacket packet, Operation<Void> upstream) {
        if (!player.server.isSameThread()) { upstream.call(packet); return; }
        var memberships = FieldworkStateAccess.get(player.server).players();
        var frame = memberships.begin(player, packet);
        try { upstream.call(packet); }
        finally { memberships.end(frame); }
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", shift = At.Shift.AFTER), require = 1, allow = 1)
    private void fieldwork$accepted(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        FieldworkStateAccess.get(player.server).players().accepted(player);
    }
}
