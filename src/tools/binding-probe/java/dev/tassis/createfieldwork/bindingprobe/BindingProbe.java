package dev.tassis.createfieldwork.bindingprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Executable feasibility experiment, explicitly NOT main-only behavior acceptance. */
@Mod("fieldwork_binding_probe")
public final class BindingProbe {
    private static final BlockPos LECTERN = new BlockPos(8, 120, 8);
    private final String phase = System.getProperty("fieldwork.binding.phase", "seed");
    private final List<Map<String, Object>> assertions = new ArrayList<>();
    private MinecraftServer server;
    private ServerLevel level;
    private FieldworkServerState mainState;
    private BindingIndex index;
    private JsonObject metadata;
    private int ticks;
    private boolean finished;

    public BindingProbe(IEventBus bus) {
        BindingContent.register(bus);
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::stopped);
    }
    private void started(ServerStartedEvent event) {
        server = event.getServer(); level = server.overworld();
        mainState = FieldworkStateAccess.get(server);
        index = new BindingIndex(server);
    }
    private void tick(ServerTickEvent.Post event) {
        if (finished || ++ticks < 20) return;
        try {
            if (ticks == 20) prepare();
            if (ticks < 60) return;
            var container = SubLevelContainer.getContainer(level);
            var a = (ServerSubLevel) container.getSubLevel(UUID.fromString(metadata.get("a").getAsString()));
            var b = (ServerSubLevel) container.getSubLevel(UUID.fromString(metadata.get("b").getAsString()));
            if ((a == null || b == null) && ticks < 200) return;
            check("both real saved Sable UUIDs loaded", a != null && b != null);
            run(a, b);
            if (phase.equals("seed")) Files.writeString(Path.of("seed.json"), metadata.toString());
            finish(null);
        } catch (Throwable error) { finish(error); }
    }
    private void prepare() throws Exception {
        check("main loaded but neither other test driver loaded", ModList.get().isLoaded("create_fieldwork") &&
            !ModList.get().isLoaded("fieldwork_acceptance") && !ModList.get().isLoaded("fieldwork_probe"));
        if (phase.equals("seed")) {
            metadata = new JsonObject();
            metadata.addProperty("jvm", ManagementFactory.getRuntimeMXBean().getStartTime());
            var a = craft(new BlockPos(0, 120, 0));
            var b = craft(new BlockPos(64, 120, 0));
            metadata.addProperty("a", a.getUniqueId().toString());
            metadata.addProperty("b", b.getUniqueId().toString());
            var auth = authorization(a);
            var first = controller(); var second = controller();
            metadata.addProperty("device", auth.authorize(first).toString());
            metadata.addProperty("secondDevice", auth.authorize(second).toString());
            force(LECTERN);
            level.setBlock(LECTERN.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(LECTERN, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
            var lectern = lectern();
            var patch = (LecternIdentityAccess) lectern;
            patch.bindingProbe$patchEnabled(false);
            try {
                lectern.setController(first.copy());
                check("negative control: native lectern loses arbitrary device ID", DeviceIdentity.read(lectern.getController()) == null);
                check("negative control still preserves frequency", sameFrequency(first, lectern.getController()));
            } finally { patch.bindingProbe$patchEnabled(true); }
            lectern.setController(first.copy()); lectern.setChanged();
        } else {
            var seedResult = JsonParser.parseString(Files.readString(Path.of("result-seed.json"))).getAsJsonObject();
            var seedAssertions = seedResult.getAsJsonArray("assertions");
            boolean validSeed = seedResult.get("kind").getAsString().equals("experimental-binding-prototype") &&
                seedResult.get("phase").getAsString().equals("seed") && seedResult.get("status").getAsString().equals("passed") &&
                seedAssertions.size() == 41;
            for (var assertion : seedAssertions) validSeed &= assertion.getAsJsonObject().get("passed").getAsBoolean();
            check("complete matching successful experimental seed report exists", validSeed);
            metadata = JsonParser.parseString(Files.readString(Path.of("seed.json"))).getAsJsonObject();
            check("reload is an independent JVM", metadata.get("jvm").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
        }
    }
    private ServerSubLevel craft(BlockPos origin) {
        // Multi-chunk coverage plus a genuine persisted Sable ticket below.
        // Vanilla chunk forcing is not the same as Sable's force-load tickets.
        for (int x = 0; x <= 35; x += 8) force(origin.offset(x, 0, 0));
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 35; x++) {
            var floor = origin.offset(x, -1, 0);
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3); blocks.add(floor);
        }
        level.setBlock(origin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); blocks.add(origin);
        level.setBlock(origin.east(), BindingContent.BLOCK.get().defaultBlockState(), 3); blocks.add(origin.east());
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        check("fixture installs native persisted Sable force-load ticket", ((ServerSubLevelContainer) SubLevelContainer.getContainer(level))
            .addForceLoadTicket(craft, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE));
        return craft;
    }
    private void force(BlockPos pos) { var chunk = new ChunkPos(pos); level.setChunkForced(chunk.x, chunk.z, true); }
    private AuthorizationBlockEntity authorization(ServerSubLevel craft) {
        return (AuthorizationBlockEntity) level.getBlockEntity(craft.getPlot().getCenterBlock().east());
    }
    private LecternControllerBlockEntity lectern() { return (LecternControllerBlockEntity) level.getBlockEntity(LECTERN); }
    private ItemStack controller() {
        var stack = AllItems.LINKED_CONTROLLER.asStack();
        stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(
            List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        return stack;
    }
    private boolean sameFrequency(ItemStack a, ItemStack b) {
        return Objects.equals(a.get(AllDataComponents.LINKED_CONTROLLER_ITEMS), b.get(AllDataComponents.LINKED_CONTROLLER_ITEMS));
    }
    private void run(ServerSubLevel a, ServerSubLevel b) throws Exception {
        UUID device = UUID.fromString(metadata.get("device").getAsString());
        UUID second = UUID.fromString(metadata.get("secondDevice").getAsString());
        var auth = authorization(a); var other = authorization(b);
        check("fixture A retains real multi-chunk plot coverage", a.getPlot().getLoadedChunks().size() >= 3);
        check("fixture B retains real multi-chunk plot coverage", b.getPlot().getLoadedChunks().size() >= 3);
        check("real ships have different instance UUIDs", !a.getUniqueId().equals(b.getUniqueId()));
        check("block stamp matches saved real instance UUID", a.getUniqueId().equals(auth.stamp()));
        check("block persists two different device IDs", !device.equals(second) && auth.devices().equals(Set.of(device, second)));
        var item = lectern().getController();
        check(phase + " lectern preserves original device ID", device.equals(DeviceIdentity.read(item)));
        check(phase + " lectern preserves frequency data", sameFrequency(item, controller()));
        check("controller stores device ID only, no ship UUID", item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
            .copyTag().getAllKeys().equals(Set.of(DeviceIdentity.KEY)));
        check("managed device blocked before reverse-index reconstruction", index.resolve(item).kind() == BindingIndex.Kind.BLOCKED);
        check("valid loaded block can rebuild reverse index", index.publish(auth));
        check("reconstructed device targets actual craft A", IsolationRuntime.key(a).equals(index.resolve(item).target()));
        check("repeated authorization preserves existing device ID", device.equals(auth.authorize(item)) && auth.devices().size() == 2);
        var sameFrequencyDifferentDevice = controller(); DeviceIdentity.write(sameFrequencyDifferentDevice, UUID.randomUUID());
        check("same-frequency unknown device is blocked", index.resolve(sameFrequencyDifferentDevice).kind() == BindingIndex.Kind.BLOCKED);
        check("ordinary no-ID controller remains explicitly ordinary", index.resolve(controller()).kind() == BindingIndex.Kind.ORDINARY);

        var saved = auth.saveWithFullMetadata(level.registryAccess());
        other.loadWithComponents(saved.copy(), level.registryAccess()); other.setChanged();
        check("copied record is physically on real craft B", IsolationRuntime.key(b).equals(other.currentCraft()));
        check("copied UUID stamp cannot authorize a different instance", !index.publish(other));
        index.remove(auth);
        check("copied record alone cannot rescue missing A grant or fall back WORLD", index.resolve(item).kind() == BindingIndex.Kind.BLOCKED);
        index.publish(auth);
        other.loadWithComponents(new CompoundTag(), level.registryAccess());
        other.authorize(item.copy()); index.publish(other);
        check("candidate policy: conflicting valid targets deny, never last-loaded wins", index.resolve(item).kind() == BindingIndex.Kind.BLOCKED);
        index.remove(other); other.loadWithComponents(new CompoundTag(), level.registryAccess()); other.setChanged();
        check("removing conflict restores remaining valid target A", IsolationRuntime.key(a).equals(index.resolve(item).target()));

        var position = auth.getBlockPos();
        level.removeBlock(position, false); index.remove(auth);
        check("ordinary isolator keeps craft private after authorization block removal",
            ScopeResolver.craft(level, IsolationRuntime.key(a), null).equals(Scope.privateCraft(IsolationRuntime.key(a))));
        check("candidate index invalidation blocks managed device even while craft stays private",
            index.resolve(item).kind() == BindingIndex.Kind.BLOCKED);
        level.setBlock(position, BindingContent.BLOCK.get().defaultBlockState(), 3);
        auth = (AuthorizationBlockEntity) level.getBlockEntity(position);
        auth.loadWithComponents(saved.copy(), level.registryAccess()); auth.setChanged(); index.publish(auth);
        check("restored block record rebuilds original target", IsolationRuntime.key(a).equals(index.resolve(item).target()));

        var lectern = lectern();
        var nativeSaved = lectern.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(LECTERN);
        var loaded = BlockEntity.loadStatic(LECTERN, level.getBlockState(LECTERN), nativeSaved, level.registryAccess());
        check("native BE deserialization recreates a fresh Create lectern", loaded instanceof LecternControllerBlockEntity && loaded != lectern);
        level.setBlockEntity(loaded);
        lectern = lectern();
        check("fresh native BE load retains ID and frequency", device.equals(DeviceIdentity.read(lectern.getController())) && sameFrequency(item, lectern.getController()));
        var safe = new CompoundTag(); lectern.writeSafe(safe, level.registryAccess());
        check("native safe serialization carries device ID", safe.hasUUID(DeviceIdentity.KEY) && device.equals(safe.getUUID(DeviceIdentity.KEY)));
        inputAndTakeBack(lectern, device, item);
    }
    private void inputAndTakeBack(LecternControllerBlockEntity lectern, UUID device, ItemStack expected) {
        var actor = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "BindingProbe"), ClientInformation.createDefault());
        var transport = new Transport();
        new ServerGamePacketListenerImpl(server, transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        actor.setPos(LECTERN.getX() + .5, LECTERN.getY(), LECTERN.getZ() + 1.1);
        level.addNewPlayer(actor);
        try {
            actor.setItemInHand(InteractionHand.MAIN_HAND, expected.copy());
            var handheld = new LinkedControllerInputPacket(List.of(0), true); handheld.handle(actor);
            check("native handheld input sees same device ID", device.equals(((InputIdentityAccess) handheld).bindingProbe$observedDevice()));
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            // Expire native handheld entries before attributing a backend update
            // to the lectern. A nonempty shared player map alone is not evidence.
            for (int i = 0; i < 31; i++) LinkedControllerServerHandler.tick(level);
            var beforeLectern = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check("handheld backend entries expire before lectern input", beforeLectern == null || beforeLectern.isEmpty());
            var rejected = new LinkedControllerInputPacket(List.of(0), true, LECTERN); rejected.handle(actor);
            check("lectern still rejects input without active operator", !((InputIdentityAccess) rejected).bindingProbe$handledItem());
            lectern.tryStartUsing(actor);
            check("native lectern validates active operator", lectern.isUsedBy(actor));
            var pressed = new LinkedControllerInputPacket(List.of(0), true, LECTERN); pressed.handle(actor);
            check("native lectern input selects reconstructed controller with same ID", device.equals(((InputIdentityAccess) pressed).bindingProbe$observedDevice()));
            java.util.Collection<?> entries = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
            check("lectern input creates fresh native pressed15 backend entry", entries != null && entries.stream().anyMatch(entry ->
                ((com.simibubi.create.content.redstone.link.IRedstoneLinkable) entry).getTransmittedStrength() == 15));
            new LinkedControllerInputPacket(List.of(0), false, LECTERN).handle(actor);
            lectern.tryStopUsing(actor);
            actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            lectern.swapControllers(ItemStack.EMPTY, actor, InteractionHand.MAIN_HAND, lectern.getBlockState());
            var returned = actor.getMainHandItem();
            check("native swap/take-back preserves original ID and frequency", device.equals(DeviceIdentity.read(returned)) && sameFrequency(expected, returned));
            check("taking back does not leave an old device ID in empty lectern", DeviceIdentity.read(lectern.getController()) == null);
            lectern.setController(returned.copy());
            lectern.dropController(lectern.getBlockState());
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(LECTERN).inflate(2));
            check("native drop fallback carries same ID and frequency", drops.stream().anyMatch(drop -> device.equals(DeviceIdentity.read(drop.getItem())) && sameFrequency(expected, drop.getItem())));
            check("native drop clears ID from emptied lectern", DeviceIdentity.read(lectern.getController()) == null);
            drops.forEach(Entity::discard);
            lectern.setController(returned.copy()); lectern.setChanged();
        } finally {
            lectern.tryStopUsing(actor);
            new LinkedControllerInputPacket(List.of(0), false).handle(actor);
            LinkedControllerServerHandler.tick(level);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            transport.embedded.finishAndReleaseAll();
        }
    }
    private void check(String name, boolean passed) {
        assertions.add(Map.of("name", name, "passed", passed));
        if (!passed) throw new AssertionError(name);
    }
    private void finish(Throwable failure) {
        finished = true;
        var result = new LinkedHashMap<String, Object>();
        result.put("kind", "experimental-binding-prototype"); result.put("phase", phase);
        result.put("status", failure == null ? "passed" : "failed");
        result.put("assertions", assertions);
        if (failure != null) {
            result.put("failure", failure.toString());
            LogUtils.getLogger().error("EXPERIMENTAL BINDING PROTOTYPE FAILED", failure);
        }
        try {
            var json = new GsonBuilder().setPrettyPrinting().create().toJson(result);
            Files.writeString(Path.of("result.json"), json); Files.writeString(Path.of("result-" + phase + ".json"), json);
        } catch (Exception error) { throw new IllegalStateException(error); }
        finally { index.close(); server.halt(false); }
    }
    private void stopped(ServerStoppedEvent event) {
        try { mainState.registry(); }
        catch (IllegalStateException expected) {
            if (!expected.getMessage().equals("Fieldwork server session is closed")) throw expected;
            try { Files.writeString(Path.of("state-closed.txt"), "main state closed after normal save; experimental index closed before halt\n"); }
            catch (Exception error) { throw new IllegalStateException(error); }
            return;
        }
        throw new AssertionError("Main state did not close");
    }
    private static final class Transport extends Connection {
        boolean connected = true;
        final EmbeddedChannel embedded;
        Transport() { super(PacketFlow.SERVERBOUND); embedded = new EmbeddedChannel(this); }
        @Override public boolean isConnected() { return connected; }
        @Override public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 0); }
        @Override public String getLoggableAddress(boolean logIps) { return "binding-probe"; }
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, PacketSendListener listener) { if (listener != null) listener.onSuccess(); }
        @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { if (listener != null) listener.onSuccess(); }
        @Override public void disconnect(Component message) { connected = false; }
        @Override public void disconnect(DisconnectionDetails details) { connected = false; }
    }
}
