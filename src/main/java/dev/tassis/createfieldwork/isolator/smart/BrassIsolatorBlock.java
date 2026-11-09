package dev.tassis.createfieldwork.isolator.smart;

import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Smart variant retains ordinary isolation, with an independent grant record. */
public final class BrassIsolatorBlock extends IsolatorBlock {
    public static final MapCodec<BrassIsolatorBlock> CODEC = simpleCodec(BrassIsolatorBlock::new);
    public BrassIsolatorBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BrassIsolatorBlockEntity(pos, state); }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                          java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(net.minecraft.network.chat.Component.translatable("block.create_fieldwork.brass_redstone_link_isolator.binding_tooltip"));
    }
}
