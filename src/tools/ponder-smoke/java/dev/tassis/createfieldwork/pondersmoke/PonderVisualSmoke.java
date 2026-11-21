package dev.tassis.createfieldwork.pondersmoke;

import com.google.gson.JsonObject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Disposable integrated world and real Ponder screens, isolated from ordinary client runs. */
final class PonderVisualSmoke {
    private static boolean started;
    private static int warmup;
    private static int boardIndex;
    private static int stage;
    private static int frame;
    private static int screenshots;

    private PonderVisualSmoke() {}

    static boolean worldReady() {
        var mc = Minecraft.getInstance();
        if (!started) {
            started = true;
            var settings = new LevelSettings("Fieldwork Ponder verification", GameType.CREATIVE, false,
                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ponder-" + System.currentTimeMillis(), settings,
                new WorldOptions(0, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return false;
        }
        return mc.level != null && mc.player != null && warmup++ > 30;
    }

    static boolean capture(JsonObject result) throws Exception {
        var mc = Minecraft.getInstance();
        var boards = PonderSmoke.boards();
        if (boardIndex == boards.size()) {
            if (!ScribingScreenChecks.capture()) return false;
            result.addProperty("tablePrintOnly", true);
            result.addProperty("screenshots", screenshots + 1);
            return true;
        }
        var board = boards.get(boardIndex);
        boolean entering = stage == 0;
        if (frame == 0) {
            var scene = PonderIndex.getSceneAccess().compile(List.of(board)).getFirst();
            mc.setScreen(new SingleScene(scene));
            int[] times = captureTimes(board.getSchematicLocation().getPath(), scene.getTotalTime());
            if (!entering) ((PonderUI) mc.screen).seekToTime(times[stage]);
        }
        boolean snapshot = entering ? frame == 5 || frame == 15 || frame == 26 || frame == 45 : frame == 15;
        if (snapshot) {
            Path folder = ScribingScreenChecks.folder();
            Files.createDirectories(folder);
            String suffix = entering ? "entrance-" + frame : Integer.toString(stage - 1);
            Screenshot.grab(folder.toFile(), board.getSchematicLocation().getPath() + "-" + suffix + ".png",
                mc.getMainRenderTarget(), message -> {});
            screenshots++;
        }
        if (++frame > (entering ? 50 : 22)) {
            frame = 0;
            int count = captureTimes(board.getSchematicLocation().getPath(), 0).length;
            if (++stage == count) {
                stage = 0;
                boardIndex++;
            }
        }
        return false;
    }

    private static int[] captureTimes(String id, int total) {
        return switch (id) {
            case "isolator_basics" -> new int[] {-1, 80, 205, 330, 465};
            case "isolator_lectern" -> new int[] {-1, 80};
            case "smart_controller_binding" -> new int[] {-1, 80, 205, 340};
            default -> new int[] {-1, 80, total / 2, total - 90};
        };
    }

    private static final class SingleScene extends PonderUI {
        SingleScene(PonderScene scene) {
            super(List.of(scene));
        }
    }
}
