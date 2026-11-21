package dev.tassis.createfieldwork.pondersmoke;

import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Independent authored-circuit expectations: support, visible channels and operation order. */
final class IsolatorSceneChecks {
    private record Stage(int time, boolean isolated, boolean lever, boolean inside, boolean outside) {}
    private final List<Stage> stages = new ArrayList<>();
    private final boolean smart;
    private final boolean shared;
    private int checked;
    private int entrancesChecked;
    private final Map<BlockPos, Float> angles = new HashMap<>();
    private final Set<BlockPos> rotated = new HashSet<>();
    private final Map<BlockPos, Boolean> moving = new HashMap<>();

    private IsolatorSceneChecks(String id) {
        smart = id.equals("smart_controller_binding");
        shared = id.equals("isolator_basics");
        if (shared) {
            stages.add(new Stage(80, false, false, false, false));
            stages.add(new Stage(205, false, true, true, true));
            stages.add(new Stage(330, true, true, true, false));
            stages.add(new Stage(465, true, false, false, true));
        } else if (smart) {
            stages.add(new Stage(80, true, false, false, false));
            stages.add(new Stage(205, true, false, true, false));
            stages.add(new Stage(340, true, false, true, false));
        } else {
            stages.add(new Stage(80, true, false, true, false));
        }
    }

    static IsolatorSceneChecks forScene(String id) {
        return switch (id) {
            case "isolator_basics", "isolator_lectern", "smart_controller_binding" -> new IsolatorSceneChecks(id);
            default -> null;
        };
    }

