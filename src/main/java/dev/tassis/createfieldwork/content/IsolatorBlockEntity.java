package dev.tassis.createfieldwork.content;

import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IsolatorBlockEntity extends BlockEntity {
    public IsolatorBlockEntity(BlockPos pos, BlockState state) { super(FieldworkContent.ISOLATOR_BE.get(), pos, state); }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) IsolationRuntime.markerChanged(server, worldPosition, true);
    }
}
