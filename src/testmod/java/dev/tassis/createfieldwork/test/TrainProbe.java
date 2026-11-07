package dev.tassis.createfieldwork.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.trains.entity.*;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.tassis.createfieldwork.test.mixin.ProbeCarriageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import java.lang.ref.WeakReference;
import java.util.*;

/** Actual Create model/entry objects, not a moving station-built gameplay train. */
final class TrainProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    private final List<CarriageContraptionEntity> entities = new ArrayList<>();
    private int nodeId;
    private record TrainFixture(Train train, TrackGraph graph) {}

    TrainProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    void run() {
        ProbeTrain.enabled = true;
        try {
            var worldSource = checks.link(new BlockPos(0, 120, 0), false);
            worldSource.transmit(7);
            var a = craft(new BlockPos(32, 120, 0));
            var b = craft(new BlockPos(64, 120, 0));
            var sourceA = checks.initialized(a.getPlot().getCenterBlock());
            var sourceB = checks.initialized(b.getPlot().getCenterBlock());
            sourceA.transmit(15); sourceB.transmit(0);
            var world = train(level, List.of(new Vec3(512, 120, 0)));
            var inA = train(level, List.of(Vec3.atLowerCornerOf(a.getPlot().getCenterBlock())));
            var inB = train(level, List.of(Vec3.atLowerCornerOf(b.getPlot().getCenterBlock())));
            checks.check("actual Train owns Carriage and runtime", inA.train.runtime.train == inA.train && inA.train.carriages.getFirst().train == inA.train);
            checks.check("loaded carriage bound through real entity registry", inA.train.carriages.getFirst().anyAvailableEntity() != null);
            checks.check("carriage live proof resolves PRIVATE A", ProbeTrain.membership(level, inA.train).key().contains(a.getUniqueId().toString()));
            checks.check("world carriage proof resolves WORLD: " + ProbeTrain.membership(level, world.train), ProbeTrain.membership(level, world.train).key().equals("WORLD"));
            outcome("WORLD train sees WORLD across ordinary link range", world.train, true, false);
            outcome("PRIVATE A sees same instance source", inA.train, true, false);
            outcome("PRIVATE B cannot see WORLD or PRIVATE A", inB.train, false, true);

            worldSource.transmit(0);
            outcome("WORLD does not observe PRIVATE-only", world.train, false, true);
            checks.check("unrelated direct global query remains upstream true", Create.REDSTONE_LINK_NETWORK_HANDLER.hasAnyLoadedPower(sourceA.getBehaviour(LinkBehaviour.TYPE).getNetworkKey()));
            ProbeTrain.enabled = false;
            checks.check("negative original condition leaks PRIVATE to WORLD", condition(false).tickCompletion(level, world.train, new CompoundTag()));
            ProbeTrain.enabled = true;
            worldSource.transmit(7); sourceA.transmit(0);
            outcome("WORLD cannot feed private train", inA.train, false, true);
            sourceB.transmit(15); worldSource.transmit(0);
            outcome("PRIVATE B cannot feed private A", inA.train, false, true);
            outcome("PRIVATE B sees its own source", inB.train, true, false);

            sourceA.transmit(15); sourceB.transmit(0);
            var twoA = train(level, List.of(Vec3.atLowerCornerOf(a.getPlot().getCenterBlock()), Vec3.atLowerCornerOf(a.getPlot().getCenterBlock().offset(1, 0, 4))));
            checks.check("all carriages in A reduce to A", ProbeTrain.membership(level, twoA.train).key().contains(a.getUniqueId().toString()));
            outcome("multi-carriage private condition", twoA.train, true, false);
            var mixed = train(level, List.of(Vec3.atLowerCornerOf(a.getPlot().getCenterBlock()), Vec3.atLowerCornerOf(b.getPlot().getCenterBlock())));
            checks.check("mixed private carriage instances are PENDING", ProbeTrain.membership(level, mixed.train).pending());
            outcome("mixed consumer defers both polarities", mixed.train, false, false);

            marker(a, false);
            checks.check("last isolator removal releases live train membership to WORLD", ProbeTrain.membership(level, inA.train).key().equals("WORLD"));
            outcome("live marker-free train keeps upstream query", inA.train, true, false);
            marker(a, true);
            checks.check("first isolator restores live private membership", ProbeTrain.membership(level, inA.train).key().contains(a.getUniqueId().toString()));

            var car = inA.train.carriages.getFirst();
            var state = car.getDimensionalIfPresent(level.dimension());
            var entity = state.entity.get();
            ((ProbeCarriageAccess) state).probe$removeAndSave(entity, false);
            checks.check("actual carriage unload clears weak live entity", state.entity.get() == null && entity.isRemoved());
            checks.check("offline plot anchors retain same-instance evidence", ProbeTrain.membership(level, inA.train).key().contains(a.getUniqueId().toString()));
            marker(a, false);
            checks.check("marker-free offline rail references resolve WORLD", ProbeTrain.membership(level, inA.train).key().equals("WORLD"));
            marker(a, true);
            var palette = new DimensionPalette();
            CompoundTag saved = inA.train.write(palette, level.registryAccess());
            Train restored = Train.read(saved, level.registryAccess(), Map.of(inA.graph.id, inA.graph), palette);
            checks.check("real Train NBT restores train ID", restored.id.equals(inA.train.id));
            checks.check("restored offline carriage scope derives from saved anchors", ProbeTrain.membership(level, restored).key().contains(a.getUniqueId().toString()));
            outcome("restored offline private condition", restored, true, false);

            cache(world.train, a);
            pending(world.train, restored, a, worldSource);
            expandingRoot(world.train, worldSource);

            int range = com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange.get();
            com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange.set(2);
            outcome("private global condition adds no distance limit", restored, true, false);
            com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange.set(range);

            var nether = level.getServer().getLevel(Level.NETHER);
            var netherChecks = new ScopeProbeAssertions(nether);
            var remote = netherChecks.link(new BlockPos(0, 200, 0), false);
            remote.transmit(9);
            outcome("WORLD global query still crosses dimensions", world.train, true, false);
            sourceA.transmit(0);
            outcome("cross-dimension WORLD source cannot feed private train", restored, false, true);
            remote.transmit(0);

            var extra = car.getDimensional(nether.dimension()); // fixture creation only
            checks.check("missing portal-side anchors are PENDING", ProbeTrain.membership(level, inA.train).pending());
            outcome("unresolved dimensional consumer defers both", inA.train, false, false);
            checks.check("train query wrapper really executes", ProbeTrain.filteredCalls > 0 && ProbeTrain.scans > 0 && ProbeTrain.pendingDeferrals > 0);
            coldCarrier(a, worldSource);
            reusedPlot();
        } finally {
            ProbeTrain.enabled = false;
            for (var entity : entities) if (!entity.isRemoved()) entity.discard();
            for (var craft : List.copyOf(SubLevelContainer.getContainer(level).getAllSubLevels()))
                SubLevelContainer.getContainer(level).removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
        }
    }

    private void cache(Train world, ServerSubLevel a) {
        var high = condition(false);
        CompoundTag context = new CompoundTag();
        checks.check("scoped absence is cached", !high.tickCompletion(level, world, context) && context.contains("LastChecked"));
        int power = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
        marker(a, false);
        checks.check("scope-only transition needs no listening receiver", Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get() == power);
        ProbeTrain.enabled = false;
        checks.check("negative raw LastChecked skips scope-only change", !high.tickCompletion(level, world, context));
        ProbeTrain.enabled = true;
        checks.check("scope epoch invalidates actual condition cache", high.tickCompletion(level, world, context));
        checks.check("unchanged decisive cache preserves upstream false return", !high.tickCompletion(level, world, context));
        context.putUUID("FieldworkProbeSession", UUID.randomUUID());
        checks.check("different server session invalidates cached stamp", high.tickCompletion(level, world, context));
        marker(a, true);
    }

    private void pending(Train world, Train privateTrain, ServerSubLevel a, RedstoneLinkBlockEntity publicSource) {
        var low = condition(true);
        var cached = new CompoundTag();
        checks.check("pre-root scoped absence completes LOW", low.tickCompletion(level, world, cached));
        long epoch = ProbeTrain.scopeEpoch;
        ProbeLifecycle.begin("train-pending-test", level, List.of(a.getPlot().getCenterBlock()));
        checks.check("root begin invalidates pre-root train cache", ProbeTrain.scopeEpoch != epoch);
        long scans = ProbeTrain.scans;
        checks.check("pre-root cached LOW defers during root", !low.tickCompletion(level, world, cached));
        checks.check("pre-root cache actually rechecks visibility", ProbeTrain.scans > scans);
        var context = new CompoundTag();
        checks.check("pending-only high cannot complete", !condition(false).tickCompletion(level, world, context));
        checks.check("pending does not commit checked cache", !context.contains("LastChecked") && !context.contains("FieldworkProbeScopeEpoch"));
        outcome("pending-only source defers low too", world, false, false);
        scans = ProbeTrain.scans;
        checks.check("same-root repeated LOW stays pending", !low.tickCompletion(level, world, cached));
        checks.check("pending result does not become same-root cache hit", ProbeTrain.scans > scans);
        publicSource.transmit(7);
        outcome("known allowed WORLD source overrides source pending", world, true, false);
        outcome("pending consumer cannot borrow WORLD positive", privateTrain, false, false);
        publicSource.transmit(0);
        int version = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
        ProbeLifecycle.end("train-pending-test", level, true);
        checks.check("pending resolution keeps Create power version unchanged", Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get() == version);
        outcome("pending private consumer resolves without power change", privateTrain, true, false);
    }

    /** Deliberately asserts the unsafe behavior of the current candidate, not a safety pass.
     * Controlled tracking proves the provenance-loss mechanism; it is not natural train motion. */
    private void coldCarrier(ServerSubLevel a, RedstoneLinkBlockEntity publicSource) {
        var fixture = train(level, List.of(new Vec3(768, 120, 0)));
        var carriage = fixture.train.carriages.getFirst();
        var state = carriage.getDimensionalIfPresent(level.dimension());
        var entity = state.entity.get();
        ((EntityMovementExtension) entity).sable$setTrackingSubLevel(a);
        checks.check("controlled projected WORLD-anchor carriage resolves private A while live",
            ProbeTrain.membership(level, fixture.train).key().contains(a.getUniqueId().toString()));
        ((ProbeCarriageAccess) state).probe$removeAndSave(entity, false);
        checks.check("warm projected carrier unload stays PENDING", ProbeTrain.membership(level, fixture.train).pending());
        var palette = new DimensionPalette();
        var saved = fixture.train.write(palette, level.registryAccess());
        Train cold = Train.read(saved, level.registryAccess(), Map.of(fixture.graph.id, fixture.graph), palette);
        checks.check("cold Train.read creates new carriage objects", cold.carriages.getFirst() != carriage);
        checks.check("UNSAFE negative control cold projected carrier silently becomes WORLD",
            ProbeTrain.membership(level, cold).key().equals("WORLD"));
        publicSource.transmit(7);
        checks.check("warm projected carrier still cannot consume WORLD", !condition(false).tickCompletion(level, fixture.train, new CompoundTag()));
        checks.check("UNSAFE negative control cold projected carrier consumes WORLD power",
            condition(false).tickCompletion(level, cold, new CompoundTag()));
        publicSource.transmit(0);
    }

    private void expandingRoot(Train world, RedstoneLinkBlockEntity publicSource) {
        publicSource.transmit(7);
        ProbeLifecycle.begin("train-expanding-root", level, List.of(new BlockPos(900, 120, 0)));
        try {
            var high = condition(false);
            var context = new CompoundTag();
            checks.check("known WORLD positive is decisive inside unrelated root", high.tickCompletion(level, world, context));
            long epoch = ProbeTrain.scopeEpoch;
            int power = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
            ProbeLifecycle.addDestination(level, List.of(publicSource.getBlockPos()));
            checks.check("same-root destination expansion quarantines positive source", ProbeLifecycle.quarantined(level, publicSource.getBlockPos()));
            checks.check("same-root expansion leaves power and epoch unchanged",
                power == Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get() && epoch == ProbeTrain.scopeEpoch);
            long scans = ProbeTrain.scans;
            checks.check("same-context upstream cached result remains false", !high.tickCompletion(level, world, context));
            checks.check("UNSAFE negative control expanded root skips scoped rescan", ProbeTrain.scans == scans);
            long deferrals = ProbeTrain.pendingDeferrals;
            checks.check("fresh context sees expanded source as PENDING", !high.tickCompletion(level, world, new CompoundTag())
                && ProbeTrain.pendingDeferrals > deferrals);
        } finally {
            ProbeLifecycle.end("train-expanding-root", level, true);
            publicSource.transmit(0);
        }
    }

    private void reusedPlot() {
        var a = craft(new BlockPos(128, 120, 0));
        var stored = a.getPlot().getCenterBlock();
        var fixture = train(level, List.of(Vec3.atLowerCornerOf(stored)));
        var state = fixture.train.carriages.getFirst().getDimensionalIfPresent(level.dimension());
        ((ProbeCarriageAccess) state).probe$removeAndSave(state.entity.get(), false);
        var palette = new DimensionPalette();
        var saved = fixture.train.write(palette, level.registryAccess());
        var data = SubLevelSerializer.toData(a, List.of());
        var oldId = a.getUniqueId();
        var container = SubLevelContainer.getContainer(level);
        container.removeSubLevel(a, SubLevelRemovalReason.REMOVED);
        UUID newId = UUID.randomUUID();
        var replacement = new SubLevelData(newId, data.bounds(), data.pose(), data.dependencies(), data.fullTag().copy());
        var b = SubLevelSerializer.fullyLoad(level, replacement);
        checks.check("train negative control reuses exact plot with different UUID",
            b != null && !oldId.equals(newId) && b.getPlot().getCenterBlock().equals(stored));
        checks.check("old craft absent and replacement is current plot owner",
            container.getSubLevel(oldId) == null && dev.ryanhcode.sable.Sable.HELPER.getContaining(level, stored) == b);
        checks.initialized(stored).transmit(15);
        Train cold = Train.read(saved, level.registryAccess(), Map.of(fixture.graph.id, fixture.graph), palette);
        checks.check("UNSAFE negative control stale train anchors acquire replacement UUID",
            ProbeTrain.membership(level, cold).key().contains(newId.toString()));
        checks.check("UNSAFE negative control stale train consumes replacement private power",
            condition(false).tickCompletion(level, cold, new CompoundTag()));
    }

    private void outcome(String name, Train train, boolean high, boolean low) {
        checks.check(name + " HIGH", condition(false).tickCompletion(level, train, new CompoundTag()) == high);
        checks.check(name + " LOW", condition(true).tickCompletion(level, train, new CompoundTag()) == low);
    }

    private RedstoneLinkCondition condition(boolean low) {
        var c = new RedstoneLinkCondition();
        c.setItem(0, new ItemStack(Items.DIAMOND)); c.setItem(1, new ItemStack(Items.EMERALD));
        c.getData().putInt("Inverted", low ? 1 : 0);
        return c;
    }

    private void marker(ServerSubLevel craft, boolean present) {
        BlockPos pos = craft.getPlot().getCenterBlock().offset(1, 0, 0);
        ProbeLifecycle.begin("train-marker", level, List.of(pos));
        level.setBlock(pos, present ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        ProbeLifecycle.end("train-marker", level, true);
    }

    private ServerSubLevel craft(BlockPos origin) {
        checks.link(origin, false);
        level.setBlock(origin.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        var blocks = List.of(origin, origin.below(), origin.offset(1, 0, 0));
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        checks.initialized(craft.getPlot().getCenterBlock()).transmit(15);
        return craft;
    }

    private TrainFixture train(ServerLevel world, List<Vec3> anchors) {
        var graph = new TrackGraph();
        var carriages = new ArrayList<Carriage>();
        for (Vec3 anchor : anchors) {
            var n1 = new TrackNode(new TrackNodeLocation(anchor).in(world), ++nodeId, new Vec3(0, 1, 0));
            var n2 = new TrackNode(new TrackNodeLocation(anchor.add(16, 0, 0)).in(world), ++nodeId, new Vec3(0, 1, 0));
            graph.addNode(n1); graph.addNode(n2);
            var edge = new TrackEdge(n1, n2, null, TrackMaterial.ANDESITE);
            graph.putConnection(n1, n2, edge);
            graph.putConnection(n2, n1, new TrackEdge(n2, n1, null, TrackMaterial.ANDESITE));
            var bogey = new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, new CompoundTag(),
                new TravellingPoint(n1, n2, edge, 6, false), new TravellingPoint(n1, n2, edge, 4, false));
            var carriage = new Carriage(bogey, null, 0);
            carriage.storage.initialize();
            carriages.add(carriage);
        }
        var train = new Train(UUID.randomUUID(), UUID.randomUUID(), graph, carriages, new ArrayList<>(Collections.nCopies(Math.max(0, carriages.size() - 1), 4)), false, 0);
        for (var carriage : carriages) {
            var contraption = new CarriageContraption(Direction.SOUTH);
            contraption.anchor = BlockPos.containing(anchors.get(carriages.indexOf(carriage)));
            contraption.getBlocks().put(BlockPos.ZERO, new StructureBlockInfo(BlockPos.ZERO, Blocks.STONE.defaultBlockState(), null));
            contraption.bounds = new AABB(0, 0, 0, 1, 1, 1);
            var entity = CarriageContraptionEntity.create(world, contraption);
            entity.setCarriage(carriage);
            var dimensional = carriage.getDimensional(world);
            dimensional.entity = new WeakReference<>(entity);
            dimensional.alignEntity(entity);
            world.getChunkAt(entity.blockPosition());
            checks.check("real carriage entity added to level", world.addFreshEntity(entity));
            entities.add(entity);
        }
        return new TrainFixture(train, graph);
    }
}
