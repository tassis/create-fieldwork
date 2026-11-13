package dev.tassis.createfieldwork.schematic.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Resource invariants, not a claim that Minecraft's client has baked the models. */
class ScribingTableAssetsTest {
    private static final String ROOT = "/assets/create_fieldwork/";
    private static final String ID = "schematic_scribing_table";
    private static final List<String> SUFFIXES = List.of("", "_schematic", "_clipboard", "_schematic_clipboard");
    private static final Set<String> DIRECTIONS = Set.of("up", "down", "north", "south", "east", "west");
    private static final double EPSILON = 0.000001;

    @Test
    void allSixteenBlockstatesSelectTheRightInputModelsAndFacing() throws IOException {
        JsonObject variants = json(ROOT + "blockstates/" + ID + ".json").getAsJsonObject("variants");
        assertEquals(16, variants.size());
        String[] facings = {"south", "west", "north", "east"};
        for (int turn = 0; turn < facings.length; turn++) {
            for (boolean schematic : List.of(false, true)) {
                for (boolean clipboard : List.of(false, true)) {
                    String key = "facing=" + facings[turn] + ",schematic=" + schematic + ",clipboard=" + clipboard;
                    JsonObject variant = variants.getAsJsonObject(key);
                    String suffix = (schematic ? "_schematic" : "") + (clipboard ? "_clipboard" : "");
                    assertEquals("create_fieldwork:block/" + ID + suffix, variant.get("model").getAsString());
                    assertEquals(turn * 90, variant.has("y") ? variant.get("y").getAsInt() : 0);
                }
            }
        }
    }

    @Test
    void modelsUseVanillaParentsAndAnEmptyDeskInventoryModel() throws IOException {
        for (String suffix : SUFFIXES) {
            JsonObject model = model(suffix);
            assertEquals("minecraft:block/block", model.get("parent").getAsString());
            assertTrue(model.get("ambientocclusion").getAsBoolean());
            assertEquals("front", model.get("gui_light").getAsString());
        }
        JsonObject item = json(ROOT + "models/item/" + ID + ".json");
        assertEquals("create_fieldwork:block/" + ID, item.get("parent").getAsString());
    }

    @Test
    void everyTextureAndFaceReferenceResolvesToAPixelPng() throws IOException {
        Set<String> used = new HashSet<>();
        Set<String> declared = new HashSet<>();
        for (String suffix : SUFFIXES) {
            JsonObject model = model(suffix);
            JsonObject textures = model.getAsJsonObject("textures");
            for (var entry : textures.entrySet()) {
                String value = entry.getValue().getAsString();
                assertTrue(value.startsWith("create_fieldwork:block/" + ID + "/"),
                    "Desk materials must remain local to Fieldwork: " + value);
                String path = "/assets/" + value.replace(":", "/textures/") + ".png";
                try (InputStream input = resource(path)) {
                    var image = ImageIO.read(input);
                    assertNotNull(image, path);
                    assertEquals(image.getWidth(), image.getHeight(), path);
                    assertTrue(Set.of(16, 32).contains(image.getWidth()), path);
                    assertFalse(image.getColorModel().hasAlpha(), "Fieldwork surfaces need no transparency");
                }
                if (!entry.getKey().equals("particle")) {
                    declared.add(entry.getKey());
                }
            }
            for (JsonElement element : model.getAsJsonArray("elements")) {
                JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                assertEquals(DIRECTIONS, faces.keySet(), "Open frame parts must retain all visible inner faces");
                for (var entry : faces.entrySet()) {
                    JsonObject face = entry.getValue().getAsJsonObject();
                    String texture = face.get("texture").getAsString();
                    assertTrue(texture.startsWith("#"));
                    assertTrue(textures.has(texture.substring(1)), texture);
                    used.add(texture.substring(1));
                    assertFalse(face.has("cullface"), "An open desk cannot assume solid neighboring geometry");
                    JsonArray uv = face.getAsJsonArray("uv");
                    assertEquals(4, uv.size());
                    for (JsonElement coordinate : uv) {
                        assertTrue(coordinate.getAsDouble() >= 0 && coordinate.getAsDouble() <= 16);
                    }
                    assertTrue(uv.get(0).getAsDouble() < uv.get(2).getAsDouble());
                    assertTrue(uv.get(1).getAsDouble() < uv.get(3).getAsDouble());
                }
            }
        }
        assertEquals(8, used.size());
        assertEquals(declared, used, "Every declared material is used by a delivered state");
    }

