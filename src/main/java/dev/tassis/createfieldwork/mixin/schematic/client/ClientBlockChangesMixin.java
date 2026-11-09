package dev.tassis.createfieldwork.mixin.schematic.client;

import dev.tassis.createfieldwork.schematic.client.SchematicClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class ClientBlockChangesMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"), require = 1)
    private void fieldwork$changed(BlockPos pos, BlockState next, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        var chunk = (LevelChunk) (Object) this;
        SchematicClientState.changed(chunk.getLevel(), pos);
    }
}
