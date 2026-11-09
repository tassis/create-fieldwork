package dev.tassis.createfieldwork.controller.reset;

import com.simibubi.create.AllItems;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ControllerResetRecipe extends CustomRecipe {
    public ControllerResetRecipe(CraftingBookCategory category) { super(category); }
    @Override public boolean matches(CraftingInput input, Level level) {
        return input.ingredientCount() == 1 && input.items().stream().filter(item -> !item.isEmpty()).allMatch(item ->
            item.is(AllItems.LINKED_CONTROLLER.get()) && (ControllerDevice.read(item) != null || ControllerReset.pending(item)));
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return input.items().stream().filter(item -> !item.isEmpty()).findFirst().map(ControllerReset::preview).orElse(ItemStack.EMPTY);
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0; }
    @Override public RecipeSerializer<?> getSerializer() { return FieldworkContent.CONTROLLER_RESET.get(); }
}
