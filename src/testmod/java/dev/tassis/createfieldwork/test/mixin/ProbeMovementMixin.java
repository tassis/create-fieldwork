package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.tassis.createfieldwork.test.MovementAccess;
import dev.tassis.createfieldwork.test.ProbeMovement;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ProbeMovementMixin implements MovementAccess {
    @Shadow public ServerPlayer player;
    @Accessor("awaitingPositionFromClient") public abstract Vec3 probe$awaitingPosition();
    @Accessor("awaitingTeleport") public abstract int probe$teleportId();

    @WrapMethod(method = "handleMovePlayer")
    private void probe$movement(ServerboundMovePlayerPacket packet, Operation<Void> original) {
        if (!ProbeMovement.enabled) { original.call(packet); return; }
        var previous = ProbeMovement.begin(player, packet);
        try { original.call(packet); }
        finally { ProbeMovement.end(previous); }
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", shift = At.Shift.AFTER), require = 1, allow = 1)
    private void probe$accepted(ServerboundMovePlayerPacket packet, CallbackInfo ci) { ProbeMovement.accepted(player); }
}
