package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import dev.tassis.createfieldwork.test.ProbeTrain;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RedstoneLinkCondition.class, remap = false)
public abstract class ProbeTrainConditionMixin {
    @Shadow @Final private Couple<Frequency> freq;

    @WrapMethod(method = "tickCompletion")
    private boolean probe$condition(Level world, Train train, CompoundTag context, Operation<Boolean> original) {
        if (!ProbeTrain.enabled) return original.call(world, train, context);
        if (!(world instanceof ServerLevel level)) return false;
        var consumer = ProbeTrain.membership(level, train);
        if (consumer.pending()) { ProbeTrain.pendingDeferrals++; return false; }
        int power = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
        boolean stampChanged = !context.hasUUID("FieldworkProbeSession") || !context.getUUID("FieldworkProbeSession").equals(ProbeTrain.session)
            || context.getLong("FieldworkProbeScopeEpoch") != ProbeTrain.scopeEpoch
            || !context.getString("FieldworkProbeConsumer").equals(consumer.key());
        if (!stampChanged && context.contains("LastChecked") && context.getInt("LastChecked") == power)
            return original.call(world, train, context);
        var result = ProbeTrain.query(level, consumer, freq);
        if (result == ProbeTrain.Power.PENDING) { ProbeTrain.pendingDeferrals++; return false; }
        if (stampChanged) context.remove("LastChecked");
        var previous = ProbeTrain.prepared;
        ProbeTrain.prepared = new ProbeTrain.Prepared(freq, result);
        try {
            boolean complete = original.call(world, train, context);
            context.putUUID("FieldworkProbeSession", ProbeTrain.session);
            context.putLong("FieldworkProbeScopeEpoch", ProbeTrain.scopeEpoch);
            context.putString("FieldworkProbeConsumer", consumer.key());
            return complete;
        } finally { ProbeTrain.prepared = previous; }
    }

    @WrapOperation(method = "tickCompletion", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;hasAnyLoadedPower(Lnet/createmod/catnip/data/Couple;)Z"), require = 1, allow = 1)
    private boolean probe$visible(RedstoneLinkNetworkHandler handler, Couple<Frequency> frequency, Operation<Boolean> original) {
        boolean raw = original.call(handler, frequency);
        if (!ProbeTrain.enabled) return raw;
        if (ProbeTrain.prepared == null || !ProbeTrain.prepared.frequency().equals(frequency)) throw new AssertionError("Unscoped train query");
        ProbeTrain.filteredCalls++;
        return ProbeTrain.prepared.result() == ProbeTrain.Power.PRESENT;
    }
}
