package dev.tassis.createfieldwork.schematic.table;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Audited Create 6.0.10 cannon policy, without the cannon's two-block firing exclusion. */
public final class ScribingPlacementPolicy {
    private ScribingPlacementPolicy() {}

    public static boolean shouldPlace(BlockGetter world, BlockPos pos, BlockState required, BlockEntity entity,
                                      BlockState current, BlockState other, boolean normalCube,
                                      int mode, boolean replaceBlockEntities) {
        if (!replaceBlockEntities && (current.hasBlockEntity() || other != null && other.hasBlockEntity())) {
            return false;
        }
        if (ignored(required, entity)) {
            return false;
        }
        if (mode == 2 || mode == 3) {
            return replacementAllowed(mode, required.isAir(), normalCube, false, false);
        }
        return replacementAllowed(mode, required.isAir(), normalCube,
            current.isRedstoneConductor(world, pos), other != null && other.isRedstoneConductor(world, pos));
    }

    static boolean replacementAllowed(int mode, boolean air, boolean normalCube, boolean currentSolid, boolean otherSolid) {
        return switch (mode) {
            case 0 -> !air && !currentSolid && !otherSolid;
            case 1 -> !air && (normalCube || !currentSolid && !otherSolid);
            case 2 -> !air;
            case 3 -> true;
            default -> false;
        };
    }

    private static boolean ignored(BlockState state, BlockEntity entity) {
        if (state.is(Blocks.STRUCTURE_VOID)) {
            return true;
        }
        ItemRequirement requirement = ItemRequirement.of(state, entity);
        if (requirement.isEmpty() || requirement.isInvalid()) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
            && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return true;
        }
        if (state.getBlock() instanceof PistonHeadBlock) {
            return true;
        }
        return AllBlocks.BELT.has(state) && state.getValue(BeltBlock.PART) == BeltPart.MIDDLE;
    }
}
