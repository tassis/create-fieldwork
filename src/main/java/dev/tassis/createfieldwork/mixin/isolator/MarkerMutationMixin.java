package dev.tassis.createfieldwork.mixin.isolator;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.isolator.state.IsolatorBlocks;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.publication.SourcePublications;
import dev.tassis.createfieldwork.isolator.state.IsolationRegistry;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.List;

/** Fence isolator identity changes and index unregistered link sources before chunk callbacks. */
@Mixin(LevelChunk.class)
public abstract class MarkerMutationMixin {
    @WrapMethod(method = "setBlockState")
    private BlockState fieldwork$marker(BlockPos pos, BlockState next, boolean moved, Operation<BlockState> upstream) {
        var chunk = (LevelChunk) (Object) this;
        if (!(chunk.getLevel() instanceof ServerLevel level)) return upstream.call(pos, next, moved);
        var previous = chunk.getBlockState(pos);
        if (SourcePublications.transmitter(previous) != SourcePublications.transmitter(next)) {
            var publications = FieldworkStateAccess.get(level.getServer()).publications();
            if (SourcePublications.transmitter(next)) publications.expect(level, pos, true);
            try { return fieldwork$identity(level, pos, previous, next, moved, upstream); }
            finally {
                if (SourcePublications.transmitter(next)) publications.written(level, pos);
                else publications.published(level, pos);
            }
        }
        return fieldwork$identity(level, pos, previous, next, moved, upstream);
    }

    @Unique
    private BlockState fieldwork$identity(ServerLevel level, BlockPos pos, BlockState previous, BlockState next,
                                          boolean moved, Operation<BlockState> upstream) {
        if ((!IsolatorBlocks.marker(previous) && !IsolatorBlocks.marker(next)) || previous.getBlock() == next.getBlock()) return upstream.call(pos, next, moved);
        var state = FieldworkStateAccess.get(level.getServer());
        var owner = Sable.HELPER.getContaining(level, pos);
        if (state.transitions().active() || owner == null) return upstream.call(pos, next, moved);
        var coordinator = state.transitions();
        var key = IsolationRuntime.key(owner);
        var snapshot = state.registry().snapshot(key);
        boolean wasMarker = IsolatorBlocks.marker(previous);
        boolean willBeMarker = IsolatorBlocks.marker(next);
        if (!IsolatorBlocks.smart(previous) && !IsolatorBlocks.smart(next) && snapshot.status() == IsolationRegistry.Status.READY
            && snapshot.positions().contains(pos) == wasMarker
            && !coordinator.blocked(key, null) && !coordinator.blocked(level, pos, null)
            && snapshot.positions().stream().anyMatch(other -> !other.equals(pos)
                && IsolatorBlocks.marker(level.getBlockState(other)))) {
            // A distinct real marker keeps the craft private throughout. Stage
            // the index before callbacks so nested last-marker removal still
            // takes the full boundary path, then reconcile the actual result.
            try {
                state.registry().marker(key, pos, willBeMarker);
                return upstream.call(pos, next, moved);
            }
            finally {
                if (state.registry().snapshot(key).status() == IsolationRegistry.Status.READY)
                    state.registry().marker(key, pos, IsolatorBlocks.marker(level.getBlockState(pos)));
                state.visuals().update(level, pos);
            }
        }
        coordinator.begin("marker", level, List.of(pos));
        boolean success = false;
        try {
            coordinator.expectMarker(level, pos, IsolatorBlocks.marker(next));
            coordinator.mutating();
            var result = upstream.call(pos, next, moved);
            success = IsolatorBlocks.marker(level.getBlockState(pos)) == IsolatorBlocks.marker(next);
            return result;
        } finally { coordinator.end("marker", success); }
    }
}
