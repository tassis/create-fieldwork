package dev.tassis.createfieldwork.integration.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

/** Two already-assembled craft illustrations; no live physics or radio authority. */
final class IsolatorScenes {
    private static final BlockPos ISOLATOR = new BlockPos(2, 3, 5);
    private static final BlockPos A_LAMP = new BlockPos(3, 3, 7);
    private static final BlockPos A_LINK = new BlockPos(2, 3, 7);
    private static final BlockPos B_LAMP = new BlockPos(11, 3, 7);
    private static final BlockPos B_LINK = new BlockPos(10, 3, 7);
    private static final BlockPos LEVER = new BlockPos(2, 4, 6);
    private static final BlockPos TRANSMITTER = new BlockPos(1, 3, 6);
    private static final BlockPos LECTERN = new BlockPos(1, 3, 5);
    private static final Vec3 LIFT = new Vec3(0, 0.5, 0);
    private record Ships(ElementLink<WorldSectionElement> a,
                         ElementLink<WorldSectionElement> rotorA,
                         ElementLink<WorldSectionElement> rotorB) {}

    private IsolatorScenes() {}

    public static void basics(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        var ships = introduction(scene, util);
        handheldDenied(scene, util, ships);
        scene.markAsFinished();
    }

    public static void lectern(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        var ships = setup(scene, util, "isolator_lectern", true, false);
        scene.world().showSectionAndMerge(util.select().position(LECTERN), Direction.DOWN, ships.a);
        Vec3 controls = aboard(LECTERN).add(-4, 1, 0);
        scene.overlay().showControls(controls, Pointing.DOWN, 70)
            .withItem(new ItemStack(AllItems.LINKED_CONTROLLER.get())).rightClick();
        scene.overlay().showLine(PonderPalette.WHITE, controls, aboard(LECTERN), 70);
        drive(scene, util, ships, true, false);
        signal(scene, aboard(LECTERN), aboard(A_LINK), PonderPalette.GREEN);
        text(scene, "lectern_works", PonderPalette.GREEN);
        scene.markAsFinished();
    }

    public static void binding(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        var ships = setup(scene, util, "smart_controller_binding", true, true);
        controller(scene, aboard(ISOLATOR), true);
        text(scene, "bind_controller", PonderPalette.BLUE);
        Vec3 insideHand = aboard(new BlockPos(4, 4, 6));
        controller(scene, insideHand, false);
        drive(scene, util, ships, true, false);
        signal(scene, insideHand, aboard(A_LINK), PonderPalette.GREEN);
        text(scene, "bound_inside", PonderPalette.GREEN);
        drive(scene, util, ships, false, false);
        scene.idle(10);
        Vec3 outsideHand = new Vec3(7.5, 3, 11.5);
        controller(scene, outsideHand, false);
        drive(scene, util, ships, true, false);
        signal(scene, outsideHand, aboard(A_LINK), PonderPalette.GREEN);
        text(scene, "bound_outside", PonderPalette.GREEN);
        scene.markAsFinished();
    }

    private static Ships setup(CreateSceneBuilder scene, SceneBuildingUtil util, String id, boolean isolated, boolean smart) {
        FieldworkPonderPlugin.begin(scene, id, 15);
        scene.scaleSceneView(0.53f);
        scene.setSceneOffsetY(-1.5f);
        var a = scene.world().showIndependentSection(util.select().fromTo(1, 1, 2, 5, 7, 9)
            .substract(util.select().position(LECTERN)), Direction.DOWN);
        var rotorA = scene.world().showIndependentSection(util.select().fromTo(3, 2, 1, 5, 4, 1), Direction.DOWN);
        scene.world().moveSection(a, LIFT, 0);
        scene.world().configureCenterOfRotation(rotorA, new Vec3(4.5, 3.5, 1.5));
        scene.world().moveSection(rotorA, LIFT, 0);
        scene.idle(8);
        var b = scene.world().showIndependentSection(util.select().fromTo(9, 1, 2, 13, 7, 9), Direction.DOWN);
        var rotorB = scene.world().showIndependentSection(util.select().fromTo(11, 2, 1, 13, 4, 1), Direction.DOWN);
        scene.world().moveSection(b, LIFT, 0);
        scene.world().configureCenterOfRotation(rotorB, new Vec3(12.5, 3.5, 1.5));
        scene.world().moveSection(rotorB, LIFT, 0);
        var ships = new Ships(a, rotorA, rotorB);
        if (isolated) placeIsolator(scene, util, ships, smart);
        drive(scene, util, ships, false, false);
        scene.idle(32);
        return ships;
    }