    void tick(PonderScene scene) {
        int time = scene.getCurrentTime();
        boolean entranceSample = time == 15 || time == 23 || time == 45;
        Map<Integer, Float> fades = new HashMap<>();
        for (var element : scene.getElements()) {
            if (!(element instanceof WorldSectionElement section)) continue;
            try {
                var field = element.getClass().getDeclaredField("section");
                field.setAccessible(true);
                var selection = (Selection) field.get(element);
                if (selection == null) continue; // a section may not have reached its display instruction yet
                if (entranceSample) {
                    var fadeField = AnimatedSceneElementBase.class.getDeclaredField("fade");
                    fadeField.setAccessible(true);
                    float fade = ((LerpedFloat) fadeField.get(element)).getValue(1);
                    if (selection.test(new BlockPos(3, 2, 5)) && !selection.test(new BlockPos(11, 2, 5))) fades.put(0, fade);
                    if (selection.test(new BlockPos(11, 2, 5)) && !selection.test(new BlockPos(3, 2, 5))) fades.put(2, fade);
                    if (selection.test(new BlockPos(4, 3, 1)) && !selection.test(new BlockPos(4, 3, 2))) fades.put(1, fade);
                    if (selection.test(new BlockPos(12, 3, 1)) && !selection.test(new BlockPos(12, 3, 2))) fades.put(3, fade);
                }
                for (var pos : List.of(new BlockPos(4, 3, 1), new BlockPos(12, 3, 1))) {
                    if (!selection.test(pos) || selection.test(pos.south())) continue;
                    float angle = (float) section.getAnimatedRotation().z;
                    Float previous = angles.put(pos, angle);
                    boolean changed = previous != null && Math.abs(previous - angle) > 0.01f;
                    moving.put(pos, changed);
                    if (changed) rotated.add(pos);
                }
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Propeller section selection is unavailable", error);
            }
        }
        if (entranceSample) {
            if (time == 15) {
                require(midFade(fades.get(0)) && midFade(fades.get(1)), "Ship A must fade in with its propeller");
                require(fades.getOrDefault(2, 0f) == 0 && fades.getOrDefault(3, 0f) == 0, "Ship B should enter after ship A");
            } else if (time == 23) {
                require(midFade(fades.get(2)) && midFade(fades.get(3)), "Ship B must fade in with its propeller");
                require(fades.get(0) > fades.get(2) && fades.get(1) > fades.get(3), "Entrances must be staggered");
            } else {
                require(fades.size() == 4 && fades.values().stream().allMatch(fade -> fade >= 0.999f), "Both ships must finish entering before instruction");
            }
            entrancesChecked++;
        }
        if (checked == stages.size() || scene.getCurrentTime() != stages.get(checked).time) return;
        var expected = stages.get(checked++);
        var world = scene.getWorld();
        String at = scene.getId() + " at " + expected.time;
        require(Boolean.TRUE.equals(moving.get(new BlockPos(4, 3, 1))) == expected.inside, "Ship A visible rotor motion " + at);
        require(Boolean.TRUE.equals(moving.get(new BlockPos(12, 3, 1))) == expected.outside, "Ship B visible rotor motion " + at);
        var marker = world.getBlockState(new BlockPos(2, 3, 5));
        require(expected.isolated ? marker.hasProperty(IsolatorBlock.ACTIVE) && marker.getValue(IsolatorBlock.ACTIVE) : marker.isAir(), "Isolator placement/active top " + at);
        require(world.getBlockState(new BlockPos(2, 4, 6)).getValue(LeverBlock.POWERED) == expected.lever, "Lever position " + at);
        require(world.getBlockState(new BlockPos(1, 3, 6)).getValue(RedstoneLinkBlock.POWERED) == expected.lever, "Transmitter power " + at);
        require(world.getBlockState(new BlockPos(3, 3, 7)).getValue(RedstoneLampBlock.LIT) == expected.inside, "Ship A lamp " + at);
        require(world.getBlockState(new BlockPos(11, 3, 7)).getValue(RedstoneLampBlock.LIT) == expected.outside, "Ship B lamp " + at);
        require(world.getBlockState(new BlockPos(2, 3, 7)).getValue(RedstoneLinkBlock.POWERED) == expected.inside, "Ship A receiver " + at);
        require(world.getBlockState(new BlockPos(10, 3, 7)).getValue(RedstoneLinkBlock.POWERED) == expected.outside, "Ship B receiver " + at);
        for (int index = 0; index < 2; index++) {
            var propeller = world.getBlockEntity(new BlockPos(index == 0 ? 4 : 12, 3, 2));
            require(propeller instanceof KineticBlockEntity, "Missing native propeller " + at);
            float speed = ((KineticBlockEntity) propeller).getSpeed();
            require(speed == ((index == 0 ? expected.inside : expected.outside) ? 64 : 0), "Propeller speed " + index + " " + at);
        }

        for (var pos : List.of(new BlockPos(2, 3, 7), new BlockPos(10, 3, 7), new BlockPos(1, 3, 6))) {
            var state = world.getBlockState(pos);
            var support = pos.relative(state.getValue(RedstoneLinkBlock.FACING).getOpposite());
            require(!world.getBlockState(support).isAir() && state.canSurvive(world, pos), "Floating Redstone Link " + pos);
            if (state.getValue(RedstoneLinkBlock.RECEIVER)) {
                require(world.getBlockState(support).is(Blocks.REDSTONE_LAMP), "Receiver is not attached to its lamp " + pos);
            }
            require(world.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity, "Missing link BE " + pos);
            var link = (RedstoneLinkBlockEntity) world.getBlockEntity(pos);
            var key = link.getBehaviour(LinkBehaviour.TYPE).getNetworkKey();
            require(key.getFirst().getStack().is(Items.IRON_INGOT) && key.getSecond().getStack().is(Items.REDSTONE), "Mismatched/empty displayed frequencies " + pos);
        }
        for (int origin : new int[]{1, 9}) {
            for (int z : new int[]{4, 8}) {
                for (int y = 3; y <= 5; y++) {
                    require(world.getBlockState(new BlockPos(origin + 2, y, z)).is(Blocks.OAK_FENCE), "Missing centered balloon support " + at);
                    require(world.getBlockState(new BlockPos(origin + 1, y, z)).isAir(), "Old off-center support returned " + at);
                }
                require(!world.getBlockState(new BlockPos(origin + 2, 2, z)).isAir()
                    && !world.getBlockState(new BlockPos(origin + 2, 6, z)).isAir(), "Balloon support must meet deck and balloon " + at);
            }
        }
        var lectern = new BlockPos(1, 3, 5);
        require(smart == world.getBlockState(lectern).isAir(), "Wrong smart/ordinary lectern layout");
        require(world.getBlockState(new BlockPos(5, 3, 7)).isAir(), "Old obscured lectern position returned");
        require(world.getBlockState(new BlockPos(1, 3, 3)).isAir(), "Bow-corner lectern position returned");
        if (!smart) {
            require(world.getBlockState(lectern.east()).hasProperty(IsolatorBlock.ACTIVE) || !expected.isolated,
                "Lectern must sit beside the isolator on the ship side");
        }
        for (int height = 1; height <= 4; height++) {
            require(world.getBlockState(lectern.above(height)).isAir(), "Lectern is beneath an obstruction");
        }
    }

    void complete() {
        require(checked == stages.size(), "Not all circuit stages were checked: " + checked);
        require(entrancesChecked == 3, "Not all entrance fade stages were checked");
        var expectedRotors = shared ? Set.of(new BlockPos(4, 3, 1), new BlockPos(12, 3, 1)) : Set.of(new BlockPos(4, 3, 1));
        require(rotated.equals(expectedRotors), "Wrong visible propeller motion for this chapter: " + rotated);
    }

    private static boolean midFade(Float fade) {
        return fade != null && fade > 0 && fade < 1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
