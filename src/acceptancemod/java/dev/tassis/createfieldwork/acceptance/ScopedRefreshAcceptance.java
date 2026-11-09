package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllDataComponents;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/** Work-count assertions and adversarial inputs, not an MSPT benchmark or scope oracle. */
final class ScopedRefreshAcceptance {
    private final ServerLevel level;
    private final ServerLevel nether;
    private final BiConsumer<String, Boolean> check;
    private final Couple<Frequency> a = key(Items.DIAMOND, Items.EMERALD);
    private final Couple<Frequency> b = key(Items.GOLD_INGOT, Items.LAPIS_LAZULI);
    private ServerSubLevel craft;
    private BlockPos stored, marker, extra;
    private RedstoneLinkBlockEntity outside;
    private Source bSource;

    ScopedRefreshAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) {
        this.level = level; this.nether = level.getServer().getLevel(Level.NETHER); this.check = check;
    }

    void run() {
        setup();
        boundaryAndPrivateCounts();
        fastPathFailuresAndNestedWrites();
        oldNewFrequencyUnion();
        oldNewDimensionUnion();
        reentrantWorkMerge();
        pendingFallback();
        simulatedUnloadCleanup();
        trackingOnlyProvenance();
        foreignControllerDependency();
        check.accept("scoped refresh suite leaves no failed transaction", state().transitions().failedTokens().isEmpty());
    }

    private FieldworkServerState state() { return FieldworkStateAccess.get(level.getServer()); }
    private void setup() {
        add(level, new Source(new BlockPos(0, 120, 0), a, 7));
        bSource = new Source(new BlockPos(0, 120, 32), b, 7); add(level, bSource);
        outside = link(level, new BlockPos(8, 120, 0), a, true);
        link(level, new BlockPos(8, 120, 32), b, true);
        simulated(level, new BlockPos(40, 120, 0), a);
        simulated(level, new BlockPos(40, 120, 32), b);
        add(nether, new Source(new BlockPos(0, 200, 0), a, 7));
        link(nether, new BlockPos(8, 200, 0), a, true);
        simulated(nether, new BlockPos(40, 200, 0), a);
        BlockPos origin = new BlockPos(32, 120, 0);
        link(level, origin, a, false); link(level, origin.offset(3, 0, 0), a, true);
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 4; x++) {
            var pos = origin.offset(x, -1, 0);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3); positions.add(pos);
        }
        positions.add(origin); positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(3, 0, 0));
        craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        stored = craft.getPlot().getCenterBlock(); marker = stored.offset(1, 0, 0); extra = stored.offset(2, 0, 0);
        var source = (RedstoneLinkBlockEntity) level.getBlockEntity(stored); source.tick(); source.transmit(15);
        ((RedstoneLinkBlockEntity) level.getBlockEntity(stored.offset(3, 0, 0))).tick();
        state().refresh().refresh(null);
        check.accept("scoped fixture has private15 and legal outside7", outside.getReceivedSignal() == 7);
        RefreshMetrics.measure(() -> state().refresh().refresh(null));
        check.accept("counter negative control detects full-refresh B bucket", count(level, b, false) > 0 && count(level, b, true) > 0);
        check.accept("counter negative control detects full-refresh other dimension", count(nether, a, false) > 0 && count(nether, a, true) > 0);
    }

    private void boundaryAndPrivateCounts() {
        RefreshMetrics.measure(() -> level.removeBlock(marker, false));
        check.accept("last-marker scope collection includes A before registry.begin", count(level, a, false) > 0 && count(level, a, true) > 0);
        excluded("last-marker removal");
        check.accept("last-marker removal performs zero cold calls in unrelated dimension", RefreshMetrics.total(nether.dimension()) == 0);
        check.accept("scoped last-marker return already exposes WORLD15", outside.getReceivedSignal() == 15);
        RefreshMetrics.measure(() -> level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3));
        excluded("first-marker placement");
        check.accept("first-marker placement performs zero cold calls in unrelated dimension", RefreshMetrics.total(nether.dimension()) == 0);
        check.accept("scoped first-marker return immediately withdraws to7", outside.getReceivedSignal() == 7);
        RefreshMetrics.measure(() -> level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3));
        check.accept("one-to-two private markers perform zero cold receiver updates", RefreshMetrics.total() == 0);
        RefreshMetrics.measure(() -> level.removeBlock(extra, false));
        check.accept("two-to-one private markers perform zero cold receiver updates", RefreshMetrics.total() == 0);
    }

    private void fastPathFailuresAndNestedWrites() {
        MarkerWriteFaults.arm(MarkerWriteFaults.NO_OP, extra, () -> check.accept("fast add stages index before attempted write", positions().contains(extra)));
        try { RefreshMetrics.measure(() -> level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3)); }
        finally { MarkerWriteFaults.clear(); }
        check.accept("no-op fast add restores actual marker index", !positions().contains(extra) && level.getBlockState(extra).isAir() && RefreshMetrics.total() == 0);
        MarkerWriteFaults.arm(MarkerWriteFaults.THROW, extra, () -> {});
        boolean[] thrown = {false};
        try { RefreshMetrics.measure(() -> {
            try { level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); }
            catch (IllegalStateException expected) { thrown[0] = expected.getMessage().equals("acceptance marker write failure"); }
        }); } finally { MarkerWriteFaults.clear(); }
        check.accept("failed fast add rolls index back without weakening privacy", thrown[0] && !positions().contains(extra) && positions().contains(marker) && RefreshMetrics.total() == 0);
        level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        MarkerWriteFaults.arm(MarkerWriteFaults.NO_OP, extra, () -> check.accept("fast removal stages index before attempted write", !positions().contains(extra)));
        try { RefreshMetrics.measure(() -> level.removeBlock(extra, false)); } finally { MarkerWriteFaults.clear(); }
        check.accept("no-op fast removal restores actual marker index", positions().contains(extra) && positions().size() == 2 && RefreshMetrics.total() == 0);
        MarkerWriteFaults.arm(MarkerWriteFaults.THROW, extra, () -> {});
        thrown[0] = false;
        try { RefreshMetrics.measure(() -> {
            try { level.removeBlock(extra, false); }
            catch (IllegalStateException expected) { thrown[0] = expected.getMessage().equals("acceptance marker write failure"); }
        }); } finally { MarkerWriteFaults.clear(); }
        check.accept("failed fast removal restores both actual markers", thrown[0] && positions().size() == 2 && RefreshMetrics.total() == 0);
        MarkerWriteFaults.arm(MarkerWriteFaults.AFTER_WRITE, extra, () -> level.removeBlock(marker, false));
        try { RefreshMetrics.measure(() -> level.removeBlock(extra, false)); } finally { MarkerWriteFaults.clear(); }
        check.accept("nested last-marker removal uses full synchronous boundary", positions().isEmpty() && count(level, a, false) > 0 && outside.getReceivedSignal() == 15);
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        MarkerWriteFaults.arm(MarkerWriteFaults.AFTER_WRITE, extra, () -> level.removeBlock(marker, false));
        try { RefreshMetrics.measure(() -> level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3)); }
        finally { MarkerWriteFaults.clear(); }
        check.accept("nested marker replacement stays private with actual witness", positions().size() == 1 && positions().contains(extra) && RefreshMetrics.total() == 0 && outside.getReceivedSignal() == 7);
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); level.removeBlock(extra, false);
        level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        MarkerWriteFaults.arm(MarkerWriteFaults.AFTER_WRITE, extra, () -> {
            level.removeBlock(marker, false);
            level.setBlock(extra, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        });
        try { RefreshMetrics.measure(() -> level.removeBlock(extra, false)); }
        finally { MarkerWriteFaults.clear(); }
        check.accept("stale outer onRemove cannot erase a reentrant replacement marker", positions().size() == 1 && positions().contains(extra)
            && level.getBlockState(extra).is(FieldworkContent.ISOLATOR.get()) && outside.getReceivedSignal() == 7);
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); level.removeBlock(extra, false);
        check.accept("fast-path adversarial writes leave no FAILED token", state().transitions().failedTokens().isEmpty());
    }

    private void oldNewFrequencyUnion() {
        var oldKey = key(Items.REDSTONE, Items.GLOWSTONE_DUST);
        var newKey = key(Items.IRON_INGOT, Items.COPPER_INGOT);
        var source = new Source(stored, oldKey, 11);
        add(level, source);
        var oldReceiver = new Receiver(new BlockPos(8, 120, 64), oldKey); add(level, oldReceiver);
        var newReceiver = new Receiver(new BlockPos(12, 120, 64), newKey); add(level, newReceiver);
        RefreshMetrics.measure(() -> {
            var tx = state().transitions(); tx.begin("acceptance-frequency-change", level, List.of(stored));
            boolean success = false;
            try {
                tx.mutating(); Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, source);
                source.key = newKey; add(level, source); success = true;
            } finally { tx.end("acceptance-frequency-change", success); }
        });
        check.accept("transaction retains old key after endpoint frequency disappears", count(level, oldKey, false) > 0);
        check.accept("transaction adds new endpoint key before final publication", count(level, newKey, false) > 0);
        excluded("old/new key transaction");
        remove(level, source); remove(level, oldReceiver); remove(level, newReceiver);
    }

    private void oldNewDimensionUnion() {
        var oldKey = key(Items.COAL, Items.FLINT); var newKey = key(Items.STRING, Items.FEATHER);
        var oldPos = new BlockPos(0, 120, 80); var newPos = new BlockPos(0, 200, 80);
        var old = new Source(oldPos, oldKey, 11); var moved = new Source(newPos, newKey, 11);
        var oldReceiver = new Receiver(new BlockPos(8, 120, 80), oldKey);
        var newReceiver = new Receiver(new BlockPos(8, 200, 80), newKey);
        add(level, old); add(level, oldReceiver); add(nether, newReceiver);
        RefreshMetrics.measure(() -> {
            var tx = state().transitions(); tx.begin("acceptance-dimension-change", level, List.of(oldPos));
            boolean success = false;
            try {
                tx.destinations(nether, List.of(newPos)); tx.mutating(); remove(level, old); add(nether, moved); success = true;
            } finally { tx.end("acceptance-dimension-change", success); }
        });
        check.accept("old dimension/key is retained after source removal", count(level, oldKey, false) > 0 && oldReceiver.signal == 0);
        check.accept("new dimension/key is added after destination registration", count(nether, newKey, false) > 0 && newReceiver.signal == 11);
        excluded("cross-dimension old/new union");
        remove(nether, moved); remove(level, oldReceiver); remove(nether, newReceiver);
    }

    private void reentrantWorkMerge() {
        var callback = new Receiver(new BlockPos(12, 120, 0), a); add(level, callback);
        boolean[] requested = {false};
        callback.callback = value -> {
            if (value == 15 && !requested[0]) {
                requested[0] = true;
                bSource.strength = 9;
                state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), b));
            }
        };
        RefreshMetrics.measure(() -> level.removeBlock(marker, false));
        check.accept("callback adds new work after initial A-only selection", requested[0]);
        check.accept("reentrant B request is merged into a later pass", count(level, b, false) > 0 && count(level, b, true) > 0);
        check.accept("reentrant scope merging still excludes unrelated dimension", count(nether, a, false) == 0 && count(nether, a, true) == 0);
        check.accept("merged B work actually updates its receiver", ((RedstoneLinkBlockEntity) level.getBlockEntity(new BlockPos(8, 120, 32))).getReceivedSignal() == 9);
        callback.callback = ignored -> {}; remove(level, callback);
        bSource.strength = 7; state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), b));
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
    }

    private void pendingFallback() {
        var pos = stored.offset(4, 0, 0);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        RefreshMetrics.measure(() -> level.removeBlock(marker, false));
        check.accept("unregistered affected source widens only its dimension", count(level, b, false) > 0 && count(level, b, true) > 0);
        check.accept("unknown frequency fallback does not refresh other dimension", count(nether, a, false) == 0 && count(nether, a, true) == 0);
        check.accept("unknown WORLD publication remains PENDING for train conditions", state().publications().pendingWorld());
        ((RedstoneLinkBlockEntity) level.getBlockEntity(pos)).tick(); level.removeBlock(pos, false);
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
    }

    private void trackingOnlyProvenance() {
        level.removeBlock(marker, false);
        var trackingKey = key(Items.SLIME_BALL, Items.BONE);
        var receiver = link(level, new BlockPos(8, 120, 48), trackingKey, true);
        var actor = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "TrackingProbe"));
        actor.setPos(0.5, 120, 0.5);
        ((EntityMovementExtension) actor).sable$setTrackingSubLevel(null);
        state().playerSessions().bind(actor);
        var item = AllItems.LINKED_CONTROLLER.asStack();
        item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.BONE))));
        actor.setItemInHand(InteractionHand.MAIN_HAND, item);
        try {
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            state().refresh().refresh(null);
            check.accept("tracking-only fixture starts as public controller", receiver.getReceivedSignal() == 15);
            // Tracking-only player state is deliberately not controller authority.
            // An ordinary handheld remains WORLD and must not enter the craft footprint.
            ((EntityMovementExtension) actor).sable$setTrackingSubLevel(craft);
            RefreshMetrics.measure(() -> level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3));
            check.accept("scope collection excludes tracking-only ordinary controller provenance", count(level, trackingKey, false) == 0);
            check.accept("tracking-only state does not retarget ordinary WORLD handheld", receiver.getReceivedSignal() == 15);
            excluded("tracking-only controller marker placement");
        } finally {
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level); actor.discard();
        }
    }

    private void foreignControllerDependency() {
        level.removeBlock(marker, false);
        var foreignKey = key(Items.WHEAT, Items.BEETROOT_SEEDS);
        var receiver = link(nether, new BlockPos(8, 200, 80), foreignKey, true);
        var actor = FakePlayerFactory.get(nether, new GameProfile(UUID.randomUUID(), "ForeignProbe"));
        actor.setPos(.5, 200, 80.5);
        ((EntityMovementExtension) actor).sable$setTrackingSubLevel(null);
        state().playerSessions().bind(actor);
        var item = AllItems.LINKED_CONTROLLER.asStack();
        item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.BEETROOT_SEEDS))));
        actor.setItemInHand(InteractionHand.MAIN_HAND, item);
        try {
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("foreign controller fixture starts as WORLD source", receiver.getReceivedSignal() == 15);
            // Controlled producer/session mismatch. Ordinary handheld routing
            // remains WORLD until native timeout and player craft tracking is irrelevant.
            actor.setServerLevel(level);
            ((EntityMovementExtension) actor).sable$setTrackingSubLevel(craft);
            RefreshMetrics.measure(() -> level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3));
            check.accept("craft change excludes unrelated foreign ordinary controller", count(nether, foreignKey, false) == 0 && receiver.getReceivedSignal() == 15);
            excluded("foreign ordinary controller placement");
            RefreshMetrics.measure(() -> level.removeBlock(marker, false));
            check.accept("last marker excludes unrelated foreign ordinary controller", count(nether, foreignKey, false) == 0 && receiver.getReceivedSignal() == 15);
            excluded("foreign ordinary controller removal");
            level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        } finally {
            LinkedControllerServerHandler.receivePressed(nether, actor.blockPosition(), actor.getUUID(), List.of(foreignKey), false);
            LinkedControllerServerHandler.tick(nether); actor.discard();
        }
    }

    private void simulatedUnloadCleanup() {
        var key = key(Items.BONE, Items.CLAY_BALL);
        var pos = new BlockPos(80, 120, 0);
        simulated(level, pos, key);
        var receiver = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(pos);
        var behaviour = receiver.getBehaviour(LinkBehaviour.TYPE);
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, behaviour);
        state().refresh().remember(receiver);
        state().refresh().unloaded(level, new net.minecraft.world.level.ChunkPos(pos));
        RefreshMetrics.measure(() -> state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), key)));
        check.accept("chunk unload removes Simulated receiver from Fieldwork index", count(level, key, true) == 0);
        state().refresh().remember(receiver);
        RefreshMetrics.measure(() -> state().refresh().refresh(null, RefreshScope.frequency(level.dimension(), key)));
        check.accept("loaded Simulated receiver can be indexed again after unload cleanup", count(level, key, true) > 0);
        level.removeBlock(pos, false); level.removeBlock(pos.below(), false);
    }

    private java.util.Set<BlockPos> positions() { return state().registry().snapshot(IsolationRuntime.key(craft)).positions(); }
    private int count(ServerLevel dimension, Couple<Frequency> key, boolean simulated) { return RefreshMetrics.count(dimension.dimension(), key, simulated); }
    private void excluded(String name) {
        check.accept(name + " leaves unrelated Create B bucket untouched", count(level, b, false) == 0);
        check.accept(name + " leaves unrelated Simulated B receiver untouched", count(level, b, true) == 0);
        check.accept(name + " leaves unrelated A bucket in other dimension untouched", count(nether, a, false) == 0 && count(nether, a, true) == 0);
    }
    private static Couple<Frequency> key(net.minecraft.world.item.Item first, net.minecraft.world.item.Item second) {
        return Couple.create(Frequency.of(new ItemStack(first)), Frequency.of(new ItemStack(second)));
    }
    private static void frequency(LinkBehaviour behaviour, Couple<Frequency> key) {
        behaviour.setFrequency(true, key.getFirst().getStack()); behaviour.setFrequency(false, key.getSecond().getStack());
    }
    private RedstoneLinkBlockEntity link(ServerLevel world, BlockPos pos, Couple<Frequency> key, boolean receiver) {
        world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        world.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = (RedstoneLinkBlockEntity) world.getBlockEntity(pos); entity.tick();
        if (receiver) { world.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); entity.tick(); }
        frequency(entity.getBehaviour(LinkBehaviour.TYPE), key); return entity;
    }
    private void simulated(ServerLevel world, BlockPos pos, Couple<Frequency> key) {
        world.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        world.setBlock(pos, SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST), 3);
        var receiver = (AbstractLinkedReceiverBlockEntity) world.getBlockEntity(pos); receiver.tick();
        frequency(receiver.getBehaviour(LinkBehaviour.TYPE), key); receiver.updateSignal();
    }
    private static void add(ServerLevel level, IRedstoneLinkable endpoint) { Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, endpoint); }
    private static void remove(ServerLevel level, IRedstoneLinkable endpoint) { Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint); }
    private static class Source implements IRedstoneLinkable {
        final BlockPos pos; Couple<Frequency> key; int strength;
        Source(BlockPos pos, Couple<Frequency> key, int strength) { this.pos = pos; this.key = key; this.strength = strength; }
        @Override public int getTransmittedStrength() { return strength; }
        @Override public void setReceivedStrength(int value) {}
        @Override public boolean isListening() { return false; }
        @Override public boolean isAlive() { return true; }
        @Override public BlockPos getLocation() { return pos; }
        @Override public Couple<Frequency> getNetworkKey() { return key; }
    }
    private static final class Receiver extends Source {
        int signal; IntConsumer callback = ignored -> {};
        Receiver(BlockPos pos, Couple<Frequency> key) { super(pos, key, 0); }
        @Override public boolean isListening() { return true; }
        @Override public void setReceivedStrength(int value) { signal = value; callback.accept(value); }
    }
}
