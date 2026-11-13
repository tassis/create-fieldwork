package dev.tassis.createfieldwork.schematic.table;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.LinkedHashSet;

/** Explicit printing reads the current server file, world and supplies; it never constructs blocks. */
public final class ScribingChecklist {
    public enum Status {
        // COOLDOWN retains its earlier wire ordinal; timed jobs do not emit it.
        IDLE, PRINTED, PARTIAL, NO_SCHEMATIC, UNDEPLOYED, TOO_FAR, UNREADABLE,
        NO_MEDIUM, OUTPUT_FULL, COOLDOWN, FAILED, INPUT_CHANGED, PRINTING;

        public static Status byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : FAILED;
        }

        public String translationKey() {
            return "gui.create_fieldwork.scribing.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Result(Status status, ItemStack output) {
        static Result failure(Status status) {
            return new Result(status, ItemStack.EMPTY);
        }
    }

    public static boolean isBlueprint(ItemStack stack) {
        return stack.is(AllItems.SCHEMATIC.get());
    }

    public static boolean isMedium(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.WRITTEN_BOOK) || stack.is(AllBlocks.CLIPBOARD.asItem());
    }

    public Result print(ServerLevel level, BlockPos table, ItemStack blueprint, ItemStack medium,
                        int mode, boolean replaceBlockEntities) {
        Status preflight = validateInputs(table, blueprint, medium);
        if (preflight != Status.IDLE) return Result.failure(preflight);
        try {
            // Uploads can replace or delete the same filename without changing
            // item components. Never authorize a new report from cached content.
            var printer = new SchematicPrinter();
            printer.loadSchematic(blueprint, level, true);
            if (!printer.isLoaded() || printer.isErrored() || printer.isWorldEmpty()) {
                return Result.failure(Status.UNREADABLE);
            }
            var checklist = new MaterialChecklist();
            printer.markAllBlockRequirements(checklist, level, (pos, state, entity, current, other, normalCube) ->
                ScribingPlacementPolicy.shouldPlace(level, pos, state, entity, current, other, normalCube,
                    mode, replaceBlockEntities));
            printer.markAllEntityRequirements(checklist);
            collectAvailable(level, table, checklist);
            ItemStack output = medium.is(AllBlocks.CLIPBOARD.asItem())
                ? checklist.createWrittenClipboard() : checklist.createWrittenBook();
            return new Result(checklist.blocksNotLoaded ? Status.PARTIAL : Status.PRINTED, output);
        } catch (RuntimeException error) {
            LogUtils.getLogger().warn("Cannot create schematic material checklist at {}", table, error);
            return Result.failure(Status.FAILED);
        }
    }

    /** Cheap preflight only: no file I/O, template placement or world scanning. */
    static Status validateInputs(BlockPos table, ItemStack blueprint, ItemStack medium) {
        if (!isBlueprint(blueprint) || blueprint.getCount() != 1) {
            return Status.NO_SCHEMATIC;
        }
        BlockPos anchor = blueprint.get(AllDataComponents.SCHEMATIC_ANCHOR);
        if (anchor == null || !Boolean.TRUE.equals(blueprint.get(AllDataComponents.SCHEMATIC_DEPLOYED))) {
            return Status.UNDEPLOYED;
        }
        if (!anchor.closerThan(table, 256)) {
            return Status.TOO_FAR;
        }
        if (!isMedium(medium) || medium.getCount() > medium.getMaxStackSize()) {
            return Status.NO_MEDIUM;
        }
        String owner = blueprint.get(AllDataComponents.SCHEMATIC_OWNER);
        String file = blueprint.get(AllDataComponents.SCHEMATIC_FILE);
        if (owner == null || owner.isBlank() || file == null || file.isBlank()) {
            return Status.UNREADABLE;
        }
        return Status.IDLE;
    }

    private static void collectAvailable(ServerLevel level, BlockPos table, MaterialChecklist checklist) {
        var inventories = new LinkedHashSet<IItemHandler>();
        for (Direction face : Direction.values()) {
            BlockPos pos = table.relative(face);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) != null) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face.getOpposite());
                if (handler != null) {
                    inventories.add(handler);
                }
            }
        }
        for (IItemHandler handler : inventories) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (!handler.extractItem(slot, 1, true).isEmpty()) {
                    checklist.collect(handler.getStackInSlot(slot));
                }
            }
        }
    }
}
