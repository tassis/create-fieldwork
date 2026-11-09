package dev.tassis.createfieldwork.bindingprobe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Test-only authority record. Not a second production isolator or a finished brass block. */
public final class BindingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("fieldwork_binding_probe");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fieldwork_binding_probe");
    public static final DeferredBlock<AuthorizationBlock> BLOCK = BLOCKS.register("authorization_record",
        () -> new AuthorizationBlock(BlockBehaviour.Properties.of().strength(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AuthorizationBlockEntity>> ENTITY =
        ENTITIES.register("authorization_record", () -> BlockEntityType.Builder.of(AuthorizationBlockEntity::new, BLOCK.get()).build(null));

    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
    private BindingContent() {}

    public static final class AuthorizationBlock extends BaseEntityBlock {
        public static final MapCodec<AuthorizationBlock> CODEC = simpleCodec(AuthorizationBlock::new);
        public AuthorizationBlock(Properties properties) { super(properties); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AuthorizationBlockEntity(pos, state); }
    }
}
