package dev.tassis.createfieldwork.schematic.comparison;

import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/** Construction properties only: never whole-state/NBT equality or generic "type" matching. */
public final class BuildStatePolicy {
    public enum Result { MATCH, MISSING, WRONG_BLOCK, WRONG_STATE, UNVERIFIED }
    private static final Set<Property<?>> CONSTRUCTION = Set.of(BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.HALF,
        BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.SLAB_TYPE);
    private static final Set<Property<?>> DYNAMIC = Set.of(BlockStateProperties.POWERED, BlockStateProperties.POWER,
        BlockStateProperties.LIT, BlockStateProperties.WATERLOGGED, BlockStateProperties.OPEN,
        BlockStateProperties.ENABLED, IsolatorBlock.ACTIVE);
    private BuildStatePolicy() {}
    public static Result compare(BlockState expected, BlockState actual) {
        if (expected == null || actual == null || expected.isAir()) return Result.UNVERIFIED;
        if (actual.isAir()) return Result.MISSING;
        if (!expected.is(actual.getBlock())) return Result.WRONG_BLOCK;
        boolean unknown = false;
        for (var property : expected.getProperties()) {
            if (CONSTRUCTION.contains(property)) {
                if (!expected.getValue(property).equals(actual.getValue(property))) return Result.WRONG_STATE;
            } else if (!DYNAMIC.contains(property)) unknown = true;
        }
        return unknown ? Result.UNVERIFIED : Result.MATCH;
    }
    public static boolean error(Result result) { return result == Result.WRONG_BLOCK || result == Result.WRONG_STATE; }
}
