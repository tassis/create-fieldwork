package dev.tassis.createfieldwork.mixin.wireless;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractLinkedReceiverBlockEntity.class, remap = false)
public abstract class SimulatedReceiverMixin {
    @WrapMethod(method = "updateSignal")
    private void fieldwork$call(Operation<Void> upstream) {
        var receiver = (BlockEntity) (Object) this;
        if (!(receiver.getLevel() instanceof ServerLevel level)) { upstream.call(); return; }
        var refresh = FieldworkStateAccess.get(level.getServer()).refresh();
        refresh.remember((AbstractLinkedReceiverBlockEntity) (Object) this);
        refresh.enter(level, this);
        try { upstream.call(); }
        finally { refresh.exit(); }
    }

    @WrapOperation(method = "updateSignal()V", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;getTransmittedStrength()I", ordinal = 0),
        require = 1, expect = 1, allow = 1)
    private int fieldwork$strength(IRedstoneLinkable sender, Operation<Integer> upstream) {
        int strength = upstream.call(sender);
        var receiver = (BlockEntity) (Object) this;
        if (!(receiver.getLevel() instanceof ServerLevel level)) return strength;
        var state = FieldworkStateAccess.get(level.getServer());
        return ScopeResolver.direct(level, sender, receiver.getBlockPos(), state.refresh().currentView()) ? strength : 0;
    }
}
