package dev.tassis.createfieldwork.schematic.table;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Generic vanilla stacks isolate snapshot/timing policy; real schematic NBT is covered by server acceptance. */
class ScribingPrintJobTest {
    @Test
    void completesExactlyOnTwentyFirstActiveTickAndCannotCompleteTwice() {
        var job = new ScribingPrintJob(new ItemStack(Items.PAPER), new ItemStack(Items.BOOK, 2), 2, false);
        for (int tick = 1; tick < 21; tick++) {
            assertFalse(job.advance());
            assertEquals(tick, job.elapsed());
        }
        assertTrue(job.advance());
        assertEquals(21, job.elapsed());
        assertFalse(job.advance());
        assertEquals(21, job.elapsed());
    }

    @Test
    void snapshotsAreIndependentAndCountsComponentsOptionsAndOutputMustMatch() {
        var blueprint = new ItemStack(Items.PAPER);
        var medium = new ItemStack(Items.BOOK, 3);
        var job = new ScribingPrintJob(blueprint, medium, 2, false);
        assertTrue(job.matches(blueprint, medium, ItemStack.EMPTY, 2, false));
        medium.shrink(1);
        assertFalse(job.matches(blueprint, medium, ItemStack.EMPTY, 2, false));
        medium.grow(1);
        assertTrue(job.matches(blueprint, medium, ItemStack.EMPTY, 2, false));
        medium.set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
        assertFalse(job.matches(blueprint, medium, ItemStack.EMPTY, 2, false));
        assertEquals(3, job.medium().getCount());
        assertFalse(job.medium().has(DataComponents.CUSTOM_NAME));
        assertFalse(job.matches(blueprint, job.medium(), ItemStack.EMPTY, 1, false));
        assertFalse(job.matches(blueprint, job.medium(), ItemStack.EMPTY, 2, true));
        assertFalse(job.matches(blueprint, job.medium(), new ItemStack(Items.WRITTEN_BOOK), 2, false));
        var exposedCopy = job.blueprint();
        exposedCopy.shrink(1);
        assertEquals(1, job.blueprint().getCount());
    }
}
