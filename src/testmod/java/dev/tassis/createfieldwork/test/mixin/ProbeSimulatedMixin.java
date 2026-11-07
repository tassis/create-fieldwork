package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.test.ProbeScope;
import dev.tassis.createfieldwork.test.ProbeEndpointGuard;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractLinkedReceiverBlockEntity.class, remap = false)
public abstract class ProbeSimulatedMixin {
    @WrapOperation(method = "updateSignal()V", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;getTransmittedStrength()I", ordinal = 0),
        require = 1, expect = 1, allow = 1)
    private int probe$strength(IRedstoneLinkable sender, Operation<Integer> upstream) {
        int strength = upstream.call(sender);
        BlockEntity receiver = (BlockEntity) (Object) this;
        ProbeScope.simulatedCalls++;
        return ProbeScope.allows(receiver.getLevel(), sender, receiver.getBlockPos())
            && ProbeEndpointGuard.accept(receiver.getLevel(), sender, receiver.getBlockPos()) ? strength : 0;
    }
}
