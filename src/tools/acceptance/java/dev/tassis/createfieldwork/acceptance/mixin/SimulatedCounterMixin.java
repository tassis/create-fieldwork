package dev.tassis.createfieldwork.acceptance.mixin;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.acceptance.RefreshMetrics;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractLinkedReceiverBlockEntity.class, remap = false)
public abstract class SimulatedCounterMixin {
    @Inject(method = "updateSignal", at = @At("HEAD"), require = 1, allow = 1)
    private void acceptance$count(CallbackInfo ci) {
        var receiver = (AbstractLinkedReceiverBlockEntity) (Object) this;
        var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
        if (behaviour != null && receiver.getLevel() instanceof ServerLevel level)
            RefreshMetrics.record(level.dimension(), behaviour.getNetworkKey(), true, receiver.getBlockPos(),
                FieldworkStateAccess.get(level.getServer()).refresh().targeted());
    }
}
