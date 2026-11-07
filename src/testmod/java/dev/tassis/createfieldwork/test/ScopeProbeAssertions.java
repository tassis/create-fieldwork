package dev.tassis.createfieldwork.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScopeProbeAssertions {
    private final ServerLevel level;
    private final List<Map<String, Object>> assertions = new ArrayList<>();
    private final List<AbstractLinkedReceiverBlockEntity> simulated = new ArrayList<>();

    public ScopeProbeAssertions(ServerLevel level) { this.level = level; }
    public List<Map<String, Object>> results() { return assertions; }

    public void run() {
        BlockPos origin = new BlockPos(0, 120, 0);
        RedstoneLinkBlockEntity sender = link(origin, false);
        RedstoneLinkBlockEntity inside = link(origin.offset(3, 0, 0), true);
        RedstoneLinkBlockEntity outside = link(origin.offset(8, 0, 0), true);
        level.setBlock(origin.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        AllBlocks.REDSTONE_LINK.get().updateTransmittedSignal(sender.getBlockState(), level, origin);
        check("actual redstone block drives real transmitter", sender.getSignal() == 15);
        AbstractLinkedReceiverBlockEntity directional = simulated(origin.offset(10, 0, 0),
            SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST));
        ModulatingLinkedReceiverBlockEntity modulating = (ModulatingLinkedReceiverBlockEntity) simulated(origin.offset(12, 0, 0),
            SimBlocks.MODULATING_LINKED_RECEIVER.getDefaultState());
        refresh();
        signal("world/world baseline", outside, 15);
        simulatedSignal("directional receiver baseline", directional, 15);
        int modulatedBaseline = modulating.receivedSignal;
        double rawBaseline = modulating.rawSignalValue;
        check("modulating receiver has nonzero original response", modulatedBaseline > 0);

        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x <= 3; x++) {
            level.setBlock(origin.offset(x, -1, 0), x == 0 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(), 3);
            blocks.add(origin.offset(x, -1, 0));
        }
        blocks.add(origin);
        blocks.add(origin.offset(3, 0, 0));
        ServerSubLevel craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        check("real Sable craft allocated", craft != null);
        BlockPos delta = craft.getPlot().getCenterBlock().subtract(origin);
        sender = initialized(origin.offset(delta));
        inside = initialized(origin.offset(3, 0, 0).offset(delta));
        check("stored coordinates belong to actual craft", Sable.HELPER.getContaining(level, sender.getBlockPos()) == craft);
        check("storage coordinates exceed Create range", !sender.getBlockPos().closerThan(outside.getBlockPos(), 256));
        ProbeScope.ready(craft, false);
        sender.transmit(15);
        refresh();
        signal("Sable projected near craft/world", outside, 15);
        signal("same craft upstream", inside, 15);
        simulatedSignal("directional projected near craft/world", directional, 15);
        simulatedSignal("modulating projection preserved", modulating, modulatedBaseline);
        check("modulating raw value preserved", Math.abs(modulating.rawSignalValue - rawBaseline) < 1e-6);
        level.setBlock(directional.getBlockPos(), directional.getBlockState().setValue(DirectionalBlock.FACING, Direction.EAST), 3);
        refresh();
        simulatedSignal("directional back-facing still rejects", directional, 0);
        level.setBlock(directional.getBlockPos(), directional.getBlockState().setValue(DirectionalBlock.FACING, Direction.WEST), 3);

        ProbeScope.enabled = true;
        ProbeScope.ready(craft, true);
        refresh();
        signal("private craft blocks world receiver", outside, 0);
        signal("private craft retains internal receiver", inside, 15);
        int originalRange = AllConfigs.server().logistics.linkRange.get();
        AllConfigs.server().logistics.linkRange.set(2);
        refresh();
        signal("same private craft does not bypass range", inside, 0);
        AllConfigs.server().logistics.linkRange.set(originalRange);
        refresh();
        inside.getBehaviour(LinkBehaviour.TYPE).setFrequency(false, new ItemStack(Items.AMETHYST_SHARD));
        refresh();
        signal("same private craft does not bypass frequency", inside, 0);
        inside.getBehaviour(LinkBehaviour.TYPE).setFrequency(false, new ItemStack(Items.EMERALD));
        refresh();
        simulatedSignal("private craft blocks directional bypass", directional, 0);
        simulatedSignal("private craft blocks modulating bypass", modulating, 0);

        RedstoneLinkBlockEntity worldSource = link(origin.offset(8, 0, 4), false);
        worldSource.transmit(7);
        refresh();
        signal("refresh preserves legal world power 7 rather than clearing", outside, 7);
        signal("private internal power remains 15", inside, 15);
        sender.transmit(0);
        refresh();
        signal("world to private craft is blocked", inside, 0);
        ProbeScope.ready(craft, false);
        refresh();
        signal("world to non-private craft is restored", inside, 7);
        sender.transmit(15);
        ProbeScope.ready(craft, true);
        refresh();
        signal("restrictive scope clears stale external 15 to legal 7", outside, 7);
        worldSource.transmit(0);

        ProbeScope.ready(craft, false);
        refresh();
        signal("relaxing scope restores world receiver", outside, 15);
        simulatedSignal("relaxing scope restores directional", directional, 15);
        simulatedSignal("relaxing scope restores modulation", modulating, modulatedBaseline);

        BlockPos otherOrigin = origin.offset(0, 0, 8);
        link(otherOrigin, true);
        List<BlockPos> otherBlocks = List.of(otherOrigin, otherOrigin.below());
        ServerSubLevel other = SubLevelAssemblyHelper.assembleBlocks(level, otherOrigin, otherBlocks, BoundingBox3i.from(otherBlocks));
        RedstoneLinkBlockEntity otherReceiver = initialized(other.getPlot().getCenterBlock());
        ProbeScope.ready(other, false);
        refresh();
        signal("two non-isolated crafts preserve communication", otherReceiver, 15);
        ProbeScope.ready(craft, true);
        refresh();
        signal("private craft A to non-private craft B blocked", otherReceiver, 0);
        ProbeScope.ready(craft, false);
        ProbeScope.ready(other, true);
        refresh();
        signal("non-private craft A to private craft B blocked", otherReceiver, 0);
        ProbeScope.ready(craft, true);
        refresh();
        signal("different private craft IDs blocked", otherReceiver, 0);
        ProbeScope.ready(craft, false);

        craft.logicalPose().position().add(4096, 0, 0);
        craft.updateLastPose();
        craft.forceUpdateGlobalBounds();
        refresh();
        signal("projected far craft/world denied by upstream range", outside, 0);
        signal("internal links survive whole-craft translation", inside, 15);
        simulatedSignal("directional projected far range still enforced", directional, 0);
        simulatedSignal("modulating projected far range still enforced", modulating, 0);
        check("Create hook actually executed", ProbeScope.networkCalls > 0);
        check("Simulated hook actually executed", ProbeScope.simulatedCalls > 0);

        SubLevelContainer.getContainer(level).removeSubLevel(other, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
        SubLevelContainer.getContainer(level).removeSubLevel(craft, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
        lifecycle();
    }

    private void lifecycle() {
        var container = SubLevelContainer.getContainer(level);
        container.addObserver(new SubLevelObserver() {
            @Override public void onSubLevelAdded(SubLevel craft) {
                if (!ProbeLifecycle.enabled) return;
                ProbeScope.initializing(craft);
                ProbeLifecycle.checkpoint("onAdded");
                if (!ProbeScope.isPrivate(craft)) ProbeLifecycle.violations.add("onAdded was not fail-closed");
            }
            @Override public void onSubLevelRemoved(SubLevel craft, SubLevelRemovalReason reason) {
                if (ProbeLifecycle.enabled) ProbeScope.initializing(craft);
            }
        });
        BlockPos origin = new BlockPos(64, 120, 0);
        RedstoneLinkBlockEntity source = link(origin, false);
        link(origin.offset(6, 0, 0), true);
        RedstoneLinkBlockEntity outside = link(origin.offset(10, 0, 0), true);
        source.transmit(15);
        List<BlockPos> positions = new ArrayList<>();
        for (int x = 0; x <= 6; x++) {
            level.setBlock(origin.offset(x, -1, 0), Blocks.STONE.defaultBlockState(), 3);
            positions.add(origin.offset(x, -1, 0));
        }
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        positions.add(origin); positions.add(origin.offset(6, 0, 0)); positions.add(origin.offset(1, 0, 0));
        refresh();
        signal("lifecycle initial world source", outside, 15);
        ProbeLifecycle.enabled = true;
        ProbeLifecycle.afterCommit = this::refresh;
        ProbeLifecycle.observation = () -> {
            refresh();
            if (outside.getReceivedSignal() != 0) throw new AssertionError("in-flight source remained shared: " + outside.getReceivedSignal());
        };
        int commits = ProbeLifecycle.commits;
        ServerSubLevel craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        check("assembly intermediate queries stayed closed", ProbeLifecycle.violations.isEmpty());
        check("nested move commits only once at outer assembly", ProbeLifecycle.commits == commits + 1 && ProbeLifecycle.depth == 0);
        check("marker block identity reconstructed", ProbeLifecycle.markers(craft).size() == 1 && ProbeScope.isPrivate(craft));
        BlockPos stored = craft.getPlot().getCenterBlock();
        source = initialized(stored);
        source.transmit(15);
        refresh();
        signal("assembly committed private source", outside, 0);

        ProbeLifecycle.observation = null;
        BlockPos firstMarker = stored.offset(1, 0, 0);
        BlockPos secondMarker = stored.offset(2, 0, 0);
        mutateMarker(secondMarker, true);
        check("two marker positions reconstructed", ProbeLifecycle.markers(craft).size() == 2);
        mutateMarker(secondMarker, true);
        check("duplicate marker mutation is idempotent", ProbeLifecycle.markers(craft).size() == 2);
        mutateMarker(firstMarker, false);
        signal("removing one of two markers keeps isolation", outside, 0);
        mutateMarker(secondMarker, false);
        signal("last marker removal restores shared world", outside, 15);
        mutateMarker(firstMarker, true);
        signal("first marker placement clears stale shared power", outside, 0);
        ProbeLifecycle.observation = () -> {
            refresh();
            if (outside.getReceivedSignal() != 0) throw new AssertionError("load leaked signal");
        };

        var uuid = craft.getUniqueId();
        var holding = container.getHoldingChunkMap();
        holding.moveToUnloaded(craft, new ChunkPos(origin));
        check("real unload enters holding map", holding.getHoldingSubLevel(uuid) != null);
        commits = ProbeLifecycle.commits;
        holding.loadHoldingSubLevel(holding.getHoldingSubLevel(uuid));
        ServerSubLevel restored = (ServerSubLevel) container.getSubLevel(uuid);
        check("real serializer restores UUID", restored != null && restored.getUniqueId().equals(uuid));
        check("load publishes once after all chunks", ProbeLifecycle.commits == commits + 1);
        check("load callbacks stayed private", ProbeLifecycle.violations.isEmpty());
        check("load reconstructs marker without BE registration", ProbeLifecycle.markers(restored).size() == 1 && ProbeScope.isPrivate(restored));
        source = initialized(stored);
        initialized(stored.offset(6, 0, 0));
        source.transmit(15);
        refresh();
        signal("restored private craft still denies world", outside, 0);
        check("restored craft remains serializable", SubLevelSerializer.toData(restored, List.of()) != null);

        var duplicateData = SubLevelSerializer.toData(restored, List.of());
        commits = ProbeLifecycle.commits;
        check("duplicate load returns null without committing", SubLevelSerializer.fullyLoad(level, duplicateData) == null && ProbeLifecycle.commits == commits);
        check("failed load preserves live private state", ProbeScope.isPrivate(restored) && ProbeLifecycle.depth == 0);

        // Exercise exception unwinding after chunk/BE load, then retry from the
        // real holding entry. A normal RETURN-only hook would miss this path.
        holding.moveToUnloaded(restored, new ChunkPos(origin));
        var retained = holding.getHoldingSubLevel(uuid);
        ProbeLifecycle.failAtCheckpoint = "loaded-chunk";
        boolean thrown = false;
        commits = ProbeLifecycle.commits;
        try { holding.loadHoldingSubLevel(retained); }
        catch (ProbeLifecycle.InjectedFailure expected) { thrown = true; }
        finally { ProbeLifecycle.failAtCheckpoint = null; }
        var partial = (ServerSubLevel) container.getSubLevel(uuid);
        check("load exception unwinds without success commit", thrown && ProbeLifecycle.depth == 0 && ProbeLifecycle.commits == commits);
        check("partially loaded craft remains fail-closed", partial != null && ProbeScope.isPrivate(partial));
        container.removeSubLevel(partial, SubLevelRemovalReason.REMOVED);
        ProbeLifecycle.discardRemovedLoad(level, uuid);
        holding.loadHoldingSubLevel(retained);
        restored = (ServerSubLevel) container.getSubLevel(uuid);
        check("load retry reconstructs block identity", restored != null && ProbeLifecycle.markers(restored).size() == 1 && ProbeScope.isPrivate(restored));
        initialized(stored).transmit(15);
        initialized(stored.offset(6, 0, 0));

        ProbeLifecycle.observation = null;
        for (int i = 0; i < 128; i++) restored.getHeatMapManager().tick();
        int beforeSplit = container.getAllSubLevels().size();
        level.setBlock(stored.offset(3, -1, 0), Blocks.AIR.defaultBlockState(), 3);
        for (int i = 0; i < 512 && container.getAllSubLevels().size() == beforeSplit; i++) restored.getHeatMapManager().tick();
        check("actual heat map creates split child", container.getAllSubLevels().size() > beforeSplit);
        check("split wrapper was exercised", ProbeLifecycle.events.stream().anyMatch(e -> e.get("stage").equals("begin:split")));
        check("split nested depth fully unwinds", ProbeLifecycle.depth == 0);
        for (var child : List.copyOf(container.getAllSubLevels())) {
            check("split registry matches child block identity " + child.getUniqueId(), ProbeScope.isPrivate(child) == !ProbeLifecycle.markers(child).isEmpty());
            check("split child remains serializable " + child.getUniqueId(), SubLevelSerializer.toData(child, List.of()) != null);
        }

        ServerSubLevel marked = container.getAllSubLevels().stream().filter(s -> !ProbeLifecycle.markers(s).isEmpty()).findFirst().orElseThrow();
        BlockPos target = new BlockPos(96, 120, 0);
        var transform = new SubLevelAssemblyHelper.AssemblyTransform(marked.getPlot().getCenterBlock(), target, 0, Rotation.NONE, level);
        var destinations = ProbeLifecycle.blocks(marked).stream().map(transform::apply).toList();
        var destinationMarkers = ProbeLifecycle.markers(marked).stream().map(transform::apply).toList();
        ProbeLifecycle.observation = () -> {
            for (var pos : destinations) {
                if (ProbeScope.allows(level, pos, outside.getBlockPos())) throw new AssertionError("destination prematurely shared: " + pos);
            }
            if (!ProbeScope.allows(level, new BlockPos(200, 120, 200), new BlockPos(210, 120, 200)))
                throw new AssertionError("whole world was quarantined");
        };
        commits = ProbeLifecycle.commits;
        SimAssemblyHelper.disassembleSubLevel(level, marked, marked.getPlot().getCenterBlock(), target, Rotation.NONE, false);
        check("disassembly outer transaction commits once", ProbeLifecycle.commits == commits + 1 && ProbeLifecycle.depth == 0);
        check("disassembly marker returned to ordinary world", destinationMarkers.stream().allMatch(p -> Sable.HELPER.getContaining(level, p) == null && level.getBlockState(p).is(Blocks.OBSIDIAN)));
        check("all recorded lifecycle observations passed", ProbeLifecycle.violations.isEmpty());
        ProbeLifecycle.observation = null;
        // The ordinary server tick removes empty/removed plots before save. This
        // suite performed multiple lifecycle operations synchronously in one tick.
        container.processSubLevelRemovals();
        negativeQuarantineControl();
    }

    private void mutateMarker(BlockPos pos, boolean present) {
        ProbeLifecycle.begin("marker-mutation", level, List.of(pos));
        level.setBlock(pos, present ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        ProbeLifecycle.end("marker-mutation", level, true);
    }

    private void negativeQuarantineControl() {
        BlockPos origin = new BlockPos(128, 120, 0);
        var sender = link(origin, false);
        var outside = link(origin.offset(8, 0, 0), true);
        sender.transmit(15);
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        List<BlockPos> blocks = List.of(origin, origin.below(), origin.offset(1, 0, 0));
        boolean[] observed = {false};
        ProbeLifecycle.useQuarantine = false;
        ProbeLifecycle.observation = () -> {
            refresh();
            observed[0] |= outside.getReceivedSignal() > 0;
        };
        try {
            SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        } finally {
            ProbeLifecycle.useQuarantine = true;
            ProbeLifecycle.observation = null;
        }
        check("negative control detects in-flight source without position quarantine", observed[0]);
        signal("negative control still becomes private after commit", outside, 0);
        containerCleanup();
    }

    private void containerCleanup() {
        var container = SubLevelContainer.getContainer(level);
        container.processSubLevelRemovals();
        for (var craft : List.copyOf(container.getAllSubLevels())) container.removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
    }

    RedstoneLinkBlockEntity link(BlockPos pos, boolean receiver) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState()
            .setValue(RedstoneLinkBlock.RECEIVER, false).setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        RedstoneLinkBlockEntity entity = initialized(pos);
        if (receiver) {
            // Follow the real transmitter -> receiver mode transition. A brand-new
            // BE placed directly in receiver state has not read deferred behaviours.
            level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3);
            entity.tick();
        }
        LinkBehaviour behaviour = entity.getBehaviour(LinkBehaviour.TYPE);
        behaviour.setFrequency(true, new ItemStack(Items.DIAMOND));
        behaviour.setFrequency(false, new ItemStack(Items.EMERALD));
        return entity;
    }

    RedstoneLinkBlockEntity initialized(BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RedstoneLinkBlockEntity entity)) {
            throw new AssertionError("No actual Redstone Link BE at " + pos);
        }
        entity.tick();
        return entity;
    }

    void refresh() {
        var networks = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level);
        for (var bucket : List.copyOf(networks.values())) {
            for (var endpoint : List.copyOf(bucket)) {
                if (endpoint.isAlive() && endpoint.isListening()) {
                    Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, endpoint);
                }
            }
        }
        for (var receiver : simulated) receiver.updateSignal();
    }

    AbstractLinkedReceiverBlockEntity simulated(BlockPos pos, BlockState state) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, state, 3);
        AbstractLinkedReceiverBlockEntity receiver = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(pos);
        receiver.tick();
        LinkBehaviour behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
        behaviour.setFrequency(true, new ItemStack(Items.DIAMOND));
        behaviour.setFrequency(false, new ItemStack(Items.EMERALD));
        simulated.add(receiver);
        return receiver;
    }

    void simulatedSignal(String name, AbstractLinkedReceiverBlockEntity receiver, int expected) {
        int actual = receiver.receivedSignal;
        assertions.add(Map.of("name", name, "expected", expected, "actual", actual, "passed", expected == actual));
        if (actual != expected) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }

    void signal(String name, RedstoneLinkBlockEntity receiver, int expected) {
        int actual = receiver.getReceivedSignal();
        assertions.add(Map.of("name", name, "expected", expected, "actual", actual, "passed", expected == actual));
        if (actual != expected) throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }

    void check(String name, boolean passed) {
        assertions.add(Map.of("name", name, "passed", passed));
        if (!passed) throw new AssertionError(name);
    }
}
