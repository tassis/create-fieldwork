package dev.tassis.createfieldwork.pondersmoke;

import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.schematic.client.ScribingTableScreen;
import dev.tassis.createfieldwork.schematic.table.ScribingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;

/** Open the real table menu through the integrated server; this is not a table Ponder scene. */
final class ScribingScreenChecks {
    private static boolean requested;
    private static int frames;

    private ScribingScreenChecks() {}

    static boolean capture() throws Exception {
        var mc = Minecraft.getInstance();
        if (!requested) {
            requested = true;
            var id = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                var pos = player.blockPosition().offset(2, 0, 2);
                level.setBlock(pos, FieldworkContent.SCRIBING_TABLE.get().defaultBlockState(), 3);
                var table = (ScribingTableBlockEntity) level.getBlockEntity(pos);
                table.setItem(1, new ItemStack(Items.BOOK, 2));
                player.openMenu(table, pos);
            });
            return false;
        }
        if (!(mc.screen instanceof ScribingTableScreen screen)) return false;
        var buttons = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        if (buttons.size() != 1 || !buttons.getFirst().getMessage().getString()
            .equals(Component.translatable("gui.create_fieldwork.scribing.print").getString())) {
            throw new AssertionError("Table must expose Print only, without Settings");
        }
        if (!screen.getMenu().slots.stream().allMatch(Slot::isActive)) {
            throw new AssertionError("Removed settings overlay still hides menu slots");
        }
        if (++frames == 15) {
            Path folder = folder();
            Files.createDirectories(folder);
            Screenshot.grab(folder.toFile(), "scribing-table-no-settings.png", mc.getMainRenderTarget(), message -> {});
        }
        if (frames < 25) return false;
        mc.player.closeContainer();
        return true;
    }

    static Path folder() {
        String mode = Boolean.getBoolean("fieldwork.ponder.expectedIntegrated") ? "integrated" : "base";
        return Path.of(System.getProperty("fieldwork.ponder.report")).getParent().resolve("ponder-screens")
            .resolve(mode + "-" + System.getProperty("fieldwork.ponder.language", "en_us"));
    }
}
