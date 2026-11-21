package dev.tassis.createfieldwork.schematic.table;

import com.simibubi.create.AllBlocks;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ScribingTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BLUEPRINT = 0;
    public static final int MEDIUM = 1;
    public static final int OUTPUT = 2;
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final ScribingChecklist checklist = new ScribingChecklist();
    private ScribingChecklist.Status status = ScribingChecklist.Status.IDLE;
    public static final int PRINT_TICKS = ScribingPrintJob.DURATION;
    private ScribingPrintJob job;
    private boolean generating;
    private boolean ticking;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> status.ordinal();
                case 1 -> job == null ? 0 : PRINT_TICKS - job.elapsed();
                case 2 -> progressTicks();
                case 3 -> isPrinting() ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public ScribingTableBlockEntity(BlockPos pos, BlockState state) {
        super(FieldworkContent.SCRIBING_TABLE_BE.get(), pos, state);
    }

    ContainerData menuData() { return data; }

    public ScribingChecklist.Status status() { return status; }
    public boolean isPrinting() { return job != null; }
    public int progressTicks() { return job == null ? (items.get(OUTPUT).isEmpty() ? 0 : PRINT_TICKS) : job.elapsed(); }

    void printChecklist() {
        if (!(level instanceof ServerLevel) || ticking || generating || job != null) {
            return;
        }
        if (!items.get(OUTPUT).isEmpty()) {
            status = ScribingChecklist.Status.OUTPUT_FULL;
            return;
        }
        status = ScribingChecklist.validateInputs(worldPosition, items.get(BLUEPRINT), items.get(MEDIUM));
        if (status != ScribingChecklist.Status.IDLE) return;
        job = new ScribingPrintJob(items.get(BLUEPRINT), items.get(MEDIUM), 2, false);
        status = ScribingChecklist.Status.PRINTING;
        setChanged();
    }

    public void serverTick() {
        if (ticking) return;
        ticking = true;
        try {
            tickJob();
        } finally {
            ticking = false;
        }
    }

    private void tickJob() {
        if (!(level instanceof ServerLevel server) || job == null || generating) return;
        if (isRemoved() || !server.hasChunkAt(worldPosition) || server.getBlockEntity(worldPosition) != this
            || !server.getBlockState(worldPosition).is(FieldworkContent.SCRIBING_TABLE.get())) {
            cancelForRemoval();
            return;
        }
        if (!matchesJob(job)) {
            changedInputs();
            setChanged();
            return;
        }
        var running = job;
        if (!running.advance()) {
            super.setChanged();
            return;
        }
        generating = true;
        try {
            var result = checklist.print(server, worldPosition, running.blueprint(), running.medium(), running.mode(), running.replaceEntities());
            // Identity loss is first: never overwrite callback cancellation/status.
            if (job != running) return;
            if (isRemoved() || server.getBlockEntity(worldPosition) != this
                || !server.getBlockState(worldPosition).is(FieldworkContent.SCRIBING_TABLE.get())) {
                cancelForRemoval();
                return;
            }
            if (!matchesJob(running)) {
                changedInputs();
                setChanged();
                return;
            }
            if (!result.output().isEmpty()) {
                // No callbacks between publication and exact one-medium consumption.
                items.set(OUTPUT, result.output());
                items.get(MEDIUM).shrink(1);
            }
            job = null;
            status = result.status();
            setChanged();
        } catch (RuntimeException error) {
            if (job == running) failJob();
            com.mojang.logging.LogUtils.getLogger().warn("Scribing job failed at {}", worldPosition, error);
        } catch (Error error) {
            if (job == running) failJob();
            throw error;
        } finally {
            generating = false;
        }
    }

    private boolean matchesJob(ScribingPrintJob current) {
        return current.matches(items.get(BLUEPRINT), items.get(MEDIUM), items.get(OUTPUT), 2, false);
    }

    private void changedInputs() {
        if (job != null) {
            if (!matchesJob(job)) {
                job = null;
                status = ScribingChecklist.Status.INPUT_CHANGED;
            }
        } else if (!generating) {
            status = ScribingChecklist.Status.IDLE;
        }
    }

    private void failJob() {
        job = null;
        status = ScribingChecklist.Status.FAILED;
        setChanged();
    }

    public void cancelForRemoval() {
        job = null;
        status = ScribingChecklist.Status.INPUT_CHANGED;
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public int getMaxStackSize() { return 64; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BLUEPRINT ? ScribingChecklist.isBlueprint(stack)
            : slot == MEDIUM && ScribingChecklist.isMedium(stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(slot == BLUEPRINT ? 1 : getMaxStackSize(stack));
        changedInputs();
        setChanged();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            changedInputs();
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        changedInputs();
        setChanged();
        return removed;
    }

    @Override
    public void clearContent() {
        items.clear();
        changedInputs();
        setChanged();
    }

    @Override
    public void setChanged() {
        if (job != null && !matchesJob(job)) changedInputs();
        super.setChanged();
        if (level == null || level.isClientSide || isRemoved() || level.getBlockEntity(worldPosition) != this) {
            return;
        }
        BlockState current = level.getBlockState(worldPosition);
        if (!current.is(FieldworkContent.SCRIBING_TABLE.get())) {
            return;
        }
        boolean blueprint = ScribingChecklist.isBlueprint(items.get(BLUEPRINT));
        boolean clipboard = items.get(MEDIUM).is(AllBlocks.CLIPBOARD.asItem()) || items.get(OUTPUT).is(AllBlocks.CLIPBOARD.asItem());
        BlockState visual = current.setValue(ScribingTableBlock.SCHEMATIC, blueprint)
            .setValue(ScribingTableBlock.CLIPBOARD, clipboard);
        if (visual != current) {
            level.setBlock(worldPosition, visual, 3);
        }
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public Component getDisplayName() { return Component.translatable("block.create_fieldwork.schematic_scribing_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ScribingTableMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (job != null) tag.put("ScribingJob", job.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        // Legacy table settings are intentionally ignored. Default-policy jobs
        // can resume; a captured non-default job fails matchesJob without consumption.
        job = null;
        generating = false;
        status = ScribingChecklist.Status.IDLE;
        if (tag.contains("ScribingJob")) {
            var restored = ScribingPrintJob.read(tag.getCompound("ScribingJob"), registries);
            if (restored != null && matchesJob(restored)
                && ScribingChecklist.validateInputs(worldPosition, restored.blueprint(), restored.medium()) == ScribingChecklist.Status.IDLE) {
                job = restored;
                status = ScribingChecklist.Status.PRINTING;
            } else {
                status = ScribingChecklist.Status.INPUT_CHANGED;
            }
        }
    }
}
