package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import dev.tassis.createfieldwork.schematic.table.ScribingTableBlockEntity;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.function.BiConsumer;

/** Production timed-job safety; most cases drive the real ticker method deterministically. */
final class ScribingTimedAcceptance {
    private final ScribingFixtures fixtures;
    private final BiConsumer<String, Boolean> check;
    private int scenario;
    private record Case(ScribingTableBlockEntity table, ServerPlayer actor, ScribingTableMenu menu) {}

    private ScribingTimedAcceptance(ScribingFixtures fixtures, BiConsumer<String, Boolean> check) {
        this.fixtures = fixtures;
        this.check = check;
    }

    static void run(ScribingFixtures fixtures, BiConsumer<String, Boolean> check) throws Exception {
        var suite = new ScribingTimedAcceptance(fixtures, check);
        suite.boundaryAndNotifications();
        suite.cancellation();
        suite.savedJobs();
        suite.freshAtFinish();
        suite.reentrancyAndFaults();
        suite.removalAndTransfer();
    }

    private Case fresh() {
        var table = fixtures.table(ScribingFixtures.TABLE.south(24 + scenario++ * 4));
        table.setItem(0, fixtures.blueprint(ScribingFixtures.ANCHOR));
        table.setItem(1, new ItemStack(Items.BOOK, 2));
        var actor = fixtures.actor(table.getBlockPos());
        return new Case(table, actor, fixtures.menu(table, actor));
    }

    private void start(Case value) {
        value.menu.clickMenuButton(value.actor, ScribingTableMenu.PRINT);
        check.accept("timed case starts without preconsumption " + scenario, value.table.isPrinting()
            && value.table.progressTicks() == 0 && value.table.getItem(1).getCount() == 2 && value.table.getItem(2).isEmpty());
    }

    private void boundaryAndNotifications() {
        var value = fresh();
        start(value);
        ScribingFixtures.advance(value.table, 20);
        check.accept("obsolete settings requests cannot change or cancel active work",
            !value.menu.clickMenuButton(value.actor, 1) && !value.menu.clickMenuButton(value.actor, 2)
                && value.table.isPrinting() && value.table.progressTicks() == 20);
        value.menu.clickMenuButton(value.actor, ScribingTableMenu.PRINT);
        value.table.setChanged();
        value.table.setItem(1, value.table.getItem(1).copy());
        check.accept("tick20 repeat and identical notification keep exact progress", value.table.progressTicks() == 20
            && value.table.isPrinting() && value.table.getItem(1).getCount() == 2 && value.table.getItem(2).isEmpty());
        value.table.serverTick();
        check.accept("tick21 commits exactly one output and medium", !value.table.isPrinting()
            && value.table.getItem(1).getCount() == 1 && value.table.getItem(2).is(Items.WRITTEN_BOOK));
        ScribingFixtures.advance(value.table, 42);
        check.accept("idle ticks never duplicate a completed output", value.table.getItem(1).getCount() == 1 && value.table.getItem(2).getCount() == 1);
    }

