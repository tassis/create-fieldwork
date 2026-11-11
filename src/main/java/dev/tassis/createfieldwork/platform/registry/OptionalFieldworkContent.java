package dev.tassis.createfieldwork.platform.registry;

import dev.tassis.createfieldwork.controller.reset.ControllerResetRecipe;
import dev.tassis.createfieldwork.platform.CreateFieldwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OptionalFieldworkContent {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateFieldwork.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateFieldwork.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ControllerResetRecipe>> CONTROLLER_RESET =
        RECIPES.register("controller_reset", () -> new SimpleCraftingRecipeSerializer<>(ControllerResetRecipe::new));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIELDWORK_TAB = CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_fieldwork"))
            .icon(() -> FieldworkContent.ISOLATOR_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FieldworkContent.ISOLATOR_ITEM.get());
                output.accept(FieldworkContent.BRASS_ISOLATOR_ITEM.get());
            })
            .build());

    private OptionalFieldworkContent() {}

    public static void register(IEventBus bus) {
        CREATIVE_TABS.register(bus);
        RECIPES.register(bus);
        bus.addListener(OptionalFieldworkContent::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(FieldworkContent.ISOLATOR_ITEM);
            event.accept(FieldworkContent.BRASS_ISOLATOR_ITEM);
        }
    }
}
