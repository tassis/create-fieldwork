package dev.tassis.createfieldwork.acceptance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.isolator.state.IsolationRegistry;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Production persistence; initial grants use service fixtures, revoke uses real air
 * interaction while natively unloaded. Receiver routing is tested separately. */
final class BindingPersistenceAcceptance {
    private static final BlockPos LECTERN = new BlockPos(8, 120, 16);
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    private final String phase;
    private JsonObject metadata;
    BindingPersistenceAcceptance(ServerLevel level, BiConsumer<String, Boolean> check, String phase) {
        this.level = level; this.check = check; this.phase = phase;
    }
    boolean tick(int tick) throws Exception {
        var state = FieldworkStateAccess.get(level.getServer());
        var ledger = state.bindingRevocations();
        if (tick == 20) {
            if (phase.equals("binding-seed")) {
                metadata = new JsonObject(); metadata.addProperty("jvm", ManagementFactory.getRuntimeMXBean().getStartTime());
                var origin = new BlockPos(0, 120, 0); var positions = new ArrayList<BlockPos>();
                for (int x = 0; x <= 35; x++) { var p = origin.east(x).below(); level.setBlock(p, Blocks.STONE.defaultBlockState(), 3); positions.add(p); }
                level.setBlock(origin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); positions.add(origin);
                for (int x : new int[]{1, 34}) { var p = origin.east(x); level.setBlock(p, FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3); positions.add(p); }
                var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
                ((ServerSubLevelContainer) SubLevelContainer.getContainer(level)).addForceLoadTicket(craft, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
                var device = ledger.issue(); var revoked = ledger.issue();
                metadata.addProperty("craft", craft.getUniqueId().toString());
                metadata.addProperty("device", device.toString()); metadata.addProperty("revoked", revoked.toString());
                for (int x : new int[]{1, 34}) {
                    var be = (BrassIsolatorBlockEntity) level.getBlockEntity(craft.getPlot().getCenterBlock().east(x));
                    be.grant(IsolationRuntime.key(craft), device, ledger.generation(device));
                    be.grant(IsolationRuntime.key(craft), revoked, ledger.generation(revoked));
                }
                level.setChunkForced(0, 1, true);
                level.setBlock(LECTERN.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(LECTERN, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
                var item = controller(); ControllerDevice.write(item, device);
                var lectern = (LecternControllerBlockEntity) level.getBlockEntity(LECTERN);
                lectern.setController(item); lectern.setChanged();
                level.setBlock(LECTERN.east(4).below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(LECTERN.east(4), AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
                var pendingItem = ControllerReset.preview(item);
                var pendingLectern = (LecternControllerBlockEntity) level.getBlockEntity(LECTERN.east(4));
                pendingLectern.setController(pendingItem); pendingLectern.setChanged();
            } else {
                metadata = JsonParser.parseString(Files.readString(Path.of("binding-restart-main.json"))).getAsJsonObject();
                check.accept("binding persistence reload uses independent JVM", metadata.get("jvm").getAsLong() != ManagementFactory.getRuntimeMXBean().getStartTime());
            }
        }
        if (tick < 60) return false;
        var craft = SubLevelContainer.getContainer(level).getSubLevel(UUID.fromString(metadata.get("craft").getAsString()));
        if (craft == null && tick < 200) return false;
        check.accept("saved real binding craft UUID loaded with native ticket", craft != null);
        UUID device = UUID.fromString(metadata.get("device").getAsString()), revoked = UUID.fromString(metadata.get("revoked").getAsString());
        if (phase.equals("binding-seed")) {
            var container = SubLevelContainer.getContainer(level);
            var holding = container.getHoldingChunkMap();
            var craftId = craft.getUniqueId();
            level.getServer().saveEverything(true, true, true);
            var pointer = ((ServerSubLevel) craft).getLastSerializationPointer();
            check.accept("native persisted pointer exists before unloading revoke fixture", pointer != null);
            holding.moveToUnloaded((ServerSubLevel) craft, pointer.chunkPos());
            check.accept("persistent revoke gesture occurs with all old entries truly unloaded", container.getSubLevel(craftId) == null);
            var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "BindingRevocation"), ClientInformation.createDefault());
            var transport = new ControllerAcceptance.Transport();
            new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
            try {
                actor.setPos(8.5, 120, 20.5); actor.setXRot(-90); actor.setShiftKeyDown(true);
                var item = controller(); ControllerDevice.write(item, revoked); actor.setItemInHand(InteractionHand.MAIN_HAND, item);
                ControllerResetCrafting.craft(actor, InteractionHand.MAIN_HAND);
                check.accept("real self-crafting take revokes old ID before restoring ordinary stack", ledger.revoked(revoked) && ControllerDevice.read(actor.getMainHandItem()) == null);
            } finally { transport.embedded.finishAndReleaseAll(); }
            holding.snatchAndLoad(pointer, craftId);
            craft = container.getSubLevel(craftId);
            check.accept("stale native entries load after real revoke without recreating UUID", craft != null);
        }
        check.accept("production ledger retains active issued generation", ledger.permits(device, 1));
        check.accept("force-loaded craft reaches READY with all three actual markers", state.registry().snapshot(IsolationRuntime.key(craft)).status() ==
            IsolationRegistry.Status.READY && state.registry().snapshot(IsolationRuntime.key(craft)).positions().size() == 3);
        check.accept("force-loaded valid issued device reconstructs usable authority", IsolationRuntime.key(craft).equals(state.bindings().target(device, level)));
        check.accept("production ledger retains revoked generation across save", ledger.revoked(revoked) && !ledger.permits(revoked, 1));
        for (int x : new int[]{1, 34}) {
            var be = (BrassIsolatorBlockEntity) level.getBlockEntity(craft.getPlot().getCenterBlock().east(x));
            check.accept("production brass BE retains actual instance stamp", craft.getUniqueId().equals(be.instanceStamp()));
            check.accept("old persisted BE grant is compacted while global revocation remains", !be.grants().containsKey(revoked) && ledger.revoked(revoked));
            check.accept("both distant brass entries retain device grant", be.grants().get(device) == 1);
        }
        var lectern = (LecternControllerBlockEntity) level.getBlockEntity(LECTERN);
        var pendingLectern = (LecternControllerBlockEntity) level.getBlockEntity(LECTERN.east(4));
        check.accept("pending lectern denial marker survives complete save and independent JVM", pendingLectern != null && ControllerReset.pending(pendingLectern.getController()) &&
            device.equals(ControllerDevice.read(pendingLectern.getController())) && Objects.equals(controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS), pendingLectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
        check.accept("native lectern stores production typed device component", device.equals(ControllerDevice.read(lectern.getController())));
        check.accept("native lectern retains frequency component", Objects.equals(controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS), lectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
        var tag = lectern.saveWithFullMetadata(level.registryAccess());
        var fresh = BlockEntity.loadStatic(LECTERN, lectern.getBlockState(), tag, level.registryAccess());
        check.accept("fresh actual Create BE deserializes same production identity", fresh instanceof LecternControllerBlockEntity next && device.equals(ControllerDevice.read(next.getController())));
        var safe = new CompoundTag(); lectern.writeSafe(safe, level.registryAccess());
        check.accept("native safe write preserves production device identity", safe.hasUUID("FieldworkDevice") && device.equals(safe.getUUID("FieldworkDevice")));
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "BindingPersistence"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        try {
            actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            lectern.swapControllers(ItemStack.EMPTY, actor, InteractionHand.MAIN_HAND, lectern.getBlockState());
            var returned = actor.getMainHandItem();
            check.accept("native take-back retains production ID", device.equals(ControllerDevice.read(returned)));
            check.accept("empty lectern no longer carries taken device ID", ControllerDevice.read(lectern.getController()) == null);
            lectern.setController(returned.copy()); lectern.setChanged();
        } finally { transport.embedded.finishAndReleaseAll(); }
        if (phase.equals("binding-seed")) Files.writeString(Path.of("binding-restart-main.json"), metadata.toString());
        return true;
    }
    private ItemStack controller() {
        var stack = AllItems.LINKED_CONTROLLER.asStack();
        stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        return stack;
    }
}
