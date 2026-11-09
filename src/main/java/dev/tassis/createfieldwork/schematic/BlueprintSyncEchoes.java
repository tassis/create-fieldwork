package dev.tassis.createfieldwork.schematic;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.ArrayList;
import java.util.List;

/** Bounded expected native slot-sync echoes: an acknowledgement is not a new blueprint. */
public final class BlueprintSyncEchoes {
    private record Echo(int slot, ItemStack expected) {}
    private final List<Echo> echoes = new ArrayList<>();
    public static ItemStack normalized(ItemStack stack) {
        var copy = stack.copy(); SchematicInstances.clearHash(copy); return copy;
    }
    public void clear() { echoes.clear(); }
    public int pending() { return echoes.size(); }
    public void expect(int slot, ItemStack stack, BlockPos anchor, StructurePlaceSettings settings, boolean deployed) {
        var expected = normalized(stack);
        expected.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
        expected.set(AllDataComponents.SCHEMATIC_ROTATION, settings.getRotation());
        expected.set(AllDataComponents.SCHEMATIC_MIRROR, settings.getMirror());
        expected.set(AllDataComponents.SCHEMATIC_DEPLOYED, deployed);
        echoes.add(new Echo(slot, expected));
        if (echoes.size() > 32) echoes.removeFirst();
    }
    public boolean consume(int slot, ItemStack stack) {
        var normalized = normalized(stack);
        for (int i = 0; i < echoes.size(); i++) {
            var echo = echoes.get(i);
            if (echo.slot != slot || !ItemStack.matches(echo.expected, normalized)) continue;
            for (int j = i; j >= 0; j--) if (echoes.get(j).slot == slot) echoes.remove(j);
            return true;
        }
        return false;
    }
}
