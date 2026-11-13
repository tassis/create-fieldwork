package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import dev.tassis.createfieldwork.schematic.table.ScribingTableBlock;
import dev.tassis.createfieldwork.schematic.table.ScribingTableBlockEntity;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.function.BiConsumer;

/** Real files, native cannon/checklist output and production-menu actions; no GUI claims. */
final class ScribingTableAcceptance {
    private final ScribingFixtures fixtures;
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    private ScribingTableBlockEntity table;
    private ServerPlayer actor;
    private ScribingTableMenu menu;
    private int resumeTick = -1;
    private final boolean reload;
    private ItemStack blueprintBefore;
    private ScribingTableBlockEntity resuming;
    private int lastProgress;
    static final BlockPos PERSISTING = ScribingFixtures.TABLE.west(24);

    ScribingTableAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) {
        this(level, check, false);
    }

    ScribingTableAcceptance(ServerLevel level, BiConsumer<String, Boolean> check, boolean reload) {
        this.level = level;
        this.fixtures = new ScribingFixtures(level);
        this.check = check;
        this.reload = reload;
    }

    boolean tick(int tick) throws Exception {
        if (reload) return reloadTick(tick);
        if (resumeTick == -1) {
            fixtures.writeTemplate();
            nativeParity();
            invalidAndPartial();
            shapesAndDrops();
            mediaTransactions();
            ScribingMenuAcceptance.run(fixtures, check);
            ScribingTimedAcceptance.run(fixtures, check);
            initialTransaction();
            resumeTick = tick + 60;
            lastProgress = 0;
            return false;
        }
        if (table.isPrinting()) {
            int progress = table.progressTicks();
            if (progress < lastProgress || progress > lastProgress + 1 || progress > 20
                || table.getItem(1).getCount() != 3 || !table.getItem(2).isEmpty() || tick >= resumeTick) {
                throw new AssertionError("natural ticker/progress/input invariant: " + progress);
            }
            if (progress > lastProgress) check.accept("natural active tick has no preconsumption " + progress, true);
            lastProgress = progress;
            return false;
        }
        check.accept("async fixture retains the same loaded table", level.getBlockEntity(ScribingFixtures.TABLE) == table);
        check.accept("real BE ticker completes exactly21 active ticks", lastProgress == 20 && !table.isPrinting()
            && table.status() == ScribingChecklist.Status.PRINTED && table.getItem(1).getCount() == 2
            && table.getItem(2).is(Items.WRITTEN_BOOK) && !menu.isPrinting());
        check.accept("schematic stays intact after timed completion", ItemStack.matches(blueprintBefore, table.getItem(0)));
        menu.clickMenuButton(actor, ScribingTableMenu.PRINT);
        check.accept("occupied output cannot consume another input", table.status() == ScribingChecklist.Status.OUTPUT_FULL
            && table.getItem(1).getCount() == 2);
        table.removeItem(2, 1);
        check.accept("taking an output clears old status", table.status() == ScribingChecklist.Status.IDLE);
        level.setBlock(ScribingFixtures.ANCHOR.east(), Blocks.STONE.defaultBlockState(), 3);
        check.accept("second authorized menu action accepted", menu.clickMenuButton(actor, ScribingTableMenu.PRINT));
        ScribingFixtures.complete(table);
        check.accept("a new print resamples the changed world", table.status() == ScribingChecklist.Status.PRINTED);
        check.accept("second print consumes one book only", table.getItem(1).getCount() == 1 && table.getItem(2).is(Items.WRITTEN_BOOK));
        menu.clickMenuButton(actor, ScribingTableMenu.CYCLE_MODE);
        menu.clickMenuButton(actor, ScribingTableMenu.TOGGLE_BLOCK_ENTITIES);
        check.accept("settings are server owned", menu.replaceMode() == 3 && menu.replaceBlockEntities());
        var saved = table.saveWithFullMetadata(level.registryAccess());
        var restored = new ScribingTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.loadWithComponents(saved, level.registryAccess());
        check.accept("all three slots round-trip through actual BE NBT", ItemStack.matches(restored.getItem(0), table.getItem(0))
            && ItemStack.matches(restored.getItem(1), table.getItem(1)) && ItemStack.matches(restored.getItem(2), table.getItem(2)));
        var active = fixtures.table(PERSISTING);
        fixtures.pin(PERSISTING);
        active.setItem(0, fixtures.blueprint(ScribingFixtures.ANCHOR));
        active.setItem(1, new ItemStack(Items.BOOK, 2));
        var activeActor = fixtures.actor(PERSISTING);
        var activeMenu = fixtures.menu(active, activeActor);
        activeMenu.clickMenuButton(activeActor, ScribingTableMenu.CYCLE_MODE);
        activeMenu.clickMenuButton(activeActor, ScribingTableMenu.TOGGLE_BLOCK_ENTITIES);
        activeMenu.clickMenuButton(activeActor, ScribingTableMenu.PRINT);
        ScribingFixtures.advance(active, 10);
        check.accept("seed leaves a clean-save mid-job snapshot without preconsumption", active.isPrinting()
            && active.progressTicks() == 10 && active.getItem(1).getCount() == 2 && active.getItem(2).isEmpty());
        fixtures.close();
        return true;
    }

    private void nativeParity() {
        var service = new ScribingChecklist();
        int scenario = 0;
        for (Mirror mirror : Mirror.values()) {
            for (Rotation rotation : Rotation.values()) {
                BlockPos anchor = ScribingFixtures.ANCHOR.offset(scenario++ * 8, 0, 0);
                level.getChunkAt(anchor);
                level.getChunkAt(anchor.offset(3, 0, 3));
                level.setBlock(anchor, Blocks.STONE.defaultBlockState(), 3);
                var blueprint = fixtures.blueprint(anchor);
                blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, rotation);
                blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, mirror);
                for (int mode = 0; mode < 4; mode++) {
                    var result = service.print(level, ScribingFixtures.TABLE, blueprint, AllBlocks.CLIPBOARD.asStack(), mode, false);
                    ItemStack expected = fixtures.nativeOutput(blueprint, true, mode, false, ScribingFixtures.TABLE.south(5));
                    check.accept("native clipboard parity " + mirror + "/" + rotation + "/" + mode,
                        result.status() == ScribingChecklist.Status.PRINTED && ItemStack.isSameItemSameComponents(result.output(), expected));
                }
                check.accept("checklist generation never replaces target blocks " + scenario, level.getBlockState(anchor).is(Blocks.STONE));
            }
        }
        var blueprint = fixtures.blueprint(ScribingFixtures.ANCHOR);
        var missing = service.print(level, ScribingFixtures.TABLE, blueprint, AllBlocks.CLIPBOARD.asStack(), 2, false);
        var stone = entries(missing.output()).stream().filter(entry -> entry.icon.is(Items.STONE)).findFirst().orElseThrow();
        check.accept("already built stone is subtracted from the three-stone schematic", stone.itemAmount == 2 && !stone.checked);
        check.accept("native entity requirements are included", entries(missing.output()).stream()
            .anyMatch(entry -> entry.icon.is(Items.ARMOR_STAND) && entry.itemAmount == 1));
        check.accept("checklist generation never spawns the schematic entity in the target world", level.getEntitiesOfClass(
            net.minecraft.world.entity.decoration.ArmorStand.class, new AABB(ScribingFixtures.TABLE).inflate(140)).isEmpty());

        BlockPos supply = ScribingFixtures.TABLE.east();
        level.setBlock(supply, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(supply);
        chest.setItem(0, new ItemStack(Items.STONE, 64));
        var available = service.print(level, ScribingFixtures.TABLE, blueprint, AllBlocks.CLIPBOARD.asStack(), 2, false);
        check.accept("adjacent extractable supply marks native clipboard entry complete", entries(available.output()).stream()
            .anyMatch(entry -> entry.icon.is(Items.STONE) && entry.checked && entry.itemAmount == 0));
        check.accept("inventory availability sampling never consumes building materials", chest.getItem(0).getCount() == 64);
        level.removeBlock(supply, false);
        var book = service.print(level, ScribingFixtures.TABLE, blueprint, new ItemStack(Items.BOOK), 2, false);
        check.accept("book output uses native written-book components", ItemStack.isSameItemSameComponents(book.output(),
            fixtures.nativeOutput(blueprint, false, 2, false, ScribingFixtures.TABLE.south(5))));
        check.accept("native written book is accepted for replacement", ScribingChecklist.isMedium(book.output()));
        level.setBlock(ScribingFixtures.ANCHOR.east(), Blocks.CHEST.defaultBlockState(), 3);
        for (boolean replace : new boolean[]{false, true}) {
            var result = service.print(level, ScribingFixtures.TABLE, blueprint, AllBlocks.CLIPBOARD.asStack(), 2, replace);
            check.accept("existing block entity policy matches cannon " + replace, ItemStack.isSameItemSameComponents(result.output(),
                fixtures.nativeOutput(blueprint, true, 2, replace, ScribingFixtures.TABLE.south(5))));
            check.accept("counting replacement never destroys the existing block entity " + replace,
                level.getBlockEntity(ScribingFixtures.ANCHOR.east()) instanceof ChestBlockEntity);
        }
        level.removeBlock(ScribingFixtures.ANCHOR.east(), false);
        var nearby = service.print(level, ScribingFixtures.ANCHOR, blueprint, AllBlocks.CLIPBOARD.asStack(), 2, false);
        check.accept("workstation counting has no cannon-only two-block firing exclusion", entries(nearby.output()).stream()
            .anyMatch(entry -> entry.icon.is(Items.STONE) && entry.itemAmount == 2));
    }

    private void invalidAndPartial() throws Exception {
        var service = new ScribingChecklist();
        var blueprint = fixtures.blueprint(ScribingFixtures.ANCHOR);
        check.accept("valid template loads before invalid input control", !service.print(level, ScribingFixtures.TABLE,
            blueprint, new ItemStack(Items.BOOK), 2, false).output().isEmpty());
        blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        check.accept("false deployment cannot reuse a previous valid template", service.print(level, ScribingFixtures.TABLE,
            blueprint, new ItemStack(Items.BOOK), 2, false).status() == ScribingChecklist.Status.UNDEPLOYED);
        blueprint = fixtures.blueprint(ScribingFixtures.ANCHOR);
        blueprint.set(AllDataComponents.SCHEMATIC_FILE, "missing-file.nbt");
        check.accept("missing server file is not printed as an empty valid list", service.print(level, ScribingFixtures.TABLE,
            blueprint, new ItemStack(Items.BOOK), 2, false).output().isEmpty());
        blueprint = fixtures.blueprint(ScribingFixtures.TABLE.east(300));
        check.accept("native anchor range is enforced", service.print(level, ScribingFixtures.TABLE,
            blueprint, new ItemStack(Items.BOOK), 2, false).status() == ScribingChecklist.Status.TOO_FAR);
        BlockPos unloaded = ScribingFixtures.TABLE.offset(-200, 0, 64);
        check.accept("partial fixture target is initially unloaded", !level.isLoaded(unloaded));
        var partial = service.print(level, ScribingFixtures.TABLE, fixtures.blueprint(unloaded), AllBlocks.CLIPBOARD.asStack(), 2, false);
        check.accept("unloaded targets yield a warning-bearing partial list", partial.status() == ScribingChecklist.Status.PARTIAL
            && !entries(partial.output()).isEmpty());
        check.accept("printing does not force-load target chunks", !level.isLoaded(unloaded));
        check.accept("invalid media is rejected", !ScribingChecklist.isMedium(new ItemStack(Items.PAPER)));
        blueprint = fixtures.blueprint(ScribingFixtures.ANCHOR);
        fixtures.writeTemplate(true);
        var changed = service.print(level, ScribingFixtures.TABLE, blueprint, AllBlocks.CLIPBOARD.asStack(), 2, false);
        check.accept("same-item same-name file replacement changes requirements", entries(changed.output()).stream()
            .anyMatch(entry -> entry.icon.is(Items.DIRT)) && entries(changed.output()).stream().noneMatch(entry -> entry.icon.is(Items.STONE)));
        fixtures.deleteTemplate();
        var deleted = service.print(level, ScribingFixtures.TABLE, blueprint, new ItemStack(Items.BOOK), 2, false);
        check.accept("same-item file deletion cannot print cached contents", deleted.status() == ScribingChecklist.Status.UNREADABLE
            && deleted.output().isEmpty());
        fixtures.writeTemplate();
    }

    private void shapesAndDrops() {
        for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var state = FieldworkContent.SCRIBING_TABLE.get().defaultBlockState().setValue(ScribingTableBlock.FACING, facing);
            check.accept("under-desk collision is open " + facing, !Shapes.joinIsNotEmpty(state.getCollisionShape(level, ScribingFixtures.TABLE),
                Shapes.box(.4, .4, .4, .6, .55, .6), BooleanOp.AND));
        }
        var dropTable = fixtures.table(ScribingFixtures.TABLE.west(8));
        dropTable.setItem(0, fixtures.blueprint(ScribingFixtures.ANCHOR));
        dropTable.setItem(1, new ItemStack(Items.BOOK, 2));
        dropTable.setItem(2, AllBlocks.CLIPBOARD.asStack());
        check.accept("inventory derives both world model flags", dropTable.getBlockState().getValue(ScribingTableBlock.SCHEMATIC)
            && dropTable.getBlockState().getValue(ScribingTableBlock.CLIPBOARD));
        BlockPos pos = dropTable.getBlockPos();
        level.removeBlock(pos, false);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        check.accept("removing a table releases all inventory contents", drops.stream().map(ItemEntity::getItem)
            .mapToInt(ItemStack::getCount).sum() == 4);
        drops.forEach(ItemEntity::discard);
    }

    private void initialTransaction() {
        fixtures.pin(ScribingFixtures.TABLE);
        fixtures.pin(ScribingFixtures.ANCHOR);
        table = fixtures.table(ScribingFixtures.TABLE);
        actor = fixtures.actor(ScribingFixtures.TABLE);
        actor.openMenu(table, table.getBlockPos());
        check.accept("native server menu opening selects the registered table menu", actor.containerMenu instanceof ScribingTableMenu
            && actor.containerMenu.getType() == FieldworkContent.SCRIBING_TABLE_MENU.get());
        menu = (ScribingTableMenu) actor.containerMenu;
        check.accept("registered table has three slots plus player inventory", menu.slots.size() == 39);
        check.accept("menu filters reject invalid inputs and all manual output insertions", !menu.getSlot(0).mayPlace(new ItemStack(Items.PAPER))
            && !menu.getSlot(1).mayPlace(new ItemStack(Items.PAPER)) && !menu.getSlot(2).mayPlace(new ItemStack(Items.BOOK)));
        var recipe = level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            "create_fieldwork", "schematic_scribing_table")).orElseThrow().value();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
            ItemStack.EMPTY, new ItemStack(Items.BOOK), ItemStack.EMPTY,
            com.simibubi.create.AllItems.GOLDEN_SHEET.asStack(), AllBlocks.ANDESITE_CASING.asStack(), com.simibubi.create.AllItems.GOLDEN_SHEET.asStack(),
            new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT)));
        check.accept("actual shaped desk recipe matches and outputs one", recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
            && shaped.matches(input, level) && shaped.assemble(input, level.registryAccess()).is(FieldworkContent.SCRIBING_TABLE_ITEM.get())
            && shaped.assemble(input, level.registryAccess()).getCount() == 1);
        var oldIngredients = new java.util.ArrayList<ItemStack>();
        for (int slot = 0; slot < 9; slot++) {
            oldIngredients.add(input.getItem(slot).copy());
        }
        oldIngredients.set(3, com.simibubi.create.AllItems.BRASS_SHEET.asStack());
        oldIngredients.set(5, com.simibubi.create.AllItems.BRASS_SHEET.asStack());
        check.accept("desk recipe rejects the superseded brass-sheet inputs", recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
            && !shaped.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, oldIngredients), level));
        check.accept("unknown menu actions rejected", !menu.clickMenuButton(actor, 99));
        table.setItem(0, fixtures.blueprint(ScribingFixtures.ANCHOR));
        table.setItem(1, new ItemStack(Items.BOOK, 3));
        blueprintBefore = table.getItem(0).copy();
        actor.setPos(0, 120, 0);
        check.accept("distant menu action rejected", !menu.clickMenuButton(actor, ScribingTableMenu.PRINT) && table.getItem(2).isEmpty());
        actor.setPos(ScribingFixtures.TABLE.getX(), 120, ScribingFixtures.TABLE.getZ());
        actor.setGameMode(GameType.SPECTATOR);
        check.accept("spectator print rejected", !menu.clickMenuButton(actor, ScribingTableMenu.PRINT));
        actor.setGameMode(GameType.CREATIVE);
        actor.containerMenu = actor.inventoryMenu;
        check.accept("stale menu identity rejected", !menu.clickMenuButton(actor, ScribingTableMenu.PRINT));
        actor.containerMenu = menu;
        actor.connection.handleContainerButtonClick(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
            menu.containerId, ScribingTableMenu.PRINT));
        check.accept("authorized vanilla packet starts a real server job", table.status() == ScribingChecklist.Status.PRINTING);
        check.accept("start has no immediate output or consumption", table.getItem(2).isEmpty() && table.getItem(1).getCount() == 3);
        menu.clickMenuButton(actor, ScribingTableMenu.PRINT);
        check.accept("same-tick repeat cannot reset or duplicate the active job", table.status() == ScribingChecklist.Status.PRINTING
            && table.progressTicks() == 0 && table.getItem(1).getCount() == 3 && table.getItem(2).isEmpty());
    }

    private void mediaTransactions() {
        for (int variant = 0; variant < 3; variant++) {
            var table = fixtures.table(ScribingFixtures.TABLE.north(8 + variant * 4));
            var actor = fixtures.actor(table.getBlockPos());
            var blueprint = fixtures.blueprint(ScribingFixtures.ANCHOR);
            if (variant == 2) blueprint.set(AllDataComponents.SCHEMATIC_FILE, "missing-transaction.nbt");
            table.setItem(0, blueprint);
            ItemStack medium = variant == 0 ? AllBlocks.CLIPBOARD.asStack() : new ItemStack(Items.WRITTEN_BOOK);
            table.setItem(1, medium);
            var menu = fixtures.menu(table, actor);
            menu.clickMenuButton(actor, ScribingTableMenu.PRINT);
            ScribingFixtures.complete(table);
            if (variant == 2) {
                check.accept("unreadable template transaction preserves its medium", table.getItem(2).isEmpty()
                    && ItemStack.matches(table.getItem(1), medium) && table.status() == ScribingChecklist.Status.UNREADABLE);
            } else {
                check.accept("actual menu prints and consumes clipboard or written book " + variant,
                    table.getItem(1).isEmpty() && (variant == 0 ? !entries(table.getItem(2)).isEmpty()
                        : table.getItem(2).has(DataComponents.WRITTEN_BOOK_CONTENT)));
            }
        }
    }

    private boolean reloadTick(int tick) {
        if (resumeTick >= 0) {
            if (resuming.isPrinting()) {
                int progress = resuming.progressTicks();
                if (progress < lastProgress || progress > lastProgress + 1 || progress > 20
                    || resuming.getItem(1).getCount() != 2 || !resuming.getItem(2).isEmpty() || tick >= resumeTick) {
                    throw new AssertionError("reloaded ticker/progress/input invariant: " + progress);
                }
                if (progress > lastProgress) check.accept("reloaded active tick has no preconsumption " + progress, true);
                lastProgress = progress;
                return false;
            }
            check.accept("reloaded job completes once after remaining11 active ticks", lastProgress == 20 && !resuming.isPrinting()
                && resuming.status() == ScribingChecklist.Status.PRINTED && resuming.getItem(1).getCount() == 1
                && resuming.getItem(2).is(Items.WRITTEN_BOOK) && resuming.getItem(0).getCount() == 1);
            fixtures.close();
            return true;
        }
        level.getChunkAt(ScribingFixtures.TABLE);
        var table = (ScribingTableBlockEntity) level.getBlockEntity(ScribingFixtures.TABLE);
        check.accept("table block entity survives independent JVM reload", table != null);
        check.accept("three persisted slots survive independent reload", table.getItem(0).is(com.simibubi.create.AllItems.SCHEMATIC.get())
            && table.getItem(1).is(Items.BOOK) && table.getItem(1).getCount() == 1 && table.getItem(2).is(Items.WRITTEN_BOOK));
        var actor = fixtures.actor(ScribingFixtures.TABLE);
        var menu = fixtures.menu(table, actor);
        check.accept("settings survive independent reload", menu.replaceMode() == 3 && menu.replaceBlockEntities());
        check.accept("transient status is not restored as success", table.status() == ScribingChecklist.Status.IDLE);
        level.getChunkAt(ScribingFixtures.ANCHOR);
        level.getChunkAt(ScribingFixtures.ANCHOR.offset(3, 0, 3));
        table.removeItem(2, 1);
        menu.clickMenuButton(actor, ScribingTableMenu.PRINT);
        ScribingFixtures.complete(table);
        check.accept("fresh JVM rebuilds printer and prints without consuming schematic", table.status() == ScribingChecklist.Status.PRINTED
            && table.getItem(0).getCount() == 1 && table.getItem(1).isEmpty() && table.getItem(2).is(Items.WRITTEN_BOOK));
        level.getChunkAt(PERSISTING);
        resuming = (ScribingTableBlockEntity) level.getBlockEntity(PERSISTING);
        check.accept("in-flight snapshot survives an independent JVM with exact progress", resuming != null
            && resuming.isPrinting() && resuming.progressTicks() == 10 && resuming.getItem(1).getCount() == 2
            && resuming.getItem(2).isEmpty());
        fixtures.pin(PERSISTING);
        fixtures.pin(ScribingFixtures.ANCHOR);
        var resumedMenu = fixtures.menu(resuming, fixtures.actor(PERSISTING));
        check.accept("in-flight options survive reload", resumedMenu.replaceMode() == 3 && resumedMenu.replaceBlockEntities());
        resumeTick = tick + 40;
        lastProgress = 10;
        return false;
    }

    private static java.util.List<ClipboardEntry> entries(ItemStack clipboard) {
        return ClipboardEntry.readAll(clipboard).stream().flatMap(java.util.Collection::stream).toList();
    }
}
