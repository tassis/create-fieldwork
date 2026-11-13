package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import dev.tassis.createfieldwork.mixin.schematic.StructureTemplateAccessor;
import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;
import dev.tassis.createfieldwork.schematic.input.BlueprintLocalDeployment;
import dev.tassis.createfieldwork.schematic.comparison.BuildStatePolicy;
import dev.tassis.createfieldwork.schematic.comparison.NativeSchematicTarget;
import dev.tassis.createfieldwork.schematic.BlueprintSyncEchoes;
import dev.tassis.createfieldwork.schematic.comparison.WorldTargetSupport;
import dev.tassis.createfieldwork.schematic.mask.GhostMask;
import dev.tassis.createfieldwork.schematic.mask.MaskedEntityBits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.UUID;
import java.util.function.BiConsumer;

/** Common native target/sync/policy evidence. Does not instantiate or validate a GUI. */
final class SchematicAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    SchematicAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    void run() {
        policy(); selectionAndSync(); localDeployment(); transforms(); worldOnly();
    }
    private void policy() {
        var stone = Blocks.STONE.defaultBlockState();
        check.accept("wrong loaded type is red", BuildStatePolicy.compare(stone, Blocks.DIRT.defaultBlockState()) == BuildStatePolicy.Result.WRONG_BLOCK);
        check.accept("actual air remains native waiting ghost", BuildStatePolicy.compare(stone, Blocks.AIR.defaultBlockState()) == BuildStatePolicy.Result.MISSING);
        check.accept("expected air does not flag extra blocks", BuildStatePolicy.compare(Blocks.AIR.defaultBlockState(), stone) == BuildStatePolicy.Result.UNVERIFIED);
        check.accept("same plain construction block matches", BuildStatePolicy.compare(stone, stone) == BuildStatePolicy.Result.MATCH);
        var observer = Blocks.OBSERVER.defaultBlockState();
        check.accept("same block wrong facing is orange", BuildStatePolicy.compare(observer.setValue(BlockStateProperties.FACING, Direction.NORTH), observer.setValue(BlockStateProperties.FACING, Direction.SOUTH)) == BuildStatePolicy.Result.WRONG_STATE);
        var log = Blocks.OAK_LOG.defaultBlockState();
        check.accept("same block wrong axis is orange", BuildStatePolicy.compare(log.setValue(BlockStateProperties.AXIS, Direction.Axis.X), log.setValue(BlockStateProperties.AXIS, Direction.Axis.Y)) == BuildStatePolicy.Result.WRONG_STATE);
        var slab = Blocks.STONE_SLAB.defaultBlockState();
        check.accept("same block wrong slab half is orange", BuildStatePolicy.compare(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)) == BuildStatePolicy.Result.WRONG_STATE);
        var stairs = Blocks.OAK_STAIRS.defaultBlockState();
        check.accept("same block wrong stair half is orange", BuildStatePolicy.compare(stairs.setValue(BlockStateProperties.HALF, Half.TOP), stairs.setValue(BlockStateProperties.HALF, Half.BOTTOM)) == BuildStatePolicy.Result.WRONG_STATE);
        check.accept("waterlogging is not a construction mismatch", BuildStatePolicy.compare(slab.setValue(BlockStateProperties.WATERLOGGED, true), slab.setValue(BlockStateProperties.WATERLOGGED, false)) == BuildStatePolicy.Result.MATCH);
        var lamp = Blocks.REDSTONE_LAMP.defaultBlockState();
        check.accept("running light state is ignored", BuildStatePolicy.compare(lamp.setValue(BlockStateProperties.LIT, true), lamp.setValue(BlockStateProperties.LIT, false)) == BuildStatePolicy.Result.MATCH);
        var note = Blocks.NOTE_BLOCK.defaultBlockState();
        check.accept("unknown properties are neutral not full-state orange", BuildStatePolicy.compare(note.setValue(BlockStateProperties.NOTE, 1), note.setValue(BlockStateProperties.NOTE, 2)) == BuildStatePolicy.Result.UNVERIFIED);
        var a = new BlockPos(1, 2, 3); var b = a.east(); var c = b.east();
        java.util.List<net.minecraft.world.level.block.entity.BlockEntity> entities = java.util.List.of(
            new net.minecraft.world.level.block.entity.ChestBlockEntity(a, Blocks.CHEST.defaultBlockState()),
            new net.minecraft.world.level.block.entity.ChestBlockEntity(b, Blocks.CHEST.defaultBlockState()),
            new net.minecraft.world.level.block.entity.ChestBlockEntity(c, Blocks.CHEST.defaultBlockState()));
        var enabled = new java.util.BitSet(); enabled.set(0, 3);
        var visible = new MaskedEntityBits(enabled, entities, pos -> pos.equals(a) || pos.equals(c));
        check.accept("correct entity ghosts are skipped without shifting native indices", visible.nextSetBit(0) == 1 && visible.nextSetBit(2) == -1);
        check.accept("visibility view never clears native entity/error enablement", enabled.cardinality() == 3);
        check.accept("restoring unknown entity visibility keeps native ordinal", new MaskedEntityBits(enabled, entities, pos -> false).nextSetBit(0) == 0);
    }
    private void selectionAndSync() {
        check.accept("offhand schematic yields native controller activation and air use", dev.tassis.createfieldwork.schematic.input.ControllerUsePriority.nativeUse(AllItems.LINKED_CONTROLLER.asStack(), null));
        check.accept("offhand schematic yields physical lectern use even with empty main hand", dev.tassis.createfieldwork.schematic.input.ControllerUsePriority.nativeUse(ItemStack.EMPTY, com.simibubi.create.AllBlocks.LECTERN_CONTROLLER.getDefaultState()));
        check.accept("non-controller non-lectern clicks retain schematic tool handling", !dev.tassis.createfieldwork.schematic.input.ControllerUsePriority.nativeUse(new ItemStack(Items.STICK), Blocks.STONE.defaultBlockState()));
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "SchematicPolicy"), ClientInformation.createDefault());
        var main = blueprint(new BlockPos(1, 120, 1)); var off = blueprint(new BlockPos(9, 120, 9));
        actor.setItemInHand(InteractionHand.MAIN_HAND, main); actor.setItemInHand(InteractionHand.OFF_HAND, off);
        var selected = BlueprintSelection.select(actor);
        check.accept("valid main schematic wins over same-name different-state offhand", selected.stack() == main && selected.slot() == actor.getInventory().selected);
        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        selected = BlueprintSelection.select(actor);
        check.accept("valid offhand selection uses exact stack and slot40", selected.stack() == off && selected.slot() == 40);
        actor.setItemInHand(InteractionHand.MAIN_HAND, main);
        var destination = new BlockPos(20, 122, 24);
        var echoes = new BlueprintSyncEchoes();
        var expectedSettings = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setMirror(Mirror.FRONT_BACK);
        echoes.expect(40, off, destination, expectedSettings, true);
        new SchematicSyncPacket(40, true, destination, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK).handle(actor);
        check.accept("real native slot40 sync mutates offhand anchor", destination.equals(off.get(AllDataComponents.SCHEMATIC_ANCHOR)));
        check.accept("native slot40 sync preserves distinct main deployment", new BlockPos(1, 120, 1).equals(main.get(AllDataComponents.SCHEMATIC_ANCHOR)));
        check.accept("native slot40 sync carries rotation and mirror", off.get(AllDataComponents.SCHEMATIC_ROTATION) == Rotation.CLOCKWISE_90 && off.get(AllDataComponents.SCHEMATIC_MIRROR) == Mirror.FRONT_BACK);
        check.accept("native placement echo is recognized without resetting local preview", echoes.consume(40, off));
        check.accept("sync echo is consumed exactly once", !echoes.consume(40, off));
        var rightToLeft = blueprint(new BlockPos(30, 122, 30));
        var preserved = new BlockPos(36, 124, 38);
        actor.setItemInHand(InteractionHand.MAIN_HAND, rightToLeft);
        actor.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        new SchematicSyncPacket(actor.getInventory().selected, true, preserved, Rotation.COUNTERCLOCKWISE_90, Mirror.LEFT_RIGHT).handle(actor);
        actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        actor.setItemInHand(InteractionHand.OFF_HAND, rightToLeft);
        check.accept("native pre-swap sync preserves right-hand transform after moving the same schematic to offhand",
            BlueprintSelection.select(actor).stack() == rightToLeft
                && preserved.equals(rightToLeft.get(AllDataComponents.SCHEMATIC_ANCHOR))
                && rightToLeft.get(AllDataComponents.SCHEMATIC_ROTATION) == Rotation.COUNTERCLOCKWISE_90
                && rightToLeft.get(AllDataComponents.SCHEMATIC_MIRROR) == Mirror.LEFT_RIGHT);
        actor.setItemInHand(InteractionHand.MAIN_HAND, main);
        actor.setItemInHand(InteractionHand.OFF_HAND, off);
        echoes.expect(0, off, destination, expectedSettings, true);
        check.accept("identical other-slot item is not a sync acknowledgement", !echoes.consume(40, off));
        for (int i = 0; i < 100; i++) echoes.expect(40, off, destination.east(i), expectedSettings, true);
        check.accept("pending sync echo history is bounded", echoes.pending() == 32);
        actor.setItemInHand(InteractionHand.MAIN_HAND, off); actor.setItemInHand(InteractionHand.OFF_HAND, main);
        check.accept("same-filename hand swap selects distinct main deployment stack", BlueprintSelection.select(actor).stack() == off &&
            destination.equals(BlueprintSelection.select(actor).stack().get(AllDataComponents.SCHEMATIC_ANCHOR)));
        actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); actor.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        check.accept("empty hands clear schematic selection", BlueprintSelection.select(actor) == null);
    }
    private ItemStack blueprint(BlockPos anchor) {
        var item = AllItems.SCHEMATIC.asStack();
        item.set(AllDataComponents.SCHEMATIC_FILE, "fieldwork-headless-policy.nbt");
        item.set(AllDataComponents.SCHEMATIC_BOUNDS, new Vec3i(3, 2, 4));
        item.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor); item.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        return item;
    }
    private void localDeployment() {
        var stack = blueprint(new BlockPos(11, 120, 13));
        stack.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        stack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_90);
        stack.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.FRONT_BACK);
        var previous = BlueprintSyncEchoes.normalized(stack);
        var selected = deploymentSelection(40, stack, previous);
        var local = BlueprintLocalDeployment.begin(selected);
        check.accept("accepted unchanged schematic can begin a local Position action", local != null);
        var destination = new BlockPos(-27, 124, 35);
        stack.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, destination);
        check.accept("native Position writes differ from the old full-stack selection snapshot",
            !ItemStack.matches(previous, BlueprintSyncEchoes.normalized(stack)));
        var acknowledged = local.acknowledge(selected, destination, true);
        check.accept("native deployed and anchor writes produce the next accepted selection snapshot",
            acknowledged != null && ItemStack.matches(acknowledged, BlueprintSyncEchoes.normalized(stack)));
        check.accept("local acknowledgement leaves the old snapshot and current item settings untouched",
            new BlockPos(11, 120, 13).equals(previous.get(AllDataComponents.SCHEMATIC_ANCHOR))
                && !previous.get(AllDataComponents.SCHEMATIC_DEPLOYED)
                && stack.get(AllDataComponents.SCHEMATIC_ROTATION) == Rotation.CLOCKWISE_90
                && stack.get(AllDataComponents.SCHEMATIC_MIRROR) == Mirror.FRONT_BACK);
        check.accept("pre-action stale state is not acknowledged as a local action",
            BlueprintLocalDeployment.begin(selected) == null);
        check.accept("identical replacement ItemStack identity is not a local mutation",
            local.acknowledge(deploymentSelection(40, stack.copy(), previous), destination, true) == null);
        check.accept("same-stack no-op Position action can retain its accepted snapshot",
            BlueprintLocalDeployment.begin(deploymentSelection(40, stack, acknowledged))
                .acknowledge(deploymentSelection(40, stack, acknowledged), destination, true) != null);
        check.accept("local acknowledgement rejects a different world",
            local.acknowledge(new BlueprintLocalDeployment.Selection(new Object(), this, 40, stack, previous), destination, true) == null);
        check.accept("local acknowledgement rejects a different player",
            local.acknowledge(new BlueprintLocalDeployment.Selection(level, new Object(), 40, stack, previous), destination, true) == null);
        check.accept("local acknowledgement rejects a hand or selected-slot change",
            local.acknowledge(deploymentSelection(0, stack, previous), destination, true) == null);
        check.accept("local acknowledgement rejects reentrant selection-snapshot replacement",
            local.acknowledge(deploymentSelection(40, stack, previous.copy()), destination, true) == null);
        check.accept("local acknowledgement rejects lost eligible selection", local.acknowledge(null, destination, true) == null);
        check.accept("local acknowledgement rejects an item anchor different from the native raw target",
            local.acknowledge(selected, destination.east(), true) == null);
        check.accept("local acknowledgement requires the native handler to be deployed",
            local.acknowledge(selected, destination, false) == null);
        check.accept("missing eligible selection cannot begin local deployment", BlueprintLocalDeployment.begin(null) == null);
        var main = deploymentSelection(0, stack, acknowledged);
        check.accept("same native action policy preserves main-hand tool semantics",
            BlueprintLocalDeployment.begin(main).acknowledge(main, destination, true) != null);

        deploymentRejected("file replacement", item -> item.set(AllDataComponents.SCHEMATIC_FILE, "other.nbt"));
        deploymentRejected("bounds replacement", item -> item.set(AllDataComponents.SCHEMATIC_BOUNDS, new Vec3i(5, 2, 4)));
        deploymentRejected("rotation replacement", item -> item.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.CLOCKWISE_180));
        deploymentRejected("mirror replacement", item -> item.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.LEFT_RIGHT));
        deploymentRejected("count change", item -> item.setCount(2));
        deploymentRejected("unrelated custom name change", item -> item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal("replacement")));
        deploymentRejected("missing anchor", item -> item.remove(AllDataComponents.SCHEMATIC_ANCHOR));
        deploymentRejected("undeployed state", item -> item.set(AllDataComponents.SCHEMATIC_DEPLOYED, false));
        deploymentRejected("missing schematic identity", item -> item.remove(AllDataComponents.SCHEMATIC_FILE));
        check.accept("empty schematic cannot begin local deployment",
            BlueprintLocalDeployment.begin(deploymentSelection(40, ItemStack.EMPTY, ItemStack.EMPTY)) == null);

        // The returned snapshot is a copy, not an alias that hides later external edits.
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, destination.east());
        check.accept("subsequent external mutation still differs from acknowledged local snapshot",
            !ItemStack.matches(acknowledged, BlueprintSyncEchoes.normalized(stack)));
        var echoes = new BlueprintSyncEchoes();
        echoes.expect(40, stack, destination, new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setMirror(Mirror.FRONT_BACK), true);
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, destination);
        check.accept("native sync acknowledgement remains independent of local deployment recognition", echoes.consume(40, stack));
    }

    private void deploymentRejected(String change, java.util.function.Consumer<ItemStack> mutate) {
        var stack = blueprint(BlockPos.ZERO);
        stack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
        stack.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE);
        var selected = deploymentSelection(40, stack, BlueprintSyncEchoes.normalized(stack));
        var local = BlueprintLocalDeployment.begin(selected);
        var destination = new BlockPos(20, 124, 24);
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, destination);
        mutate.accept(stack);
        check.accept("local Position acknowledgement rejects " + change, local.acknowledge(selected, destination, true) == null);
    }

    private BlueprintLocalDeployment.Selection deploymentSelection(int slot, ItemStack stack, ItemStack previous) {
        return new BlueprintLocalDeployment.Selection(level, this, slot, stack, previous);
    }
    private void transforms() {
        var source = new BlockPos(768, 120, 0);
        var stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).setValue(BlockStateProperties.HALF, Half.TOP);
        level.setBlock(source, stairs, 2);
        level.setBlock(source.offset(2, 0, 1), Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), 2);
        level.setBlock(source.offset(0, 1, 3), Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH), 2);
        level.setBlock(source.offset(1, 0, 2), Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), 2);
        var template = new StructureTemplate();
        template.fillFromWorld(level, source, new Vec3i(3, 2, 4), false, Blocks.STRUCTURE_VOID);
        var target = NativeSchematicTarget.inspect(template);
        check.accept("native asymmetric captured template has supported single palette", target != null);
        for (var variant : Mirror.values()) {
            var virtualSettings = new StructurePlaceSettings().setMirror(variant);
            var offset = variant == Mirror.FRONT_BACK ? new BlockPos(2, 0, 0) : variant == Mirror.LEFT_RIGHT ? new BlockPos(0, 0, 3) : BlockPos.ZERO;
            for (var info : target.blocks()) if (!info.state().isAir()) {
                var virtual = StructureTemplate.calculateRelativePosition(virtualSettings, info.pos()).offset(offset);
                check.accept("ghost mask resolves native mirrored virtual position to original source " + variant + "/" + info.pos(),
                    GhostMask.sourcePosition(virtual, variant, target.size()) == info.pos().asLong());
            }
        }
        int scenario = 0;
        for (var mirror : Mirror.values()) for (var rotation : Rotation.values()) for (int translated = 0; translated < 2; translated++) {
            var anchor = new BlockPos(800 + scenario++ * 12, 120 + translated * 3, 64 + translated * 16);
            var settings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation).setIgnoreEntities(true);
            if (translated == 1) settings.setRotationPivot(new BlockPos(1, 0, 2));
            check.accept("native placement succeeds " + mirror + "/" + rotation + "/" + translated,
                template.placeInWorld(level, anchor, anchor, settings, RandomSource.create(0), 2));
            int nonAir = 0;
            for (int i = 0; i < target.blocks().size(); i++) {
                var expected = NativeSchematicTarget.sample(target, i, anchor, settings);
                if (expected == null || expected.state().isAir()) continue;
                nonAir++;
                var actual = level.getBlockState(expected.position());
                check.accept("native world-coordinate/type equivalence " + mirror + "/" + rotation + "/" + translated + " " + expected.position(), actual.is(expected.state().getBlock()));
                check.accept("native once-only orientation equivalence " + mirror + "/" + rotation + "/" + translated + " " + expected.position(),
                    !BuildStatePolicy.error(BuildStatePolicy.compare(expected.state(), actual)));
            }
            check.accept("all asymmetric non-air members compared " + mirror + "/" + rotation + "/" + translated, nonAir == 4);
        }
        var palettes = ((StructureTemplateAccessor) template).fieldwork$palettes(); palettes.add(palettes.getFirst());
        check.accept("ambiguous multi-palette template is unsupported rather than guessed", NativeSchematicTarget.inspect(template) == null);
    }
    private void worldOnly() {
        var ordinary = new BlockPos(1600, 120, 96);
        level.setBlock(ordinary, Blocks.STONE.defaultBlockState(), 3);
        check.accept("loaded ordinary world is eligible for comparison", WorldTargetSupport.comparable(level, ordinary));
        check.accept("unloaded world remains neutral without force load", !WorldTargetSupport.comparable(level, new BlockPos(10_000_000, 120, -10_000_000)));
        var floor = ordinary.below(); level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        var positions = java.util.List.of(ordinary, floor);
        var craft = dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level, ordinary, positions, dev.ryanhcode.sable.companion.math.BoundingBox3i.from(positions));
        check.accept("reserved plot coordinates are never compared as ordinary world", !WorldTargetSupport.comparable(level, craft.getPlot().getCenterBlock()));
        var projected = craft.logicalPose().transformPosition(new org.joml.Vector3d(craft.getPlot().getCenterBlock().getX() + .5,
            craft.getPlot().getCenterBlock().getY() + .5, craft.getPlot().getCenterBlock().getZ() + .5));
        check.accept("projected craft occupancy is neutral outside reserved plot arena", !WorldTargetSupport.comparable(level, BlockPos.containing(projected.x, projected.y, projected.z)));
        var original = new dev.ryanhcode.sable.companion.math.Pose3d(craft.logicalPose());
        try {
            craft.logicalPose().set(craft.lastPose());
            var stationary = WorldTargetSupport.capture(level);
            check.accept("stationary metadata exclusion is ready without cached global bounds", stationary.ready());
            craft.logicalPose().position().add(16, 4, -8);
            craft.logicalPose().orientation().rotateY(Math.PI / 2);
            craft.logicalPose().scale().set(1.5, .75, 2);
            var moving = WorldTargetSupport.capture(level);
            check.accept("moving non-unit scaled craft snapshot is ready", moving.ready());
            var bounds = craft.getPlot().getBoundingBox();
            check.accept("fixture uses nonzero plot rotation point", craft.logicalPose().rotationPoint().lengthSquared() > 0);
            for (int step = 0; step <= 4; step++) {
                double t = step / 4d;
                var pose = new dev.ryanhcode.sable.companion.math.Pose3d(craft.lastPose());
                pose.position().lerp(craft.logicalPose().position(), t);
                pose.orientation().slerp(craft.logicalPose().orientation(), t);
                pose.scale().lerp(craft.logicalPose().scale(), t);
                pose.rotationPoint().lerp(craft.logicalPose().rotationPoint(), t);
                for (int corner = 0; corner < 8; corner++) {
                    var point = pose.transformPosition(new org.joml.Vector3d(
                        (corner & 1) == 0 ? bounds.minX() + .01 : bounds.maxX() + .99,
                        (corner & 2) == 0 ? bounds.minY() + .01 : bounds.maxY() + .99,
                        (corner & 4) == 0 ? bounds.minZ() + .01 : bounds.maxZ() + .99));
                    var pos = BlockPos.containing(point.x, point.y, point.z);
                    level.getChunkAt(pos); // Deliberate fixture loading, never production comparison.
                    check.accept("loaded swept rotated/scaled corner remains neutral " + step + "/" + corner,
                        level.hasChunkAt(pos) && !moving.comparable(pos));
                    if (step == 0) check.accept("stationary native-projected corner remains neutral " + corner, !stationary.comparable(pos));
                }
            }
            var nearbyWorld = ordinary.offset(128, 0, 128); level.getChunkAt(nearbyWorld);
            check.accept("bounded ship exclusion does not neutralize separate ordinary world", moving.comparable(nearbyWorld));
            craft.logicalPose().position().set(Double.NaN, 0, 0);
            check.accept("unavailable pose metadata neutralizes comparison instead of assuming WORLD", !WorldTargetSupport.capture(level).ready());
        } finally { craft.logicalPose().set(original); }
    }
}
