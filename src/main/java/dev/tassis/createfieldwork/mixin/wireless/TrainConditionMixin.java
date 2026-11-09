package dev.tassis.createfieldwork.mixin.wireless;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.train.TrainConditionQuery;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RedstoneLinkCondition.class, remap = false)
public abstract class TrainConditionMixin {
    @Shadow @Final private Couple<Frequency> freq;

    @WrapMethod(method = "tickCompletion")
    private boolean fieldwork$condition(Level world, Train train, CompoundTag context, Operation<Boolean> upstream) {
        if (!(world instanceof ServerLevel level)) return upstream.call(world, train, context);
        var state = FieldworkStateAccess.get(level.getServer());
        int power = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
        boolean changed = !context.hasUUID("FieldworkSession") || !context.getUUID("FieldworkSession").equals(state.session())
            || context.getLong("FieldworkVisibility") != state.visibilityEpoch();
        if (!changed && context.contains("LastChecked") && context.getInt("LastChecked") == power)
            return upstream.call(world, train, context);
        var query = state.trainQuery();
        var result = query.query(freq);
        if (result == TrainConditionQuery.Power.PENDING) return false;
        if (changed) context.remove("LastChecked");
        var previous = query.prepared();
        query.prepared(new TrainConditionQuery.Prepared(freq, result));
        try {
            boolean complete = upstream.call(world, train, context);
            context.putUUID("FieldworkSession", state.session());
            context.putLong("FieldworkVisibility", state.visibilityEpoch());
            return complete;
        } finally { query.prepared(previous); }
    }

    @WrapOperation(method = "tickCompletion", at = @At(value = "INVOKE", target =
        "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;hasAnyLoadedPower(Lnet/createmod/catnip/data/Couple;)Z"), require = 1, allow = 1)
    private boolean fieldwork$publicPower(RedstoneLinkNetworkHandler handler, Couple<Frequency> frequency,
                                         Operation<Boolean> upstream, Level world, Train train, CompoundTag context) {
        boolean raw = upstream.call(handler, frequency);
        if (!(world instanceof ServerLevel level)) return raw;
        var prepared = FieldworkStateAccess.get(level.getServer()).trainQuery().prepared();
        if (prepared == null || !prepared.frequency().equals(frequency)) throw new IllegalStateException("Unscoped train condition query");
        return prepared.power() == TrainConditionQuery.Power.PRESENT;
    }
}
