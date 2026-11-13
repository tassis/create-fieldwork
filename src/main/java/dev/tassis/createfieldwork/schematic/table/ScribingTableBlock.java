package dev.tassis.createfieldwork.schematic.table;

import com.mojang.serialization.MapCodec;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class ScribingTableBlock extends BaseEntityBlock {
    public static final MapCodec<ScribingTableBlock> CODEC = simpleCodec(ScribingTableBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty SCHEMATIC = BooleanProperty.create("schematic");
    public static final BooleanProperty CLIPBOARD = BooleanProperty.create("clipboard");
    private static final Map<Direction, VoxelShape> SHAPES = shapes();

    public ScribingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH)
            .setValue(SCHEMATIC, false).setValue(CLIPBOARD, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ScribingTableBlockEntity(pos, state); }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FieldworkContent.SCRIBING_TABLE_BE.get(),
            (world, pos, block, table) -> table.serverTick());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SCHEMATIC, CLIPBOARD);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ScribingTableBlockEntity table) {
            player.openMenu(table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!next.is(this) && level.getBlockEntity(pos) instanceof ScribingTableBlockEntity table) {
            table.cancelForRemoval();
            Containers.dropContents(level, pos, table);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    private static Map<Direction, VoxelShape> shapes() {
        VoxelShape shape = Shapes.empty();
        for (double x : new double[]{.65, 12.65}) {
            shape = Shapes.or(shape, Block.box(x, 0, 1.6, x + 2.35, 14.25, 3.85));
            shape = Shapes.or(shape, Block.box(x, 0, 11.4, x + 2.35, 10.55, 13.65));
        }
        shape = Shapes.or(shape, Block.box(2, 3.25, 12, 14, 4.25, 13.2),
            Block.box(2, 5, 2.3, 14, 6, 3.3), Block.box(1, 14, 1.5, 15.8, 16, 4));
        // Native collisions are axis-aligned: a stepped thin envelope follows
        // the visual slope, leaving the center under the desk genuinely open.
        for (int step = 0; step < 6; step++) {
            double z = 2 + step * 2;
            double top = 13 - (z - 8) * Math.tan(Math.PI / 8);
            shape = Shapes.or(shape, Block.box(.5, top - 1.6, z, 15.8, top, z + 2));
        }
        var result = new EnumMap<Direction, VoxelShape>(Direction.class);
        for (Direction facing : new Direction[]{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST}) {
            result.put(facing, shape.optimize());
            VoxelShape[] rotated = {Shapes.empty()};
            shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> rotated[0] = Shapes.or(rotated[0],
                Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
            shape = rotated[0];
        }
        return Map.copyOf(result);
    }
}
