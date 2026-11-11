package dev.tassis.createfieldwork.isolator.ordinary;

import com.mojang.serialization.MapCodec;
import dev.tassis.createfieldwork.platform.IsolatorIntegrationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;

/** Marker only; ACTIVE is a derived visual and this block emits no ordinary redstone. */
public class IsolatorBlock extends BaseEntityBlock {
    public static final MapCodec<IsolatorBlock> CODEC = simpleCodec(IsolatorBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public IsolatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new IsolatorBlockEntity(pos, state); }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        super.onPlace(state, level, pos, previous, moved);
        if (level instanceof ServerLevel server && !previous.is(this)) IsolatorIntegrationBridge.markerChanged(server, pos, true);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (level instanceof ServerLevel server && !next.is(this)) IsolatorIntegrationBridge.markerChanged(server, pos, false);
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("block.create_fieldwork.redstone_link_isolator.tooltip"));
    }
}
