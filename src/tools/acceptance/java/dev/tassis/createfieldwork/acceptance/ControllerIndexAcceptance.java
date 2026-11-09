package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.controller.routing.ControllerEndpoint;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Test-only observation of server-owned endpoint index lifecycle. */
final class ControllerIndexAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;

    ControllerIndexAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) {
        this.level = level;
        this.check = check;
    }

    void run() throws Exception {
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "IndexCase"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        var floor = new BlockPos(0, 119, 96);
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        actor.setPos(.5, 120, 96.5);
        level.addNewPlayer(actor);
        var controller = AllItems.LINKED_CONTROLLER.asStack();
        controller.set(AllDataComponents.LINKED_CONTROLLER_ITEMS,
            ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        actor.setItemInHand(InteractionHand.MAIN_HAND, controller);
        var sources = FieldworkStateAccess.get(level.getServer()).controllers();
        try {
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("successful native add commits one identity-owned endpoint", size(sources, "endpoints") == 1);
            check.accept("successful native add commits player reverse index", selected(sources, "byPlayer", actor.getUUID()) == 1);
            check.accept("successful native add commits level reverse index", size(sources, "byLevel") == 1);

            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("duplicate press updates rather than duplicating endpoint ownership", size(sources, "endpoints") == 1);

            java.util.Collection<?> entries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            var endpoint = (IRedstoneLinkable) entries.iterator().next();
            var tagged = (ControllerEndpoint) endpoint;

            var other = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "IndexUnrelated"), ClientInformation.createDefault());
            var otherTransport = new ControllerAcceptance.Transport();
            new ServerGamePacketListenerImpl(level.getServer(), otherTransport, other, CommonListenerCookie.createInitial(other.getGameProfile(), false));
            other.setPos(4.5, 120, 96.5); level.addNewPlayer(other);
            var otherController = AllItems.LINKED_CONTROLLER.asStack();
            otherController.set(AllDataComponents.LINKED_CONTROLLER_ITEMS,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.REDSTONE))));
            other.setItemInHand(InteractionHand.MAIN_HAND, otherController);
            try {
                new LinkedControllerInputPacket(List.of(0), true).handle(other);
                check.accept("unrelated player and frequency register a second independent endpoint", size(sources, "endpoints") == 2);
                var heartbeat = ControllerWorkMetrics.measure(() ->
                    new LinkedControllerInputPacket(List.of(0), true).handle(actor));
                check.accept("actual heartbeat/updatePosition visits only its exact endpoint",
                    heartbeat.endpointReconciliations() == 1 && heartbeat.fullSweeps() == 0);
                var targeted = ControllerWorkMetrics.measure(() -> sources.reconcilePlayer(actor.getUUID()));
                check.accept("player event visits only its one associated endpoint, not unrelated player/frequency",
                    targeted.endpointReconciliations() == 1 && targeted.fullSweeps() == 0);
            } finally {
                new LinkedControllerInputPacket(List.of(0), false).handle(other);
                LinkedControllerServerHandler.tick(level);
                level.removePlayerImmediately(other, Entity.RemovalReason.DISCARDED);
                otherTransport.embedded.finishAndReleaseAll();
            }
            check.accept("unrelated endpoint cleanup leaves original endpoint ownership intact", size(sources, "endpoints") == 1);

            var a = new CraftKey(level.dimension(), UUID.randomUUID());
            var b = new CraftKey(level.dimension(), UUID.randomUUID());
            register(sources, endpoint, tagged, Scope.privateCraft(a));
            check.accept("private publication adds exact craft reverse association", selected(sources, "byCraft", a) == 1);
            register(sources, endpoint, tagged, Scope.privateCraft(b));
            check.accept("A to B replacement removes stale A reverse association",
                selected(sources, "byCraft", a) == 0 && selected(sources, "byCraft", b) == 1);
            sources.reconcileEndpoint(level, endpoint);

            var oldSource = tagged.fieldwork$source();
            sources.markEndpoint(endpoint);
            var snapshot = sources.beginNativeAdd(endpoint, level);
            sources.nativeAddFailed(snapshot);
            check.accept("failed duplicate add restores metadata ownership and pending dirty work",
                tagged.fieldwork$source() == oldSource && size(sources, "endpoints") == 1 &&
                    selected(sources, "byPlayer", actor.getUUID()) == 1 && collectionSize(sources, "dirty") == 1);
            sources.drain();

            sources.markEndpoint(endpoint); sources.markEndpoint(endpoint);
            check.accept("dirty endpoint selection is identity deduplicated", collectionSize(sources, "dirty") == 1);
            sources.drain();
            check.accept("successful dirty drain retains live ownership and consumes work",
                collectionSize(sources, "dirty") == 0 && size(sources, "endpoints") == 1);
            AcceptanceFaults.throwNativeRemove = true; boolean removeFailed = false;
            try { com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint); }
            catch (IllegalStateException expected) { removeFailed = expected.getMessage().equals("native remove fault"); }
            finally { AcceptanceFaults.throwNativeRemove = false; }
            check.accept("failed native remove retains registered endpoint and schedules exact retry",
                removeFailed && collectionSize(sources, "dirty") == 1 && size(sources, "endpoints") == 1);
            sources.drain();
            check.accept("ambiguous native removal retries exact membership without losing ownership",
                collectionSize(sources, "dirty") == 0 && size(sources, "endpoints") == 1);

            AcceptanceFaults.throwNativeUnload = true; boolean unloadFailed = false;
            try { com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.onUnloadWorld(level); }
            catch (IllegalStateException expected) { unloadFailed = expected.getMessage().equals("native unload fault"); }
            finally { AcceptanceFaults.throwNativeUnload = false; }
            check.accept("failed native level unload retains only that level ownership", unloadFailed && size(sources, "endpoints") == 1);
            sources.levelUnloaded(level);
            check.accept("successful native level unload callback clears the exact level index",
                size(sources, "endpoints") == 0 && size(sources, "byLevel") == 0);
            sources.nativeAdded(level, endpoint);
            check.accept("live endpoint can register cleanly after level-local index teardown", size(sources, "endpoints") == 1);

            BindingFaults.arm("initial-withdrawal"); boolean refreshFailed = false;
            try { sources.refreshManagedAdd(RefreshScope.frequency(level.dimension(), endpoint.getNetworkKey())); }
            catch (IllegalStateException expected) { refreshFailed = true; }
            check.accept("failed controller refresh is retained in retry ledger", refreshFailed && !retry(sources).isEmpty());
            sources.drain();
            check.accept("next dirty drain retries retained refresh even with no dirty endpoint", retry(sources).isEmpty());

            com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint);
            check.accept("exact native remove unregisters before Create collection cleanup", size(sources, "endpoints") == 0 && entries.contains(endpoint));
            check.accept("exact native remove clears reverse indexes", selected(sources, "byPlayer", actor.getUUID()) == 0 && size(sources, "byLevel") == 0);

            entries.remove(endpoint);
            @SuppressWarnings("rawtypes") Map rawInputs = LinkedControllerServerHandler.receivedInputs.get(level);
            AcceptanceFaults.throwNativeAdd = true; boolean addFailed = false;
            try { new LinkedControllerInputPacket(List.of(0), true).handle(actor); }
            catch (IllegalStateException expected) { addFailed = expected.getMessage().equals("native add fault"); }
            finally { AcceptanceFaults.throwNativeAdd = false; }
            check.accept("failed native add restores capture metadata without committing ownership",
                addFailed && size(sources, "endpoints") == 0);

            rawInputs.put(actor.getUUID(), new RejectingCollection(false));
            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("false collection add rolls back native and index ownership", size(sources, "endpoints") == 0);
            rawInputs.remove(actor.getUUID());
            rawInputs.put(actor.getUUID(), new RejectingCollection(true));
            boolean collectionThrew = false;
            try { new LinkedControllerInputPacket(List.of(0), true).handle(actor); }
            catch (IllegalStateException expected) { collectionThrew = expected.getMessage().equals("controller collection add fault"); }
            check.accept("throwing collection add preserves original failure and rolls back ownership",
                collectionThrew && size(sources, "endpoints") == 0);
            rawInputs.remove(actor.getUUID());

            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("fresh endpoint can register after exact removal", size(sources, "endpoints") == 1);

            java.util.Collection<?> sweepEntries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            var sweepEndpoint = (IRedstoneLinkable) sweepEntries.iterator().next();
            var sweepTagged = (ControllerEndpoint) sweepEndpoint;
            sweepTagged.fieldwork$published(Scope.PENDING); // test-only missed dirty notification
            field(sources, "postTicks").setInt(sources, 0);
            var beforeSweep = ControllerWorkMetrics.measure(() -> {
                for (int i = 0; i < 199; i++) sources.postTick();
            });
            check.accept("missed dirty notification is not hidden by an ordinary tick scan",
                sweepTagged.fieldwork$published() == Scope.PENDING && beforeSweep.fullSweeps() == 0 &&
                    beforeSweep.endpointReconciliations() == 0);
            var repairMissed = ControllerWorkMetrics.measure(sources::postTick);
            check.accept("tick-200 safety sweep repairs missed publication metadata",
                sweepTagged.fieldwork$published() == Scope.WORLD && repairMissed.fullSweeps() == 1 &&
                    repairMissed.endpointReconciliations() == 1);

            unregister(sources, sweepEndpoint);
            check.accept("native-only safety control starts absent from Fieldwork index but present in both native owners",
                size(sources, "endpoints") == 0 && sweepEntries.contains(sweepEndpoint) && radioContains(sweepEndpoint));
            field(sources, "postTicks").setInt(sources, 199);
            sources.postTick();
            check.accept("bidirectional safety sweep adopts native-only endpoint with retained provenance",
                size(sources, "endpoints") == 1 && selected(sources, "byPlayer", actor.getUUID()) == 1);

            sweepEntries.removeIf(candidate -> candidate == sweepEndpoint);
            com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level)
                .get(sweepEndpoint.getNetworkKey()).removeIf(candidate -> candidate == sweepEndpoint);
            check.accept("index-only safety control retains Fieldwork ownership after both native owners are removed",
                size(sources, "endpoints") == 1 && !radioContains(sweepEndpoint));
            field(sources, "postTicks").setInt(sources, 199);
            sources.postTick();
            check.accept("bidirectional safety sweep purges index-only leak and reverse associations",
                size(sources, "endpoints") == 0 && selected(sources, "byPlayer", actor.getUUID()) == 0);

            new LinkedControllerInputPacket(List.of(0), true).handle(actor);
            check.accept("fresh endpoint can register after safety drift repairs", size(sources, "endpoints") == 1);
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            check.accept("native TTL removal clears endpoint ownership without a safety sweep", size(sources, "endpoints") == 0);
            var afterTtl = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check.accept("native TTL removal and iterator cleanup agree", afterTtl == null || afterTtl.isEmpty());
        } finally {
            transport.connected = true;
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            transport.embedded.finishAndReleaseAll();
        }
    }

    private static int size(Object target, String name) throws Exception {
        return ((Map<?, ?>) field(target, name).get(target)).size();
    }

    private static int selected(Object target, String name, Object key) throws Exception {
        var value = ((Map<?, ?>) field(target, name).get(target)).get(key);
        return value instanceof java.util.Collection<?> collection ? collection.size() : 0;
    }

    private void register(Object sources, IRedstoneLinkable endpoint, ControllerEndpoint tagged, Scope scope) throws Exception {
        var method = sources.getClass().getDeclaredMethod("registerOrUpdate", ServerLevel.class,
            IRedstoneLinkable.class, ControllerEndpoint.class, Scope.class);
        method.setAccessible(true);
        method.invoke(sources, level, endpoint, tagged, scope);
    }

    private void unregister(Object sources, IRedstoneLinkable endpoint) throws Exception {
        var method = sources.getClass().getDeclaredMethod("unregister", ServerLevel.class, IRedstoneLinkable.class);
        method.setAccessible(true);
        method.invoke(sources, level, endpoint);
    }

    private boolean radioContains(IRedstoneLinkable endpoint) {
        var bucket = com.simibubi.create.Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(endpoint.getNetworkKey());
        return bucket != null && bucket.stream().anyMatch(candidate -> candidate == endpoint);
    }

    private static int collectionSize(Object target, String name) throws Exception {
        return ((java.util.Collection<?>) field(target, name).get(target)).size();
    }

    private static RefreshScope retry(Object target) throws Exception {
        return (RefreshScope) field(target, "reconcileRetry").get(target);
    }

    private static final class RejectingCollection extends ArrayList<Object> {
        private final boolean throwing;
        private RejectingCollection(boolean throwing) { this.throwing = throwing; }
        @Override public boolean add(Object entry) {
            if (throwing) throw new IllegalStateException("controller collection add fault");
            return false;
        }
    }

    private static Field field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
