package dev.tassis.createfieldwork.pondersmoke;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.tassis.createfieldwork.platform.AeronauticsIntegration;
import dev.tassis.createfieldwork.platform.CreateFieldwork;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Separate client driver. Production scenes supply every storyboard and animation. */
@Mod(value = "fieldwork_ponder_smoke", dist = Dist.CLIENT)
@EventBusSubscriber(modid = "fieldwork_ponder_smoke", value = Dist.CLIENT)
public final class PonderSmoke {
    private static final JsonObject RESULT = new JsonObject();
    private static boolean finished;
    private static boolean verified;
    private static int ticks;
    private static boolean languageConfigured;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (finished) return;
        try {
            if (++ticks > 2400) throw new AssertionError("Client verification timed out");
            if (mc.getOverlay() != null || mc.screen == null && mc.level == null) return;
            if (!languageConfigured) {
                languageConfigured = true;
                String language = System.getProperty("fieldwork.ponder.language", "en_us");
                RESULT.addProperty("language", language);
                if (!mc.options.languageCode.equals(language)) {
                    mc.options.languageCode = language;
                    mc.getLanguageManager().setSelected(language);
                    mc.reloadResourcePacks();
                    return;
                }
            }
            if (!PonderVisualSmoke.worldReady()) return;
            if (!verified) {
                verify();
                verified = true;
            }
            if (Boolean.getBoolean("fieldwork.ponder.visual") && !PonderVisualSmoke.capture(RESULT)) return;
            RESULT.addProperty("passed", true);
            finish();
        } catch (Throwable error) {
            error.printStackTrace();
            RESULT.addProperty("passed", false);
            RESULT.addProperty("error", error.toString());
            finish();
        }
    }

    private static void verify() throws Exception {
        boolean integrated = AeronauticsIntegration.active();
        RESULT.addProperty("integrated", integrated);
        RESULT.addProperty("visual", Boolean.getBoolean("fieldwork.ponder.visual"));
        require(integrated == Boolean.getBoolean("fieldwork.ponder.expectedIntegrated"), "Wrong actual integration mode");
        if (!integrated) {
            for (String id : List.of("sable", "aeronautics", "simulated")) {
                require(!ModList.get().isLoaded(id), "Create-only run still loads " + id);
            }
        }
        var english = json("lang/en_us.json");
        var chinese = json("lang/zh_tw.json");
        Map<String, Set<String>> expected = new HashMap<>();
        if (integrated) {
            expected.put("redstone_link_isolator", Set.of("isolator_basics", "isolator_lectern", "smart_controller_binding"));
            expected.put("brass_redstone_link_isolator", Set.of("isolator_basics", "smart_controller_binding"));
        }
        PonderIndex.gatherSharedText();
        var localization = (PonderLocalization) PonderIndex.getLangAccess();
        Map<String, Set<String>> actual = new HashMap<>();
        Set<String> visited = new HashSet<>();
        for (var entry : PonderIndex.getSceneAccess().getRegisteredEntries()) {
            var board = entry.getValue();
            if (!board.getNamespace().equals(CreateFieldwork.MOD_ID)) continue;
            String id = board.getSchematicLocation().getPath();
            require(entry.getKey().getNamespace().equals(CreateFieldwork.MOD_ID), "Unexpected component namespace");
            require(actual.computeIfAbsent(entry.getKey().getPath(), ignored -> new HashSet<>()).add(id), "Duplicate association " + id);
            if (!visited.add(id)) continue;
            PonderSceneRegistry.compileScene(localization, board, null);
            String key = "create_fieldwork.ponder." + id + ".header";
            require(english.get(key).getAsString().equals(localization.specific.get(board.getSchematicLocation()).get("header")), "Title registration " + id);
            require(chinese.has(key) && !chinese.get(key).getAsString().isBlank(), "Missing translated title " + id);
            var template = PonderSceneRegistry.loadSchematic(board.getSchematicLocation());
            require(template.getSize().getX() > 1 && template.getSize().getY() > 1, "Empty structure " + id);
            try (var input = CreateFieldwork.class.getResourceAsStream("/assets/create_fieldwork/ponder/" + id + ".nbt")) {
                var nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
                for (var element : nbt.getList("palette", Tag.TAG_COMPOUND)) {
                    var state = (net.minecraft.nbt.CompoundTag) element;
                    String matcher = state.getString("Name");
                    if (state.contains("Properties")) {
                        var properties = state.getCompound("Properties");
                        matcher += "[" + String.join(",", properties.getAllKeys().stream()
                            .map(k -> k + "=" + properties.getString(k)).toList()) + "]";
                    }
                    BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), matcher, false);
                }
            }
            var scene = PonderIndex.getSceneAccess().compile(List.of(board)).getFirst();
            int stages = switch (id) {
                case "isolator_basics" -> 4;
                case "isolator_lectern" -> 1;
                case "smart_controller_binding" -> 3;
                default -> throw new AssertionError("Unexpected story " + id);
            };
            require(scene.getKeyframeCount() >= stages, "Missing instructional stages " + id);
            play(scene, id);
            scene.reset();
            play(scene, id);
        }
        require(actual.equals(expected), "Wrong item associations " + actual);
        if (integrated) {
            for (var component : List.of("redstone_link_isolator", "brass_redstone_link_isolator")) {
                var chapters = PonderIndex.getSceneAccess().compile(ResourceLocation.fromNamespaceAndPath(CreateFieldwork.MOD_ID, component));
                var expectedOrder = component.equals("redstone_link_isolator")
                    ? List.of("isolator_basics", "isolator_lectern", "smart_controller_binding")
                    : List.of("isolator_basics", "smart_controller_binding");
                require(chapters.stream().map(scene -> scene.getId().getPath()).toList()
                    .equals(expectedOrder), "Wrong shared/specific chapter order " + component);
            }
        }
        String prefix = "create_fieldwork.ponder.shared.";
        for (var entry : english.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            String key = entry.getKey().substring(prefix.length());
            require(entry.getValue().getAsString().equals(localization.shared.get(ResourceLocation.fromNamespaceAndPath(CreateFieldwork.MOD_ID, key))), "Shared caption " + key);
            require(chinese.has(entry.getKey()) && !chinese.get(entry.getKey()).getAsString().isBlank(), "Translated caption " + key);
        }
        RESULT.addProperty("scenes", visited.size());
        RESULT.addProperty("replays", visited.size());
        RESULT.addProperty("associations", actual.toString());
        RESULT.addProperty("source", CreateFieldwork.class.getProtectionDomain().getCodeSource().getLocation().toString());
    }

    private static void play(PonderScene scene, String id) {
        var circuit = IsolatorSceneChecks.forScene(id);
        scene.begin();
        for (int tick = 0; tick <= scene.getTotalTime() + 5; tick++) {
            scene.tick();
            if (circuit != null) circuit.tick(scene);
        }
        require(scene.getCurrentTime() >= scene.getTotalTime(), "Incomplete playback " + id);
        if (circuit != null) circuit.complete();
    }

    private static JsonObject json(String path) throws Exception {
        try (var input = CreateFieldwork.class.getResourceAsStream("/assets/create_fieldwork/" + path)) {
            return JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static List<net.createmod.ponder.api.registration.StoryBoardEntry> boards() {
        var result = new ArrayList<net.createmod.ponder.api.registration.StoryBoardEntry>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (var entry : PonderIndex.getSceneAccess().getRegisteredEntries()) {
            var board = entry.getValue();
            if (board.getNamespace().equals(CreateFieldwork.MOD_ID) && seen.add(board.getSchematicLocation())) result.add(board);
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void finish() {
        finished = true;
        try {
            Path report = Path.of(System.getProperty("fieldwork.ponder.report"));
            Files.createDirectories(report.getParent());
            Files.writeString(report, RESULT.toString());
            System.out.println("FIELDWORK_PONDER_RESULT " + RESULT);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        } finally {
            Minecraft.getInstance().stop();
        }
    }
}
