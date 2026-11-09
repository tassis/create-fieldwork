package dev.tassis.createfieldwork.schematic.mask;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

/** Read-only nextSetBit view used only by Create's audited entity helper. Native
 * indices and its independent error bits remain unchanged. */
public final class MaskedEntityBits extends BitSet {
    private final BitSet enabled;
    private final List<BlockEntity> entities;
    private final Predicate<BlockPos> hidden;
    public MaskedEntityBits(BitSet enabled, List<BlockEntity> entities, Predicate<BlockPos> hidden) {
        this.enabled = enabled;
        this.entities = entities;
        this.hidden = hidden;
    }
    @Override public int nextSetBit(int from) {
        int index = enabled.nextSetBit(from);
        while (index >= 0 && index < entities.size() && hidden.test(entities.get(index).getBlockPos())) index = enabled.nextSetBit(index + 1);
        return index;
    }
}