    private static Ships introduction(CreateSceneBuilder scene, SceneBuildingUtil util) {
        var ships = setup(scene, util, "isolator_basics", false, false);
        text(scene, "two_airships", PonderPalette.WHITE);
        scene.overlay().showControls(aboard(LEVER), Pointing.DOWN, 100).rightClick();
        lever(scene, true);
        drive(scene, util, ships, true, true);
        scene.overlay().showOutline(PonderPalette.BLUE, "matching-links",
            util.select().position(A_LINK).add(util.select().position(B_LINK)), 110);
        signal(scene, aboard(TRANSMITTER), aboard(B_LINK), PonderPalette.GREEN);
        text(scene, "matching_airships", PonderPalette.BLUE);

        placeIsolator(scene, util, ships, false);
        drive(scene, util, ships, true, false);
        text(scene, "isolate_airship", PonderPalette.RED);
        lever(scene, false);
        drive(scene, util, ships, false, false);
        scene.idle(10);
        return ships;
    }

    private static void placeIsolator(CreateSceneBuilder scene, SceneBuildingUtil util, Ships ships, boolean smart) {
        var marker = smart ? FieldworkContent.BRASS_ISOLATOR.get() : FieldworkContent.ISOLATOR.get();
        scene.world().setBlock(ISOLATOR, marker.defaultBlockState().setValue(IsolatorBlock.ACTIVE, true), false);
        scene.world().showSectionAndMerge(util.select().position(ISOLATOR), Direction.DOWN, ships.a);
    }

    private static void handheldDenied(CreateSceneBuilder scene, SceneBuildingUtil util, Ships ships) {
        Vec3 hand = aboard(new BlockPos(4, 4, 6));
        controller(scene, hand, false);
        drive(scene, util, ships, false, true);
        signal(scene, hand, aboard(A_LINK), PonderPalette.RED);
        signal(scene, hand, aboard(B_LINK), PonderPalette.GREEN);
        text(scene, "handheld_public_airship", PonderPalette.RED);
    }

    private static void controller(SceneBuilder scene, Vec3 pos, boolean sneak) {
        var hint = scene.overlay().showControls(pos, Pointing.DOWN, 100)
            .withItem(new ItemStack(AllItems.LINKED_CONTROLLER.get())).rightClick();
        if (sneak) hint.whileSneaking();
    }

    private static Vec3 aboard(BlockPos pos) { return Vec3.atCenterOf(pos).add(LIFT); }

    private static void text(SceneBuilder scene, String key, PonderPalette color) {
        scene.overlay().showText(110).sharedText(key).colored(color).independent(10).attachKeyFrame();
        scene.idle(125);
    }

    private static void signal(SceneBuilder scene, Vec3 source, Vec3 target, PonderPalette color) {
        scene.overlay().showLine(color, source, target, 110);
    }

    private static void lever(SceneBuilder scene, boolean powered) {
        scene.world().modifyBlock(LEVER, state -> state.setValue(LeverBlock.POWERED, powered), false);
        scene.world().modifyBlock(TRANSMITTER, state -> state.setValue(RedstoneLinkBlock.POWERED, powered), false);
    }

    private static void drive(CreateSceneBuilder scene, SceneBuildingUtil util, Ships ships, boolean a, boolean b) {
        boolean[] powered = {a, b};
        BlockPos[] lamps = {A_LAMP, B_LAMP};
        BlockPos[] links = {A_LINK, B_LINK};
        for (int index = 0; index < 2; index++) {
            boolean on = powered[index];
            int x = index == 0 ? 4 : 12;
            scene.world().setBlock(lamps[index], Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, on), false);
            scene.world().modifyBlock(links[index], state -> state.setValue(RedstoneLinkBlock.POWERED, on), false);
            scene.world().setKineticSpeed(util.select().fromTo(x, 3, 2, x, 3, 3), on ? 64 : 0);
            if (on) {
                scene.world().rotateSection(index == 0 ? ships.rotorA : ships.rotorB, 0, 0, 360, 125);
            }
        }
    }
}
