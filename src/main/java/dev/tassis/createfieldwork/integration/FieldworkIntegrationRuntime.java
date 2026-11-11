package dev.tassis.createfieldwork.integration;

import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.platform.IsolatorIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class FieldworkIntegrationRuntime implements IsolatorIntegrationBridge.Runtime {
    @Override
    public void markerChanged(ServerLevel level, BlockPos pos, boolean present) {
        IsolationRuntime.markerChanged(level, pos, present);
    }

    @Override
    public void smartLoaded(BrassIsolatorBlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel level) {
            IsolationRuntime.markerChanged(level, blockEntity.getBlockPos(), true);
            FieldworkStateAccess.get(level.getServer()).bindings().track(blockEntity);
        }
    }

    @Override
    public void smartRemoved(BrassIsolatorBlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel level && !level.getServer().isStopped()) {
            FieldworkStateAccess.get(level.getServer()).bindings().untrack(blockEntity);
        }
    }

    @Override
    public CraftKey currentCraft(BrassIsolatorBlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level) || blockEntity.isRemoved()
            || !level.hasChunkAt(blockEntity.getBlockPos())) return null;
        var owner = Sable.HELPER.getContaining(level, blockEntity.getBlockPos());
        if (owner == null || owner.isRemoved()) return null;
        return level.getChunkAt(blockEntity.getBlockPos()).getBlockEntities().get(blockEntity.getBlockPos()) == blockEntity
            ? IsolationRuntime.key(owner) : null;
    }
}
