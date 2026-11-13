package dev.tassis.createfieldwork.schematic.table;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** One captured, unconsumed print request. Progress counts loaded server ticks, never wall time. */
final class ScribingPrintJob {
    static final int DURATION = 21;
    private final ItemStack blueprint;
    private final ItemStack medium;
    private final int mode;
    private final boolean replaceEntities;
    private int elapsed;

    ScribingPrintJob(ItemStack blueprint, ItemStack medium, int mode, boolean replaceEntities) {
        this.blueprint = blueprint.copy();
        this.medium = medium.copy();
        this.mode = mode;
        this.replaceEntities = replaceEntities;
    }

    int elapsed() { return elapsed; }
    ItemStack blueprint() { return blueprint.copy(); }
    ItemStack medium() { return medium.copy(); }
    int mode() { return mode; }
    boolean replaceEntities() { return replaceEntities; }

    boolean matches(ItemStack currentBlueprint, ItemStack currentMedium, ItemStack output, int currentMode, boolean currentReplace) {
        return output.isEmpty() && currentMode == mode && currentReplace == replaceEntities
            && ItemStack.matches(blueprint, currentBlueprint) && ItemStack.matches(medium, currentMedium);
    }

    boolean advance() {
        if (elapsed >= DURATION) return false;
        return ++elapsed == DURATION;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put("Blueprint", blueprint.save(registries));
        tag.put("Medium", medium.save(registries));
        tag.putInt("Mode", mode);
        tag.putBoolean("ReplaceEntities", replaceEntities);
        tag.putInt("Elapsed", elapsed);
        return tag;
    }

    static ScribingPrintJob read(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("Blueprint", Tag.TAG_COMPOUND) || !tag.contains("Medium", Tag.TAG_COMPOUND)
            || !tag.contains("Mode", Tag.TAG_INT) || !tag.contains("Elapsed", Tag.TAG_INT)
            || !tag.contains("ReplaceEntities", Tag.TAG_BYTE)) {
            return null;
        }
        int elapsed = tag.getInt("Elapsed");
        int mode = tag.getInt("Mode");
        if (elapsed < 0 || elapsed >= DURATION || mode < 0 || mode > 3) return null;
        try {
            var blueprint = ItemStack.parseOptional(registries, tag.getCompound("Blueprint"));
            var medium = ItemStack.parseOptional(registries, tag.getCompound("Medium"));
            if (!ScribingChecklist.isBlueprint(blueprint) || blueprint.getCount() != 1
                || !ScribingChecklist.isMedium(medium) || medium.getCount() > medium.getMaxStackSize()) {
                return null;
            }
            var job = new ScribingPrintJob(blueprint, medium, mode, tag.getBoolean("ReplaceEntities"));
            job.elapsed = elapsed;
            return job;
        } catch (RuntimeException invalid) {
            return null;
        }
    }
}
