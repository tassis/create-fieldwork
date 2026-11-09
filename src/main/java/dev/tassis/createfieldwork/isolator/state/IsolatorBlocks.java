package dev.tassis.createfieldwork.isolator.state;

import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.minecraft.world.level.block.state.BlockState;

/** Shared exact marker classification, independent of grant eligibility and ACTIVE. */
public final class IsolatorBlocks {
    private IsolatorBlocks() {}
    public static boolean marker(BlockState state) {
        return state.is(FieldworkContent.ISOLATOR.get()) || smart(state);
    }
    public static boolean smart(BlockState state) { return state.is(FieldworkContent.BRASS_ISOLATOR.get()); }
}
