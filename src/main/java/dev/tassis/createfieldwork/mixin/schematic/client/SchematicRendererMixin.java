package dev.tassis.createfieldwork.mixin.schematic.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.tassis.createfieldwork.schematic.mask.GhostMask;
import dev.tassis.createfieldwork.schematic.client.SchematicGhostRenderer;
import dev.tassis.createfieldwork.schematic.mask.MaskedEntityBits;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.BitSet;
import java.util.List;
import java.util.Set;

@Mixin(value = SchematicRenderer.class, remap = false)
public abstract class SchematicRendererMixin implements SchematicGhostRenderer {
    @Shadow @Final private BlockPos anchor;
    @Shadow public abstract void update();
    @Unique private Set<Long> fieldwork$hidden = Set.of();
    @Unique private Mirror fieldwork$variant = Mirror.NONE;
    @Unique private Vec3i fieldwork$size = Vec3i.ZERO;

    @Override public void fieldwork$visibility(Set<Long> sourcePositions, Mirror variant, Vec3i size) {
        if (fieldwork$hidden == sourcePositions && fieldwork$variant == variant && fieldwork$size.equals(size)) return;
        fieldwork$hidden = sourcePositions; fieldwork$variant = variant; fieldwork$size = size;
        update(); // Existing native cache, only on a coalesced mask/selection change.
    }
    @Unique private boolean fieldwork$hidden(BlockPos virtualPos) {
        return !fieldwork$hidden.isEmpty() && fieldwork$hidden.contains(GhostMask.sourcePosition(virtualPos.subtract(anchor), fieldwork$variant, fieldwork$size));
    }
    @WrapOperation(method = "drawLayer", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/levelWrappers/SchematicLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState fieldwork$omitCorrect(SchematicLevel level, BlockPos pos, Operation<BlockState> upstream) {
        return fieldwork$hidden(pos) ? Blocks.AIR.defaultBlockState() : upstream.call(level, pos);
    }
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/render/BlockEntityRenderHelper;renderBlockEntities(Ljava/util/List;Ljava/util/BitSet;Ljava/util/BitSet;Lcom/simibubi/create/foundation/virtualWorld/VirtualRenderWorld;Lnet/minecraft/world/level/Level;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V"), require = 1, allow = 1)
    private void fieldwork$omitCorrectEntities(List<BlockEntity> entities, BitSet enabled, BitSet errors, VirtualRenderWorld virtualWorld,
        Level level, PoseStack pose, Matrix4f transform, MultiBufferSource buffers, float partialTicks, Operation<Void> upstream) {
        if (fieldwork$hidden.isEmpty()) {
            upstream.call(entities, enabled, errors, virtualWorld, level, pose, transform, buffers, partialTicks); return;
        }
        // Preserve native indices and error bits. The audited helper only reads
        // nextSetBit; filtering its existing walk adds no second entity scan.
        BitSet visible = new MaskedEntityBits(enabled, entities, this::fieldwork$hidden);
        upstream.call(entities, visible, errors, virtualWorld, level, pose, transform, buffers, partialTicks);
    }
}