    private void cancellation() {
        for (int kind = 0; kind < 6; kind++) {
            var value = fresh();
            start(value);
            ScribingFixtures.advance(value.table, 5);
            int expectedMedia = 2;
            switch (kind) {
                case 0 -> value.table.removeItem(0, 1);
                case 1 -> value.table.removeItemNoUpdate(0);
                case 2 -> { value.table.removeItem(1, 1); expectedMedia = 1; }
                case 3 -> { value.table.getItem(1).shrink(1); expectedMedia = 1; }
                case 4 -> value.table.getItem(1).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal("Changed"));
                case 5 -> value.table.setItem(2, new ItemStack(Items.WRITTEN_BOOK));
                default -> throw new AssertionError();
            }
            value.table.serverTick();
            ScribingFixtures.complete(value.table);
            check.accept("mutation cancels without further consumption " + kind, !value.table.isPrinting()
                && value.table.status() == ScribingChecklist.Status.INPUT_CHANGED && value.table.getItem(1).getCount() == expectedMedia
                && (kind == 5 ? value.table.getItem(2).getCount() == 1 : value.table.getItem(2).isEmpty()));
        }
        var retry = fresh();
        start(retry);
        ItemStack held = retry.table.removeItem(0, 1);
        retry.table.setItem(0, held);
        retry.menu.clickMenuButton(retry.actor, ScribingTableMenu.PRINT);
        check.accept("cancelled request can retry from zero without redundant cooldown", retry.table.isPrinting() && retry.table.progressTicks() == 0);
        ScribingFixtures.complete(retry.table);
        check.accept("retry consumes only one medium", retry.table.getItem(1).getCount() == 1 && retry.table.getItem(2).getCount() == 1);
    }

    private void savedJobs() {
        var value = fresh();
        start(value);
        ScribingFixtures.advance(value.table, 10);
        CompoundTag saved = value.table.saveWithFullMetadata(fixtures.level.registryAccess());
        var restored = decode(value.table, saved);
        check.accept("active snapshot NBT preserves exact progress without consuming", restored.isPrinting() && restored.progressTicks() == 10
            && restored.getItem(1).getCount() == 2 && restored.getItem(2).isEmpty());
        var legacy = saved.copy();
        legacy.putInt("ReplaceMode", 3);
        legacy.putBoolean("ReplaceBlockEntities", true);
        legacy.getCompound("ScribingJob").putInt("Mode", 3);
        legacy.getCompound("ScribingJob").putBoolean("ReplaceEntities", true);
        var oldCustomJob = decode(value.table, legacy);
        check.accept("legacy customized job is cancelled without consuming or retargeting its inputs",
            !oldCustomJob.isPrinting() && oldCustomJob.status() == ScribingChecklist.Status.INPUT_CHANGED
                && oldCustomJob.getItem(1).getCount() == 2 && oldCustomJob.getItem(2).isEmpty()
                && ItemStack.matches(oldCustomJob.getItem(0), value.table.getItem(0)));
        legacy.remove("ScribingJob");
        var oldIdle = decode(value.table, legacy);
        var normalized = oldIdle.saveWithFullMetadata(fixtures.level.registryAccess());
        check.accept("legacy idle settings normalize away while inventory survives",
            oldIdle.status() == ScribingChecklist.Status.IDLE && oldIdle.getItem(1).getCount() == 2
                && !normalized.contains("ReplaceMode") && !normalized.contains("ReplaceBlockEntities"));
        var oldDefault = saved.copy();
        oldDefault.putInt("ReplaceMode", 2);
        oldDefault.putBoolean("ReplaceBlockEntities", false);
        check.accept("legacy default job resumes its exact loaded-tick progress", decode(value.table, oldDefault).progressTicks() == 10);
        for (int kind = 0; kind < 8; kind++) {
            var bad = saved.copy();
            var job = bad.getCompound("ScribingJob");
            switch (kind) {
                case 0 -> job.putInt("Elapsed", -1);
                case 1 -> job.putInt("Elapsed", 21);
                case 2 -> job.remove("Medium");
                case 3 -> job.putInt("Mode", 3);
                case 4 -> job.remove("ReplaceEntities");
                case 5 -> job.put("Medium", new ItemStack(Items.BOOK, 3).save(fixtures.level.registryAccess()));
                case 6 -> bad.putString("ScribingJob", "wrong type");
                case 7 -> job.putBoolean("ReplaceEntities", true);
                default -> throw new AssertionError();
            }
            var rejected = decode(value.table, bad);
            check.accept("malformed or mismatched job fails closed " + kind, !rejected.isPrinting()
                && rejected.status() == ScribingChecklist.Status.INPUT_CHANGED && rejected.getItem(1).getCount() == 2 && rejected.getItem(2).isEmpty());
        }
        restored.setItem(2, new ItemStack(Items.WRITTEN_BOOK));
        var occupied = restored.saveWithFullMetadata(fixtures.level.registryAccess());
        occupied.put("ScribingJob", saved.getCompound("ScribingJob").copy());
        var rejected = decode(value.table, occupied);
        check.accept("saved occupied output cannot resume a job", !rejected.isPrinting() && rejected.getItem(1).getCount() == 2
            && rejected.getItem(2).getCount() == 1);
    }

    private ScribingTableBlockEntity decode(ScribingTableBlockEntity source, CompoundTag tag) {
        var result = new ScribingTableBlockEntity(source.getBlockPos(), source.getBlockState());
        result.loadWithComponents(tag, fixtures.level.registryAccess());
        return result;
    }

    private void freshAtFinish() throws Exception {
        var value = fresh();
        value.table.setItem(1, AllBlocks.CLIPBOARD.asStack());
        value.menu.clickMenuButton(value.actor, ScribingTableMenu.PRINT);
        ScribingFixtures.advance(value.table, 20);
        fixtures.writeTemplate(true);
        value.table.serverTick();
        var entries = ClipboardEntry.readAll(value.table.getItem(2)).stream().flatMap(java.util.Collection::stream).toList();
        check.accept("completion reads a same-name file replaced during the job", entries.stream().anyMatch(entry -> entry.icon.is(Items.DIRT))
            && entries.stream().noneMatch(entry -> entry.icon.is(Items.STONE)));
        fixtures.writeTemplate();
        var changedWorld = fresh();
        changedWorld.table.setItem(1, AllBlocks.CLIPBOARD.asStack());
        changedWorld.menu.clickMenuButton(changedWorld.actor, ScribingTableMenu.PRINT);
        ScribingFixtures.advance(changedWorld.table, 20);
        fixtures.level.setBlock(ScribingFixtures.ANCHOR.east(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        changedWorld.table.serverTick();
        check.accept("completion samples target blocks changed during the job", ClipboardEntry.readAll(changedWorld.table.getItem(2)).stream()
            .flatMap(java.util.Collection::stream).anyMatch(entry -> entry.icon.is(Items.STONE) && entry.itemAmount == 1));
        fixtures.level.removeBlock(ScribingFixtures.ANCHOR.east(), false);
        var missing = fresh();
        start(missing);
        ScribingFixtures.advance(missing.table, 20);
        fixtures.deleteTemplate();
        missing.table.serverTick();
        check.accept("file deleted during job fails without consumption", missing.table.status() == ScribingChecklist.Status.UNREADABLE
            && !missing.table.isPrinting() && missing.table.getItem(1).getCount() == 2 && missing.table.getItem(2).isEmpty());
        fixtures.writeTemplate();
    }

    private void reentrancyAndFaults() {
        var callback = fresh();
        start(callback);
        ScribingFixtures.advance(callback.table, 20);
        ScribingPrintFaults.arm(() -> {
            callback.table.setItem(1, new ItemStack(Items.BOOK, 3));
            callback.table.setItem(1, new ItemStack(Items.BOOK, 2));
            callback.menu.clickMenuButton(callback.actor, ScribingTableMenu.PRINT);
            callback.table.serverTick();
        });
        callback.table.serverTick();
        check.accept("callback cancel/restore and nested start cannot publish or replace a job", !callback.table.isPrinting()
            && callback.table.status() == ScribingChecklist.Status.INPUT_CHANGED && callback.table.getItem(1).getCount() == 2
            && callback.table.getItem(2).isEmpty());
        for (boolean fatal : new boolean[]{false, true}) {
            var value = fresh();
            start(value);
            ScribingFixtures.advance(value.table, 20);
            ScribingPrintFaults.arm(() -> {
                if (fatal) throw new AssertionError("expected timed-print Error");
                throw new IllegalStateException("expected timed-print RuntimeException");
            });
            boolean rethrown = false;
            try { value.table.serverTick(); }
            catch (AssertionError expected) { rethrown = true; }
            finally { ScribingPrintFaults.clear(); }
            check.accept("generation fault consumes nothing and Error is rethrown " + fatal,
                rethrown == fatal && !value.table.isPrinting() && value.table.status() == ScribingChecklist.Status.FAILED
                    && value.table.getItem(1).getCount() == 2 && value.table.getItem(2).isEmpty());
        }
        var inner = fresh();
        start(inner);
        ScribingFixtures.advance(inner.table, 20);
        ScribingPrintFaults.armDuring(() -> { throw new IllegalStateException("expected native-work fault"); });
        inner.table.serverTick();
        check.accept("exception within native-work try path fails without consumption", !inner.table.isPrinting()
            && inner.table.status() == ScribingChecklist.Status.FAILED && inner.table.getItem(1).getCount() == 2 && inner.table.getItem(2).isEmpty());
        var after = fresh();
        start(after);
        ScribingFixtures.advance(after.table, 20);
        var external = new ItemStack(Items.WRITTEN_BOOK);
        external.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("External result"));
        ScribingPrintFaults.armAfter(() -> {
            after.table.setItem(2, external);
            after.menu.clickMenuButton(after.actor, ScribingTableMenu.PRINT);
        });
        after.table.serverTick();
        check.accept("post-generation output replacement wins over native success", !after.table.isPrinting()
            && after.table.status() == ScribingChecklist.Status.INPUT_CHANGED && after.table.getItem(1).getCount() == 2
            && ItemStack.matches(after.table.getItem(2), external));
    }

    private void removalAndTransfer() {
        var broken = fresh();
        start(broken);
        ScribingFixtures.advance(broken.table, 8);
        var pos = broken.table.getBlockPos();
        fixtures.level.removeBlock(pos, false);
        ScribingFixtures.complete(broken.table);
        var drops = fixtures.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        check.accept("busy removal cancels before exactly the unconsumed inputs drop", !broken.table.isPrinting()
            && drops.stream().map(ItemEntity::getItem).mapToInt(ItemStack::getCount).sum() == 3
            && drops.stream().noneMatch(entity -> entity.getItem().is(Items.WRITTEN_BOOK)));
        drops.forEach(ItemEntity::discard);
        var transfer = fresh();
        start(transfer);
        ScribingFixtures.advance(transfer.table, 8);
        ScribingMenuAcceptance.quickMove(transfer.actor, transfer.menu, 1);
        ScribingFixtures.complete(transfer.table);
        check.accept("native quick-move during work cancels and conserves the medium", !transfer.table.isPrinting()
            && transfer.table.getItem(1).isEmpty() && transfer.table.getItem(2).isEmpty()
            && transfer.actor.getInventory().items.stream().filter(stack -> stack.is(Items.BOOK)).mapToInt(ItemStack::getCount).sum() == 2);
    }
}