    @Test
    void desktopUsesTheLocalWoodSpriteWithFullUvs() throws IOException {
        for (String suffix : SUFFIXES) {
            JsonObject model = model(suffix);
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals("create_fieldwork:block/" + ID + "/wood_top", textures.get("wood_top").getAsString());
            assertEquals("create_fieldwork:block/" + ID + "/iron", textures.get("particle").getAsString());
            JsonObject top = named(model, "desktop_inset_wood").getAsJsonObject("faces").getAsJsonObject("up");
            assertEquals("#wood_top", top.get("texture").getAsString());
            assertEquals(List.of(0.0, 0.0, 16.0, 16.0), top.getAsJsonArray("uv")
                .asList().stream().map(JsonElement::getAsDouble).toList());
        }
        try (InputStream input = resource(ROOT + "textures/block/" + ID + "/wood_top.png")) {
            var wood = ImageIO.read(input);
            assertNotNull(wood);
            assertEquals(16, wood.getWidth());
            assertEquals(16, wood.getHeight());
        }
        assertNull(ScribingTableAssetsTest.class.getResource(ROOT + "textures/block/" + ID + "/andesite_casing.png"),
            "Upstream casing must not be copied into Fieldwork");
    }

    @Test
    void selectedFinishUsesGoldAndOnePlainNailWithoutOldBrass() throws IOException {
        for (String suffix : SUFFIXES) {
            JsonObject textures = model(suffix).getAsJsonObject("textures");
            assertEquals("create_fieldwork:block/" + ID + "/gold", textures.get("gold").getAsString());
            assertFalse(textures.has("brass"));
        }
        try (InputStream input = resource(ROOT + "textures/block/" + ID + "/gold.png")) {
            var gold = ImageIO.read(input);
            assertNotNull(gold);
            assertEquals(16, gold.getWidth());
            assertEquals(16, gold.getHeight());
            int nailPixels = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int rgb = gold.getRGB(x, y) & 0xffffff;
                    if (rgb == 0x4c5146) {
                        nailPixels++;
                        assertTrue(x >= 7 && x < 9 && y >= 7 && y < 9);
                    }
                    assertNotEquals(0xfdeba1, rgb, "No miniature white stud glint");
                }
            }
            assertEquals(4, nailPixels);
        }
        assertNull(ScribingTableAssetsTest.class.getResource(ROOT + "textures/block/" + ID + "/brass.png"));
    }

    @Test
    void everyRotatedCornerRemainsWithinOneBlock() throws IOException {
        for (String suffix : SUFFIXES) {
            Set<String> names = new HashSet<>();
            for (JsonElement entry : model(suffix).getAsJsonArray("elements")) {
                JsonObject element = entry.getAsJsonObject();
                String name = element.get("name").getAsString();
                assertTrue(names.add(name), "Duplicate part: " + name);
                double[] low = vector(element.getAsJsonArray("from"));
                double[] high = vector(element.getAsJsonArray("to"));
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(low[axis] < high[axis], name);
                }
                for (int corner = 0; corner < 8; corner++) {
                    double[] point = new double[3];
                    for (int axis = 0; axis < 3; axis++) {
                        point[axis] = (corner & (1 << axis)) == 0 ? low[axis] : high[axis];
                    }
                    point = rotate(point, element, false);
                    for (double coordinate : point) {
                        assertTrue(Double.isFinite(coordinate) && coordinate >= -EPSILON && coordinate <= 16 + EPSILON,
                            name + " extends outside a block: " + coordinate);
                    }
                }
            }
        }
    }

    @Test
    void desktopAndInsertedItemsShareOneLegalNativeSlope() throws IOException {
        JsonObject model = model("_schematic_clipboard");
        JsonObject slope = named(model, "desktop_inset_wood").getAsJsonObject("rotation");
        assertEquals("x", slope.get("axis").getAsString());
        assertEquals(22.5, slope.get("angle").getAsDouble());
        assertArrayEquals(new double[]{8, 13, 8}, vector(slope.getAsJsonArray("origin")));
        assertFalse(slope.get("rescale").getAsBoolean());
        for (JsonElement entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            if (element.has("rotation")) {
                assertEquals(slope, element.getAsJsonObject("rotation"));
            }
        }
        double[] back = rotate(new double[]{8, 13, 2}, named(model, "desktop_inset_wood"), false);
        double[] front = rotate(new double[]{8, 13, 14}, named(model, "desktop_inset_wood"), false);
        assertTrue(back[1] > front[1], "The north/back edge must be taller");
    }

    @Test
    void fourInputStatesPreserveExactlyTheSameDesk() throws IOException {
        JsonArray base = model("").getAsJsonArray("elements");
        for (String suffix : SUFFIXES) {
            JsonArray elements = model(suffix).getAsJsonArray("elements");
            Set<String> inputs = new HashSet<>();
            if (suffix.contains("schematic")) {
                inputs.add("inserted_schematic");
            }
            if (suffix.contains("clipboard")) {
                inputs.add("inserted_clipboard");
            }
            assertEquals(base.size() + inputs.size(), elements.size());
            for (int i = 0; i < base.size(); i++) {
                assertEquals(base.get(i), elements.get(i));
            }
            Set<String> actualInputs = new HashSet<>();
            for (int i = base.size(); i < elements.size(); i++) {
                actualInputs.add(elements.get(i).getAsJsonObject().get("name").getAsString());
            }
            assertEquals(inputs, actualInputs);
        }
    }

    @Test
    void paperIsSeparatedFromTimberAndBlueprintIsLeftOfClipboard() throws IOException {
        JsonObject model = model("_schematic_clipboard");
        double desktopTop = named(model, "desktop_inset_wood").getAsJsonArray("to").get(1).getAsDouble();
        JsonObject blueprint = named(model, "inserted_schematic");
        JsonObject clipboard = named(model, "inserted_clipboard");
        for (JsonObject item : List.of(blueprint, clipboard)) {
            assertTrue(item.getAsJsonArray("from").get(1).getAsDouble() > desktopTop + .01,
                "Paper must not be coplanar with timber");
            assertEquals(List.of(0.0, 0.0, 16.0, 16.0),
                item.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv")
                    .asList().stream().map(JsonElement::getAsDouble).toList());
        }
        assertTrue(blueprint.getAsJsonArray("to").get(0).getAsDouble()
            < clipboard.getAsJsonArray("from").get(0).getAsDouble());
    }

    @Test
    void centerUnderTheDesktopIsActuallyEmptyGeometry() throws IOException {
        JsonArray elements = model("_schematic_clipboard").getAsJsonArray("elements");
        for (double x = 6; x <= 10; x++) {
            for (double y = 6.5; y <= 9.5; y++) {
                for (double z = 5; z <= 10; z++) {
                    for (JsonElement entry : elements) {
                        JsonObject element = entry.getAsJsonObject();
                        double[] point = rotate(new double[]{x, y, z}, element, true);
                        double[] low = vector(element.getAsJsonArray("from"));
                        double[] high = vector(element.getAsJsonArray("to"));
                        boolean inside = true;
                        for (int axis = 0; axis < 3; axis++) {
                            inside &= point[axis] >= low[axis] && point[axis] <= high[axis];
                        }
                        assertFalse(inside, "Under-desk void occupied by " + element.get("name"));
                    }
                }
            }
        }
    }

    private static double[] rotate(double[] point, JsonObject element, boolean inverse) {
        if (!element.has("rotation")) {
            return point;
        }
        JsonObject rotation = element.getAsJsonObject("rotation");
        assertEquals("x", rotation.get("axis").getAsString());
        assertEquals(22.5, rotation.get("angle").getAsDouble());
        assertFalse(rotation.get("rescale").getAsBoolean());
        double[] origin = vector(rotation.getAsJsonArray("origin"));
        double angle = Math.toRadians(rotation.get("angle").getAsDouble() * (inverse ? -1 : 1));
        double y = point[1] - origin[1];
        double z = point[2] - origin[2];
        return new double[]{point[0], origin[1] + y * Math.cos(angle) - z * Math.sin(angle),
            origin[2] + y * Math.sin(angle) + z * Math.cos(angle)};
    }

    private static double[] vector(JsonArray array) {
        assertEquals(3, array.size());
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    private static JsonObject named(JsonObject model, String name) {
        return model.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject)
            .filter(element -> name.equals(element.get("name").getAsString())).findFirst().orElseThrow();
    }

    private static JsonObject model(String suffix) throws IOException {
        return json(ROOT + "models/block/" + ID + suffix + ".json");
    }

    private static JsonObject json(String path) throws IOException {
        try (var reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream input = ScribingTableAssetsTest.class.getResourceAsStream(path);
        assertNotNull(input, "Missing classpath resource " + path);
        return input;
    }
}
