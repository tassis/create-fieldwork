package dev.tassis.createfieldwork.schematic.asset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tassis.createfieldwork.schematic.table.ScribingChecklist;
import dev.tassis.createfieldwork.schematic.table.ScribingTableLayout;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ScribingUiResourcesTest {
    @Test
    void originalSkinHasTransparentOutsideEdgesAndAllItemWellsAreEmpty() throws IOException {
        try (InputStream input = resource("/assets/create_fieldwork/textures/gui/schematic_scribing_table.png")) {
            var skin = ImageIO.read(input);
            assertNotNull(skin);
            assertEquals(256, skin.getWidth());
            assertEquals(256, skin.getHeight());
            assertTrue(skin.getColorModel().hasAlpha());
            assertEquals(0, skin.getRGB(0, 0) >>> 24);
            assertEquals(0, skin.getRGB(240, 250) >>> 24);
            for (int index = 0; index < 39; index++) {
                var point = index < 3 ? ScribingTableLayout.tableSlot(index) : ScribingTableLayout.playerSlot(index - 3);
                for (int y = point.y(); y < point.y() + 16; y++) {
                    for (int x = point.x(); x < point.x() + 16; x++) {
                        assertEquals(0xffa6a79a, skin.getRGB(x, y), "No baked item/icon/count graphics in native wells");
                    }
                }
            }
            assertEquals(0xfff1efdd, skin.getRGB(14, 125), "Status indicator must be dynamic, not baked green");
            assertEquals(0xff577987, skin.getRGB(16, 76), "Selected lighter blueprint blue");
            assertEquals(0xffefe5cf, skin.getRGB(50, 8), "Title is blank paper, not baked English text");
        }
    }

    @Test
    void bothLocalesContainAllDynamicHintsAndShortStatuses() throws IOException {
        for (String locale : new String[]{"en_us", "zh_tw"}) {
            try (var reader = new InputStreamReader(resource("/assets/create_fieldwork/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
                JsonObject language = JsonParser.parseReader(reader).getAsJsonObject();
                String prefix = "gui.create_fieldwork.scribing.";
                assertEquals(locale.equals("en_us") ? "Book / Clipboard" : "書本／剪貼簿",
                    language.get(prefix + "slot.medium").getAsString());
                assertTrue(language.get(prefix + "overwrite").getAsString().length() > 20,
                    "Keep the full overwrite/consumption warning on Print, not on the media slot");
                for (String key : new String[]{"print", "media_label", "output_label", "workflow",
                    "hint.schematic", "hint.medium", "hint.ready"}) {
                    assertTrue(language.has(prefix + key), locale + "/" + key);
                }
                for (var status : ScribingChecklist.Status.values()) {
                    if (status != ScribingChecklist.Status.IDLE) {
                        assertTrue(language.has(prefix + "short." + status.name().toLowerCase(Locale.ROOT)));
                    }
                }
            }
        }
    }

    private static InputStream resource(String path) {
        var input = ScribingUiResourcesTest.class.getResourceAsStream(path);
        assertNotNull(input, path);
        return input;
    }
}
