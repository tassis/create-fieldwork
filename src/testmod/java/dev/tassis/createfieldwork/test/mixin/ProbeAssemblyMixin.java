package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class ProbeAssemblyMixin {
    @WrapMethod(method = "assembleBlocks")
    private static ServerSubLevel probe$assemble(ServerLevel level, BlockPos anchor, Iterable<BlockPos> positions,
                                                BoundingBox3ic bounds, Operation<ServerSubLevel> original) {
        if (!ProbeLifecycle.enabled) return original.call(level, anchor, positions, bounds);
        ProbeLifecycle.begin("assemble", level, positions);
        boolean success = false;
        try {
            ServerSubLevel craft = original.call(level, anchor, positions, bounds);
            success = craft != null;
            return craft;
        } finally { ProbeLifecycle.end("assemble", level, success); }
    }

    @WrapMethod(method = "moveBlocks")
    private static void probe$move(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform,
                                   Iterable<BlockPos> positions, Operation<Void> original) {
        if (!ProbeLifecycle.enabled) { original.call(level, transform, positions); return; }
        ProbeLifecycle.begin("move", level, positions);
        var destinations = new ArrayList<BlockPos>();
        for (var pos : positions) {
            BlockPos destination = transform.apply(pos);
            destinations.add(destination);
            ProbeLifecycle.recordMarkerMove(level, pos, transform.getLevel(), destination);
        }
        ProbeLifecycle.addDestination(transform.getLevel(), destinations);
        boolean success = false;
        try { original.call(level, transform, positions); success = true; }
        finally { ProbeLifecycle.end("move", level, success); }
    }

    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        ordinal = 0), require = 1, allow = 1)
    private static BlockState probe$failOneWrite(LevelChunk chunk, BlockPos pos, BlockState state, boolean moved,
                                                Operation<BlockState> original) {
        if (ProbeLifecycle.enabled && ProbeLifecycle.skipMarkerWriteOnce && state.is(Blocks.OBSIDIAN)) {
            ProbeLifecycle.skipMarkerWriteOnce = false;
            throw new ProbeLifecycle.InjectedFailure("one-marker-destination-write");
        }
        return original.call(chunk, pos, state, moved);
    }

    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        ordinal = 1), require = 1, allow = 1)
    private static BlockState probe$failOneClear(LevelChunk chunk, BlockPos pos, BlockState state, boolean moved,
                                                Operation<BlockState> original) {
        if (ProbeLifecycle.enabled && ProbeLifecycle.skipMarkerSourceClearOnce && state.isAir() && chunk.getBlockState(pos).is(Blocks.OBSIDIAN)) {
            ProbeLifecycle.skipMarkerSourceClearOnce = false;
            throw new ProbeLifecycle.InjectedFailure("one-marker-source-clear");
        }
        return original.call(chunk, pos, state, moved);
    }

    @Inject(method = "moveBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        shift = At.Shift.AFTER), require = 2, allow = 2)
    private static void probe$write(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform,
                                    Iterable<BlockPos> positions, CallbackInfo ci) {
        if (ProbeLifecycle.enabled) ProbeLifecycle.checkpoint("block-write");
    }
}
