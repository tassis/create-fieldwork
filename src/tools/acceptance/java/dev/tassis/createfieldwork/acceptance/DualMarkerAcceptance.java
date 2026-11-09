package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Actual mixed marker assembly/mutation and native upgrade recipe, no registry answers. */
final class DualMarkerAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    DualMarkerAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    void run() {
        var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("create_fieldwork", "brass_redstone_link_isolator")).orElseThrow();
        check.accept("smart upgrade is shapeless", holder.value() instanceof ShapelessRecipe);
        var recipe = (ShapelessRecipe) holder.value();
        var input = CraftingInput.of(2, 2, List.of(AllItems.BRASS_SHEET.asStack(), FieldworkContent.ISOLATOR_ITEM.get().getDefaultInstance(), AllItems.BRASS_SHEET.asStack(), AllItems.BRASS_SHEET.asStack()));
        check.accept("ordinary plus3 brass plates matches smart recipe", recipe.matches(input, level));
        var output = recipe.assemble(input, level.registryAccess());
        check.accept("smart upgrade outputs one smart item", output.is(FieldworkContent.BRASS_ISOLATOR_ITEM.get()) && output.getCount() == 1);
        var origin = new BlockPos(-256, 120, 64);
        var positions = new ArrayList<BlockPos>();
        for (int i = 0; i < 2; i++) { var p = origin.east(i).below(); level.setBlock(p, Blocks.STONE.defaultBlockState(), 3); positions.add(p); }
        level.setBlock(origin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); positions.add(origin);
        level.setBlock(origin.east(), FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3); positions.add(origin.east());
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        var key = IsolationRuntime.key(craft); var stored = craft.getPlot().getCenterBlock();
        var state = FieldworkStateAccess.get(level.getServer());
        check.accept("mixed marker assembly reconstructs both positions", state.registry().snapshot(key).positions().size() == 2);
        check.accept("smart marker ACTIVE normalized", level.getBlockState(stored.east()).getValue(IsolatorBlock.ACTIVE));
        level.removeBlock(stored, false);
        check.accept("smart marker alone retains PRIVATE", state.registry().scope(key).equals(Scope.privateCraft(key)));
        level.setBlock(stored.east(), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        check.accept("smart to ordinary swap remains ready private", state.registry().scope(key).equals(Scope.privateCraft(key)) && state.registry().snapshot(key).positions().size() == 1);
        level.setBlock(stored.east(), FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3);
        check.accept("ordinary to smart swap retains marker membership", state.registry().snapshot(key).positions().size() == 1);
        level.removeBlock(stored.east(), false);
        check.accept("removing final smart marker restores WORLD", state.registry().scope(key) == Scope.WORLD);
        check.accept("mixed marker mutations leave no failed tokens", state.transitions().failedTokens().isEmpty());
    }
}
