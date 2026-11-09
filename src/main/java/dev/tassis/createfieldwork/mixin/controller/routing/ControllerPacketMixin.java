package dev.tassis.createfieldwork.mixin.controller.routing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerPacketBase;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
import java.util.UUID;

@Mixin(value = LinkedControllerInputPacket.class, remap = false)
public abstract class ControllerPacketMixin {
    @WrapOperation(method = "handleItem", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/controller/LinkedControllerServerHandler;receivePressed(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/util/UUID;Ljava/util/List;Z)V"), require = 1, allow = 1)
    private void fieldwork$provenance(LevelAccessor level, BlockPos pos, UUID id, List<Couple<Frequency>> frequencies,
                                      boolean press, Operation<Void> upstream, ServerPlayer actor, ItemStack controller) {
        var sources = FieldworkStateAccess.get(actor.server).controllers();
        if (!sources.acceptInput(actor, press)) return;
        var previous = sources.begin(actor, ((LinkedControllerPacketBase) (Object) this).getLecternPos(), controller);
        try {
            sources.withdrawInvalidForInput(actor);
            upstream.call(level, pos, id, frequencies, press);
            if (!press && ControllerDevice.read(controller) != null) {
                sources.releaseDeadManaged(actor);
                var scope = dev.tassis.createfieldwork.wireless.refresh.RefreshScope.none();
                for (var frequency : frequencies) scope = scope.union(dev.tassis.createfieldwork.wireless.refresh.RefreshScope.frequency(actor.serverLevel().dimension(), frequency));
                FieldworkStateAccess.get(actor.server).refresh().refresh(null, scope);
            }
        }
        finally { sources.end(previous); }
    }
}
