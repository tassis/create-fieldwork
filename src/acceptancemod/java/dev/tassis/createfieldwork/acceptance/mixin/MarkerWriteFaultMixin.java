package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.MarkerWriteFaults;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MarkerWriteFaultMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void acceptance$before(BlockPos pos, BlockState next, boolean moved, CallbackInfoReturnable<BlockState> ci) {
        int mode = MarkerWriteFaults.mode;
        if (mode == MarkerWriteFaults.NONE || mode == MarkerWriteFaults.AFTER_WRITE || !pos.equals(MarkerWriteFaults.target)) return;
        MarkerWriteFaults.mode = MarkerWriteFaults.NONE; MarkerWriteFaults.reached = true;
        MarkerWriteFaults.action.run();
        if (mode == MarkerWriteFaults.NO_OP) ci.setReturnValue(null);
        else throw new IllegalStateException("acceptance marker write failure");
    }

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;",
        shift = At.Shift.AFTER), require = 1, allow = 1)
    private void acceptance$after(BlockPos pos, BlockState next, boolean moved, CallbackInfoReturnable<BlockState> ci) {
        if (MarkerWriteFaults.mode != MarkerWriteFaults.AFTER_WRITE || !pos.equals(MarkerWriteFaults.target)) return;
        MarkerWriteFaults.mode = MarkerWriteFaults.NONE; MarkerWriteFaults.reached = true;
        MarkerWriteFaults.action.run();
    }
}
