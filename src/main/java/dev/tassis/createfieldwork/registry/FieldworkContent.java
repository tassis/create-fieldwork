package dev.tassis.createfieldwork.registry;

import dev.tassis.createfieldwork.CreateFieldwork;
import dev.tassis.createfieldwork.content.IsolatorBlock;
import dev.tassis.createfieldwork.content.IsolatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;

public final class FieldworkContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateFieldwork.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateFieldwork.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateFieldwork.MOD_ID);
    public static final DeferredBlock<IsolatorBlock> ISOLATOR = BLOCKS.register("redstone_link_isolator", () ->
        new IsolatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ISOLATOR_ITEM = ITEMS.registerSimpleBlockItem(ISOLATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IsolatorBlockEntity>> ISOLATOR_BE =
        BLOCK_ENTITIES.register("redstone_link_isolator", () -> BlockEntityType.Builder.of(IsolatorBlockEntity::new, ISOLATOR.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);
        bus.addListener(FieldworkContent::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) event.accept(ISOLATOR_ITEM);
    }

    private FieldworkContent() {}
}
