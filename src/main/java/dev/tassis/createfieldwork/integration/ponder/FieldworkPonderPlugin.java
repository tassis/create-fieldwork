package dev.tassis.createfieldwork.integration.ponder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.tassis.createfieldwork.platform.AeronauticsIntegration;
import dev.tassis.createfieldwork.platform.CreateFieldwork;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Client presentation only. Scenes never resolve or mutate live craft authority. */
public final class FieldworkPonderPlugin implements PonderPlugin {
    private static final String ROOT = "/assets/create_fieldwork/";
    private static final Map<String, String> ENGLISH = read("lang/en_us.json", new TypeToken<>() {});
    private final List<Tutorial> tutorials = read("ponder/tutorials.json", new TypeToken<>() {});

    @Override
    public String getModId() {
        return CreateFieldwork.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (var tutorial : tutorials) {
            if (tutorial.integrated && !AeronauticsIntegration.active()) {
                continue;
            }
            var registration = helper.forComponents(tutorial.items.stream().map(ResourceLocation::parse).toList());
            switch (tutorial.id) {
                case "isolator_basics" -> registration.addStoryBoard(tutorial.id, IsolatorScenes::basics);
                case "isolator_lectern" -> registration.addStoryBoard(tutorial.id, IsolatorScenes::lectern,
                    entry -> entry.orderAfter("isolator_basics"));
                case "smart_controller_binding" -> registration.addStoryBoard(tutorial.id, IsolatorScenes::binding,
                    entry -> entry.orderAfter(entry.getComponent().getPath().equals("redstone_link_isolator")
                        ? "isolator_lectern" : "isolator_basics"));
                default -> throw new IllegalStateException("Unknown Fieldwork tutorial: " + tutorial.id);
            }
        }
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        tutorials.stream().flatMap(tutorial -> tutorial.captions.stream()).distinct()
            .forEach(key -> helper.registerSharedText(key, english("shared." + key)));
    }

    static void begin(SceneBuilder scene, String id, int width) {
        scene.title(id, english(id + ".header"));
        scene.configureBasePlate(0, 0, width);
        scene.scaleSceneView(width > 9 ? 0.65f : 0.8f);
        scene.showBasePlate();
        scene.idle(10);
    }

    static void caption(SceneBuilder scene, String key, BlockPos target, PonderPalette color) {
        scene.overlay().showText(110).sharedText(key).colored(color)
            .pointAt(Vec3.atCenterOf(target)).placeNearTarget().attachKeyFrame();
        scene.idle(125);
    }

    private static String english(String suffix) {
        String key = "create_fieldwork.ponder." + suffix;
        String text = ENGLISH.get(key);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Missing Ponder text: " + key);
        }
        return text;
    }

    private static <T> T read(String path, TypeToken<T> type) {
        try (var input = FieldworkPonderPlugin.class.getResourceAsStream(ROOT + path)) {
            if (input == null) {
                throw new IllegalStateException("Missing Ponder resource: " + path);
            }
            return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Ponder resource: " + path, error);
        }
    }

    private record Tutorial(String id, List<String> items, boolean integrated, List<String> captions) {}
}
