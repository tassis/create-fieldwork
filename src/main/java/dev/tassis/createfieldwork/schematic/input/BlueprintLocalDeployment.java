package dev.tassis.createfieldwork.schematic.input;

import com.simibubi.create.AllDataComponents;
import dev.tassis.createfieldwork.schematic.BlueprintSyncEchoes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** A synchronous native DeployTool write is not a replacement schematic or a sync echo. */
public final class BlueprintLocalDeployment {
    public record Selection(Object world, Object player, int slot, ItemStack stack, ItemStack previous) {}

    private final Selection original;
    private final ItemStack before;

    private BlueprintLocalDeployment(Selection original, ItemStack before) {
        this.original = original;
        this.before = before;
    }

    public static BlueprintLocalDeployment begin(Selection current) {
        if (current == null || current.world() == null || current.player() == null
            || !BlueprintSelection.valid(current.stack())) {
            return null;
        }
        var before = BlueprintSyncEchoes.normalized(current.stack());
        if (!ItemStack.matches(current.previous(), before)) {
            return null;
        }
        return new BlueprintLocalDeployment(current, before);
    }

    /** Called only after a successful native action, with its raw logical target (not getAnchor). */
    public ItemStack acknowledge(Selection current, BlockPos nativeTarget, boolean nativeDeployed) {
        if (current == null || current.world() != original.world() || current.player() != original.player()
            || current.slot() != original.slot() || current.stack() != original.stack()
            || current.previous() != original.previous() || !nativeDeployed
            || !BlueprintSelection.valid(current.stack())) {
            return null;
        }
        var stack = current.stack();
        var anchor = stack.get(AllDataComponents.SCHEMATIC_ANCHOR);
        if (anchor == null || !anchor.equals(nativeTarget)
            || !Boolean.TRUE.equals(stack.get(AllDataComponents.SCHEMATIC_DEPLOYED))) {
            return null;
        }

        // Position writes these two components, not the live rotation/mirror.
        // In particular, do not serialize the transformation back into the item here.
        var expected = before.copy();
        expected.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        expected.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
        var after = BlueprintSyncEchoes.normalized(stack);
        return ItemStack.matches(expected, after) ? after : null;
    }
}
