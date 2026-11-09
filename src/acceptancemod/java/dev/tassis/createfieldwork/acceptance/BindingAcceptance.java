package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.platform.FieldworkChunkEvents;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Real item interaction and controller packet routing to real Create and Simulated BEs. */
final class BindingAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    BindingAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    void run() throws Exception {
        var state = FieldworkStateAccess.get(level.getServer());
        var origin = new BlockPos(0, 120, 64);
        var a = craft(origin); var b = craft(origin.east(24));
        UUID bId = b.getUniqueId();
        var stored = a.getPlot().getCenterBlock();
        var smart = (BrassIsolatorBlockEntity) level.getBlockEntity(stored.east());
        var inside = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.east(2)); inside.tick();
        var other = (RedstoneLinkBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east(2)); other.tick();
        var simulated = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.east(6)); simulated.tick(); simulated.updateSignal();
        var world = link(origin.offset(8, 0, 8));
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MainBinding"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        var listener = new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        // Align with the native directional receiver's WEST axis; off-axis input
        // intentionally attenuates upstream and is not a scope-routing failure.
        actor.setPos(origin.getX() - 1.5, origin.getY(), origin.getZ() + .5); level.addNewPlayer(actor);
        var controller = controller(); actor.setItemInHand(InteractionHand.MAIN_HAND, controller);
        try {
            actor.setShiftKeyDown(true);
            actor.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            try { useBlock(actor, smart.getBlockPos()); }
            catch (UnsupportedOperationException expectedTransport) {
                if (!expectedTransport.getMessage().contains("neoforge:advanced_open_screen")) throw expectedTransport;
            }
            check.accept("native adventure use-first does not bypass mayBuild for grant mutation", ControllerDevice.read(controller) == null && smart.grants().isEmpty());
            actor.closeContainer();
            actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            useBlock(actor, smart.getBlockPos());
            var id = ControllerDevice.read(controller);
            check.accept("native crouch use-first binds actual server-held device", id != null);
            check.accept("smart BE owns device grant and real UUID stamp", smart.grants().containsKey(id) && a.getUniqueId().equals(smart.instanceStamp()));
            check.accept("server loaded authority resolves valid A", IsolationRuntime.key(a).equals(state.bindings().target(id, level)));
            actor.setShiftKeyDown(false);
            input(actor, true);
            signal("authorized WORLD operator controls private A", inside, 15);
            signal("authorized A device does not control same-frequency B", other, 0);
            signal("authorized A device never contributes WORLD", world, 0);
            check.accept("Simulated private A receives authorized controller15 actual=" + simulated.receivedSignal, simulated.receivedSignal == 15);
            input(actor, false);
            var offhandController = controller(); actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            actor.setItemInHand(InteractionHand.OFF_HAND, offhandController); actor.setShiftKeyDown(true);
            useBlock(actor, smart.getBlockPos(), InteractionHand.OFF_HAND); actor.setShiftKeyDown(false);
            var offhandId = ControllerDevice.read(offhandController);
            check.accept("actual offhand use-first binds its own device ID", offhandId != null && !id.equals(offhandId));
            input(actor, true); signal("native offhand packet controls authorized A", inside, 15); signal("offhand managed packet stays off WORLD", world, 0);
            input(actor, false); signal("offhand release withdraws A", inside, 0);
            input(actor, true); actor.setShiftKeyDown(true); actor.setXRot(-90);
            ControllerResetCrafting.craft(actor, InteractionHand.OFF_HAND);
            check.accept("native offhand self-craft reset clears only offhand identity", ControllerDevice.read(actor.getOffhandItem()) == null &&
                state.bindingRevocations().revoked(offhandId) && state.bindingRevocations().permits(id, 1));
            check.accept("self-crafting unbind preserves the offhand frequency program", actor.getOffhandItem().getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY).equals(controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            actor.setShiftKeyDown(false); input(actor, true); signal("offhand unbind also requires fresh press", world, 0);
            input(actor, false); input(actor, true); signal("ordinary offhand fresh press works after explicit unbind", world, 15);
            input(actor, false); LinkedControllerServerHandler.tick(level);
            actor.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY); actor.setItemInHand(InteractionHand.MAIN_HAND, controller); input(actor, true);
            signal("restoring bound main hand does not inherit ordinary offhand WORLD power", world, 0);
            signal("original main device remains authorized after offhand unbind", inside, 15);
            var bStored = b.getPlot().getCenterBlock();
            var board = b.logicalPose().transformPosition(new org.joml.Vector3d(bStored.getX() + 3.5, bStored.getY() + .25, bStored.getZ() + .5));
            listener.teleport(board.x, board.y, board.z, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -.5, 0)); listener.resetPosition();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(bStored.getX() + 3.5, bStored.getY() + .1, bStored.getZ() + .5, true));
            check.accept("binding fixture actually boards other private craft B", dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(actor) == b);
            input(actor, true); signal("accepted B operator can control authorized A", inside, 15); signal("B operator does not retarget managed device to B", other, 0);
            var airborne = actor.position();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(airborne.x, airborne.y + .3, airborne.z, false));
            check.accept("managed departure fixture uses actually accepted airborne WORLD coordinates",
                ControllerAcceptance.awaitingPosition(listener) == null && Math.abs(actor.getY() - airborne.y - .3) < .01);
            input(actor, true);
            signal("accepted airborne departure preserves fixed managed A control", inside, 15);
            signal("accepted airborne departure never broadcasts managed input WORLD", world, 0);
            signal("accepted airborne departure never retargets managed input to B", other, 0);
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(actor.getX() + 5000, actor.getY(), actor.getZ(), true));
            input(actor, true); signal("rejected movement does not retarget or quarantine managed A", inside, 15);
            level.setBlock(origin.offset(-2, -1, 0), Blocks.STONE.defaultBlockState(), 3);
            listener.teleport(origin.getX() - 1.5, origin.getY() + .1, origin.getZ() + .5, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -.5, 0)); listener.resetPosition();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            input(actor, false); input(actor, true); signal("accepted WORLD support recovers managed A input", inside, 15);
            var acceptedWorldPosition = actor.position();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(actor.getX() + 5000, actor.getY(), actor.getZ(), true));
            check.accept("rejected WORLD-to-WORLD move keeps the previous server position", actor.position().distanceToSqr(acceptedWorldPosition) < .01 &&
                ControllerAcceptance.awaitingPosition(listener) != null);
            input(actor, true); signal("managed input ignores teleport acknowledgement as routing authority", inside, 15);
            ControllerAcceptance.acknowledge(listener); input(actor, true);
            signal("acknowledged correction uses the old accepted position for managed range", inside, 15);
            signal("rejected WORLD move never converts managed input to public power", world, 0);
            var frequency = inside.getBehaviour(LinkBehaviour.TYPE).getNetworkKey();
            int range = com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange.get();
            RefreshMetrics.measure(() -> {
                actor.setPos(origin.getX() - range - 8.5, origin.getY(), origin.getZ() + .5);
                input(actor, true);
            });
            signal("managed range exit uses native player WORLD distance", inside, 0);
            check.accept("stable managed position skips same-frequency B cold updates", RefreshMetrics.count(level.dimension(), frequency, false, other.getBlockPos()) == 0);
            check.accept("stable managed position skips same-frequency WORLD cold updates", RefreshMetrics.count(level.dimension(), frequency, false, world.getBlockPos()) == 0);
            actor.setPos(origin.getX() - 1.5, origin.getY(), origin.getZ() + .5); input(actor, true);
            signal("managed range reentry restores A", inside, 15);
            input(actor, false); signal("release clears A", inside, 0);
            input(actor, true);
            for (int i = 0; i < 29; i++) LinkedControllerServerHandler.tick(level);
            signal("managed native timeout remains live immediately before exact expiry", inside, 15);
            LinkedControllerServerHandler.tick(level);
            signal("managed native timeout withdraws A on exact expiry tick", inside, 0);
            check.accept("managed timeout clears Simulated output", simulated.receivedSignal == 0);
            input(actor, true); transport.connected = false;
            state.controllers().playerRemoved(actor); // exact production logout invalidation seam
            state.controllers().postTick();
            signal("managed logout withdraws A", inside, 0); signal("managed logout never converts source to WORLD", world, 0);
            transport.connected = true; input(actor, false); input(actor, true);
            signal("valid connection fresh press can recover managed A", inside, 15);
            input(actor, false);

            // Same-frequency unrelated managed identity is not frequency-based authorization.
            var impostor = controller(); ControllerDevice.write(impostor, UUID.randomUUID());
            actor.setItemInHand(InteractionHand.MAIN_HAND, impostor);
            RefreshMetrics.measure(() -> input(actor, true));
            signal("same-frequency unknown managed ID cannot control A", inside, 0);
            signal("unknown managed ID cannot fall back WORLD", world, 0);
            check.accept("managed identity change uses full frequency fallback", RefreshMetrics.count(level.dimension(), frequency, false, other.getBlockPos()) > 0);
            input(actor, false);
            actor.setItemInHand(InteractionHand.MAIN_HAND, controller); input(actor, true);
            signal("original device fresh input restores A", inside, 15);
            var originalProgram = actor.getMainHandItem().getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
            actor.getMainHandItem().set(AllDataComponents.LINKED_CONTROLLER_ITEMS,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD))));
            actor.getInventory().setChanged();
            signal("managed program mutation withdraws A before inventory notification returns", inside, 0);
            signal("managed program mutation cannot fall back WORLD", world, 0);
            actor.getMainHandItem().set(AllDataComponents.LINKED_CONTROLLER_ITEMS, originalProgram);
            actor.getInventory().setChanged(); input(actor, false); input(actor, true);
            signal("restored managed program permits a fresh A press", inside, 15);
            int selectedSlot = actor.getInventory().selected;
            int alternateSlot = (selectedSlot + 1) % 9;
            actor.getInventory().setItem(alternateSlot, new ItemStack(Items.STICK));
            listener.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(alternateSlot));
            signal("selected-slot mutation withdraws A before packet handling returns", inside, 0);
            listener.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(selectedSlot));
            input(actor, false); input(actor, true);
            signal("returning to managed slot permits a fresh A press", inside, 15);
            actor.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
            listener.handlePlayerAction(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN));
            signal("main/offhand swap withdraws A before packet handling returns", inside, 0);
            listener.handlePlayerAction(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO, Direction.DOWN));
            input(actor, false); input(actor, true);
            signal("swapping managed controller back permits a fresh A press", inside, 15);
            var afterDrop = controller.copy();
            listener.handlePlayerAction(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM,
                BlockPos.ZERO, Direction.DOWN));
            signal("dropping the managed controller withdraws A before packet handling returns", inside, 0);
            actor.setItemInHand(InteractionHand.MAIN_HAND, afterDrop); controller = actor.getMainHandItem();
            input(actor, false); input(actor, true);
            signal("replacing a dropped managed controller permits a fresh A press", inside, 15);
            actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            signal("replacing managed controller withdraws A before inventory mutation returns", inside, 0);
            actor.setItemInHand(InteractionHand.MAIN_HAND, controller); input(actor, false); input(actor, true);
            signal("restoring real held device permits fresh A press", inside, 15);
            input(actor, false);
            var lecternPos = origin.offset(-3, 0, 0);
            level.setBlock(lecternPos.below(), Blocks.STONE.defaultBlockState(), 3);
            var programBeforeLectern = controller.get(AllDataComponents.LINKED_CONTROLLER_ITEMS);
            level.setBlock(lecternPos, Blocks.LECTERN.defaultBlockState(), 3);
            useBlock(actor, lecternPos);
            var lectern = (com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity) level.getBlockEntity(lecternPos);
            check.accept("native survival item use replaces vanilla lectern and consumes controller", lectern != null && actor.getMainHandItem().isEmpty());
            check.accept("native placed lectern retains actual frequency program", programBeforeLectern.equals(lectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            check.accept("native placed lectern retains managed device identity", id.equals(ControllerDevice.read(lectern.getController())));
            var clientTag = lectern.getUpdateTag(level.registryAccess());
            check.accept("lectern client update tag includes frequency data and device ID", clientTag.contains("ControllerData") && clientTag.hasUUID("FieldworkDevice") && id.equals(clientTag.getUUID("FieldworkDevice")));
            lectern.tryStartUsing(actor);
            check.accept("managed WORLD lectern accepts real native operator", lectern.isUsedBy(actor));
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            signal("managed lectern with retained ID controls remote A", inside, 15);
            signal("managed WORLD lectern does not broadcast WORLD", world, 0);
            var lecternSnapshot = lectern.getController();
            lectern.setController(null);
            check.accept("native null lectern assignment preserves both program and identity", id.equals(ControllerDevice.read(lectern.getController())) &&
                programBeforeLectern.equals(lectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            var failedAssignment = ControllerReset.preview(controller());
            ControllerDevice.write(failedAssignment, UUID.randomUUID());
            AcceptanceFaults.throwLecternAssignment = true; boolean assignmentFailed = false;
            try { lectern.setController(failedAssignment); }
            catch (IllegalStateException expected) { if (!expected.getMessage().equals("lectern assignment fault")) throw expected; assignmentFailed = true; }
            finally { AcceptanceFaults.throwLecternAssignment = false; }
            check.accept("failed native assignment rolls program identity and pending state back together", assignmentFailed &&
                id.equals(ControllerDevice.read(lectern.getController())) && programBeforeLectern.equals(lectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)) &&
                !ControllerReset.pending(lectern.getController()));
            signal("rolled-back lectern assignment preserves original actual output", inside, 15);
            var pendingLecternItem = ControllerReset.preview(lecternSnapshot);
            // Preserve channels deliberately: only the pending flag distinguishes
            // this denial control from the still-authorized original controller.
            pendingLecternItem.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, programBeforeLectern);
            BindingFaults.arm("initial-withdrawal");
            lectern.setController(pendingLecternItem);
            check.accept("post-assignment RuntimeException keeps native lectern assignment coherent", BindingFaults.reached &&
                ControllerReset.pending(lectern.getController()) &&
                id.equals(ControllerDevice.read(lectern.getController())));
            state.controllers().postTick();
            check.accept("post-assignment RuntimeException retries retained refresh scope", BindingFaults.point.isEmpty());
            signal("pending lectern identity cannot supply power even with unchanged channels", inside, 0);
            lectern.setController(lecternSnapshot); lectern.tryStartUsing(actor);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            signal("normal lectern recovers before Error assignment control", inside, 15);
            BindingFaults.arm("initial-withdrawal-error");
            lectern.setController(pendingLecternItem);
            check.accept("post-assignment Error keeps native lectern assignment coherent", BindingFaults.reached &&
                ControllerReset.pending(lectern.getController()) &&
                id.equals(ControllerDevice.read(lectern.getController())));
            state.controllers().postTick();
            signal("post-assignment Error retry withdraws actual output", inside, 0);
            var pendingTag = lectern.saveWithFullMetadata(level.registryAccess());
            var pendingFresh = (com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(lecternPos, lectern.getBlockState(), pendingTag, level.registryAccess());
            check.accept("pending lectern form survives native save and fresh deserialize", ControllerReset.pending(pendingFresh.getController()) &&
                id.equals(ControllerDevice.read(pendingFresh.getController())) && programBeforeLectern.equals(pendingFresh.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            var pendingSafe = new net.minecraft.nbt.CompoundTag(); lectern.writeSafe(pendingSafe, level.registryAccess());
            check.accept("pending lectern safe export retains denial marker", pendingSafe.getBoolean("FieldworkResetPending"));
            lectern.dropController(lectern.getBlockState());
            check.accept("native lectern drop preserves pending flag and old device on the item", level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(lecternPos).inflate(3)).stream().anyMatch(entity -> ControllerReset.pending(entity.getItem()) && id.equals(ControllerDevice.read(entity.getItem()))));
            check.accept("native drop clears empty lectern identity and pending marker", ControllerDevice.read(lectern.getController()) == null && !ControllerReset.pending(lectern.getController()));
            lectern.tick(); lectern.setController(lecternSnapshot); lectern.tryStartUsing(actor);
            new LinkedControllerInputPacket(List.of(0), false, lecternPos).handle(actor);
            new LinkedControllerInputPacket(List.of(0), true, lecternPos).handle(actor);
            signal("restored normal lectern form accepts a fresh native press", inside, 15);
            var lecternOperatorPosition = actor.position();
            actor.setPos(lecternOperatorPosition.add(32, 0, 0));
            lectern.tick();
            check.accept("native lectern tick automatically stops an out-of-range operator", !lectern.isUsedBy(actor));
            signal("automatic lectern range stop withdraws managed input in the same native tick", inside, 0);
            actor.setPos(lecternOperatorPosition);
            lectern.swapControllers(ItemStack.EMPTY, actor, InteractionHand.MAIN_HAND, lectern.getBlockState());
            check.accept("real lectern take-back retains bound identity", id.equals(ControllerDevice.read(actor.getMainHandItem())));
            controller = actor.getMainHandItem();
            input(actor, false); input(actor, true);
            signal("taken-back managed controller can fresh-press A", inside, 15);
            input(actor, false);
            var shipLecternPos = stored.east(3);
            level.setBlock(shipLecternPos, Blocks.LECTERN.defaultBlockState(), 3);
            var shipBoard = a.logicalPose().transformPosition(new org.joml.Vector3d(stored.getX() + 4.5, stored.getY() + .1, stored.getZ() + .5));
            listener.teleport(shipBoard.x, shipBoard.y, shipBoard.z, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -.5, 0)); listener.resetPosition();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(stored.getX() + 4.5, stored.getY() + .1, stored.getZ() + .5, true));
            useBlock(actor, shipLecternPos);
            var shipLectern = (com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity) level.getBlockEntity(shipLecternPos);
            check.accept("native physical-structure lectern placement retains frequency and device", shipLectern != null &&
                id.equals(ControllerDevice.read(shipLectern.getController())) && programBeforeLectern.equals(shipLectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            shipLectern.tryStartUsing(actor);
            check.accept("native physical lectern uses projected reach and active-user checks", shipLectern.isUsedBy(actor));
            new LinkedControllerInputPacket(List.of(0), true, shipLecternPos).handle(actor);
            signal("physical-structure lectern controls managed A", inside, 15); signal("physical lectern cannot broadcast WORLD", world, 0);
            var hovering = actor.position();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(hovering.x, hovering.y + .15, hovering.z, false));
            check.accept("physical lectern hover fixture retains native reach", com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity.playerInRange(actor, level, shipLecternPos));
            new LinkedControllerInputPacket(List.of(0), true, shipLecternPos).handle(actor);
            signal("accepted hovering operator still controls managed physical lectern", inside, 15);
            check.accept("physical lectern program is unchanged while its operator hovers", programBeforeLectern.equals(shipLectern.getController().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            shipLectern.tryStopUsing(actor);
            signal("physical lectern active-user stop withdraws output before stopUsing returns", inside, 0);
            shipLectern.swapControllers(ItemStack.EMPTY, actor, InteractionHand.MAIN_HAND, shipLectern.getBlockState());
            controller = actor.getMainHandItem();
            check.accept("physical lectern take-back preserves managed ID and program", id.equals(ControllerDevice.read(controller)) && programBeforeLectern.equals(controller.get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            listener.teleport(origin.getX() - 1.5, origin.getY() + .1, origin.getZ() + .5, 0, 0); ControllerAcceptance.acknowledge(listener);
            actor.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -.5, 0)); listener.resetPosition();
            listener.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(actor.getX(), actor.getY(), actor.getZ(), true));
            // Ending the lectern session is not a physical key release. Supply
            // the native release before the next deliberately fresh device bind.
            input(actor, false); LinkedControllerServerHandler.tick(level);
            var secondDevice = controller(); actor.setItemInHand(InteractionHand.MAIN_HAND, secondDevice);
            actor.setShiftKeyDown(true); useBlock(actor, smart.getBlockPos()); actor.setShiftKeyDown(false);
            var secondId = ControllerDevice.read(secondDevice);
            check.accept("same craft can authorize a second distinct controller", secondId != null && !id.equals(secondId) && smart.grants().containsKey(id) && smart.grants().containsKey(secondId));
            input(actor, true); signal("second authorized device controls A", inside, 15);
            actor.setItemInHand(InteractionHand.MAIN_HAND, controller);
            RefreshMetrics.measure(() -> input(actor, true));
            signal("same-frequency authorized device swap retains valid A output", inside, 15);
            check.accept("same-frequency device swap is full fallback not position shortcut", RefreshMetrics.count(level.dimension(), frequency, false, other.getBlockPos()) > 0);
            input(actor, false);
            var secondEntry = stored.east(3);
            level.setBlock(secondEntry, FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3);
            actor.setShiftKeyDown(true); useBlock(actor, secondEntry); actor.setShiftKeyDown(false);
            input(actor, true); signal("second loaded same-craft entry is valid", inside, 15);
            level.removeBlock(secondEntry, false);
            signal("removing one entry preserves another valid same-craft grant", inside, 15);
            check.accept("one remaining smart entry retains Simulated15", simulated.receivedSignal == 15);

            var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
            var holding = container.getHoldingChunkMap();
            UUID aId = a.getUniqueId();
            holding.moveToUnloaded(a, new net.minecraft.world.level.ChunkPos(origin));
            check.accept("real native holding unload removes all A entries from active craft", container.getSubLevel(aId) == null);
            // Sable unloads loading-dependency chains (including wireless-linked B).
            // Restore B through its own saved native record, never recreate UUIDs.
            if (holding.getHoldingSubLevel(bId) != null) holding.loadHoldingSubLevel(holding.getHoldingSubLevel(bId));
            b = (ServerSubLevel) container.getSubLevel(bId);
            check.accept("native B-only holding load leaves A actually unloaded", b != null && container.getSubLevel(aId) == null);
            other = (RedstoneLinkBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east(2)); other.tick();
            check.accept("unloaded A has no positive loaded authority", state.bindings().target(id, level) == null);
            signal("unloaded target does not publish managed source as WORLD", world, 0);
            actor.setPos(origin.getX() + 22.5, origin.getY(), origin.getZ() + .5);
            actor.setShiftKeyDown(true);
            long generationBefore = state.bindingRevocations().generation(id);
            useBlock(actor, b.getPlot().getCenterBlock().east());
            check.accept("binding B while all A entries unloaded is denied without generation change", id.equals(ControllerDevice.read(actor.getMainHandItem())) && generationBefore == state.bindingRevocations().generation(id));
            holding.loadHoldingSubLevel(holding.getHoldingSubLevel(aId));
            a = (ServerSubLevel) container.getSubLevel(aId);
            b = (ServerSubLevel) container.getSubLevel(bId);
            check.accept("B UUID remains loaded independently of A lifecycle", b != null);
            check.accept("B current plot ownership still matches B", dev.ryanhcode.sable.Sable.HELPER.getContaining(level, b.getPlot().getCenterBlock()) == b);
            other = (RedstoneLinkBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east(2)); other.tick();
            smart = (BrassIsolatorBlockEntity) level.getBlockEntity(stored.east());
            inside = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.east(2)); inside.tick();
            simulated = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.east(6)); simulated.tick();
            check.accept("restored directional fixture has valid native support", simulated.getBlockState().canSurvive(level, simulated.getBlockPos()));
            check.accept("native holding load reconstructs original UUID authority automatically", a != null && IsolationRuntime.key(a).equals(state.bindings().target(id, level)));
            actor.setShiftKeyDown(false); actor.setPos(origin.getX() - 1.5, origin.getY(), origin.getZ() + .5);
            input(actor, false); input(actor, true);
            signal("native grant reload preserves usable original A binding", inside, 15);

            // UUID-stamped record copied to a truly different craft cannot authorize it.
            var otherSmart = (BrassIsolatorBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east());
            check.accept("B smart BE survives independent A holding reload at " + b.getPlot().getCenterBlock().east() +
                " state=" + level.getBlockState(b.getPlot().getCenterBlock().east()) + " bRemoved=" + b.isRemoved(), otherSmart != null);
            var saved = smart.saveWithFullMetadata(level.registryAccess());
            otherSmart.loadWithComponents(saved.copy(), level.registryAccess());
            check.accept("copied authorization list on B does not change A target", IsolationRuntime.key(a).equals(state.bindings().target(id, level)));
            otherSmart.grant(IsolationRuntime.key(b), id, state.bindingRevocations().generation(id));
            check.accept("conflicting valid loaded records deny rather than last-loaded wins", state.bindings().target(id, level) == null);
            signal("conflicting ID withdraws A immediately", inside, 0); signal("conflicting ID never powers B", other, 0);
            otherSmart.loadWithComponents(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
            signal("removing conflicting record restores remaining valid A", inside, 15);
            var lookBlock = actor.blockPosition().offset(0, 1, 2);
            level.setBlock(lookBlock, Blocks.STONE.defaultBlockState(), 3); actor.setXRot(0); actor.setYRot(0); actor.setShiftKeyDown(true);
            check.accept("native frequency-menu fixture actually points at a non-air hit", actor.pick(5, 1, false).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK);
            boolean nativeMenuBranch;
            try {
                actor.getMainHandItem().getItem().use(level, actor, InteractionHand.MAIN_HAND);
                nativeMenuBranch = actor.containerMenu != actor.inventoryMenu;
            } catch (UnsupportedOperationException expectedTransport) {
                // This fake transport did not negotiate NeoForge custom UI payloads.
                // Exact native open-screen refusal proves the guard fell through;
                // do not claim actual client menu synchronization from this fixture.
                if (!expectedTransport.getMessage().contains("neoforge:advanced_open_screen")) throw expectedTransport;
                nativeMenuBranch = true;
            }
            check.accept("non-air crouch use retains native frequency-menu branch", nativeMenuBranch);
            check.accept("crouch use on other block does not unbind via native fallback", id.equals(ControllerDevice.read(actor.getMainHandItem())) && state.bindingRevocations().permits(id, 1));
            actor.closeContainer(); actor.setShiftKeyDown(false); level.removeBlock(lookBlock, false);
            input(actor, false); input(actor, true);

            // Removing final brass entry retains ordinary privacy but revokes availability.
            var smartPosition = smart.getBlockPos();
            RefreshMetrics.measure(() -> level.removeBlock(smartPosition, false));
            check.accept("ordinary marker still keeps A private", state.registry().scope(IsolationRuntime.key(a)).equals(Scope.privateCraft(IsolationRuntime.key(a))));
            signal("final brass removal withdraws A before returning", inside, 0);
            var liveSim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(stored.east(6));
            check.accept("final brass removal clears current Simulated contribution cached=" + simulated.receivedSignal +
                " current=" + liveSim.receivedSignal + " sameBE=" + (liveSim == simulated) +
                " cold=" + RefreshMetrics.count(level.dimension(), liveSim.getBehaviour(LinkBehaviour.TYPE).getNetworkKey(), true, liveSim.getBlockPos()), liveSim.receivedSignal == 0);
            signal("missing target does not reroute old press to WORLD", world, 0);
            check.accept("managed component survives passive loss", id.equals(ControllerDevice.read(actor.getMainHandItem())));

            // Restore the saved entry, then truly unload it before the real air revoke.
            level.setBlock(stored.east(), FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3);
            ((BrassIsolatorBlockEntity) level.getBlockEntity(stored.east())).loadWithComponents(saved.copy(), level.registryAccess());
            input(actor, false); input(actor, true); signal("restored persisted grant can authorize fresh input", inside, 15);
            holding.moveToUnloaded(a, new net.minecraft.world.level.ChunkPos(origin));
            check.accept("explicit-unbind fixture really has unloaded old entries", container.getSubLevel(aId) == null);
            if (holding.getHoldingSubLevel(bId) != null) holding.loadHoldingSubLevel(holding.getHoldingSubLevel(bId));
            b = (ServerSubLevel) container.getSubLevel(bId);
            check.accept("B is available while old A grants remain in native holding storage", b != null && container.getSubLevel(aId) == null);
            otherSmart = (BrassIsolatorBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east());
            other = (RedstoneLinkBlockEntity) level.getBlockEntity(b.getPlot().getCenterBlock().east(2)); other.tick();

            // Native crouch-air keeps Create's channel menu and bound identity.
            actor.setShiftKeyDown(true); actor.setXRot(-90); actor.setYRot(0);
            check.accept("native crouch-air frequency fixture actually observes air", actor.pick(5, 1, false).getType() == net.minecraft.world.phys.HitResult.Type.MISS);
            boolean airMenu = false;
            try { controller.getItem().use(level, actor, InteractionHand.MAIN_HAND); airMenu = actor.containerMenu != actor.inventoryMenu; }
            catch (UnsupportedOperationException expectedTransport) {
                if (!expectedTransport.getMessage().contains("neoforge:advanced_open_screen")) throw expectedTransport;
                airMenu = true;
            }
            check.accept("native crouch-air frequency menu preserves bound device", airMenu && id.equals(ControllerDevice.read(actor.getMainHandItem())) && state.bindingRevocations().permits(id, 1));
            actor.closeContainer();
            var staleOriginal = controller.copy();
            actor.getInventory().setItem(8, staleOriginal);
            controller = ControllerResetCrafting.craft(actor, InteractionHand.MAIN_HAND);
            check.accept("native self-crafting reset clears actual stack component", ControllerDevice.read(actor.getMainHandItem()) == null);
            check.accept("native reset persists old ID revocation", state.bindingRevocations().revoked(id));
            check.accept("self-craft does not silently clear an unreset old copy in inventory", id.equals(ControllerDevice.read(staleOriginal)) && !ControllerReset.pending(staleOriginal));
            check.accept("native unbind preserves frequency program without reconfiguration", controller.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY).equals(controller().get(AllDataComponents.LINKED_CONTROLLER_ITEMS)));
            actor.setShiftKeyDown(false);
            input(actor, true); signal("old held press cannot immediately become WORLD", world, 0);
            input(actor, false); input(actor, true);
            signal("fresh press after explicit unbind uses ordinary WORLD", world, 15);
            signal("ordinary controller cannot access private B before rebind", other, 0);
            input(actor, false);
            actor.setPos(origin.getX() + 22.5, origin.getY(), origin.getZ() + .5); actor.setShiftKeyDown(true);
            useBlock(actor, otherSmart.getBlockPos()); actor.setShiftKeyDown(false);
            var reboundId = ControllerDevice.read(actor.getMainHandItem());
            check.accept("explicitly unbound controller can bind B with fresh identity", reboundId != null && !id.equals(reboundId));
            input(actor, true); signal("fresh B binding controls B", other, 15);
            holding.loadHoldingSubLevel(holding.getHoldingSubLevel(aId));
            var reloadedA = (RedstoneLinkBlockEntity) level.getBlockEntity(stored.east(2)); reloadedA.tick();
            check.accept("stale A list cannot revive revoked old ID after B rebind", state.bindings().target(id, level) == null && state.bindingRevocations().revoked(id));
            signal("old A remains unpowered after stale list loads", reloadedA, 0);
            signal("loading stale A does not redirect B binding", other, 15);
            var reboundController = actor.getMainHandItem();
            actor.getInventory().setItem(8, ItemStack.EMPTY); actor.setItemInHand(InteractionHand.MAIN_HAND, staleOriginal);
            input(actor, false); input(actor, true);
            signal("unreset stale controller copy cannot revive private A", reloadedA, 0);
            signal("unreset stale controller copy cannot access private B", other, 0);
            signal("unreset stale controller copy cannot fall back WORLD", world, 0);
            check.accept("denied stale copy keeps its managed ID until explicitly unbound", id.equals(ControllerDevice.read(actor.getMainHandItem())));
            actor.setItemInHand(InteractionHand.MAIN_HAND, reboundController); input(actor, false); input(actor, true);
            signal("restoring current rebound controller still controls B", other, 15);
            var removedSession = state.playerSessions().bind(actor);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED);
            state.controllers().playerRemoved(actor);
            check.accept("logout removes the exact old player session", state.playerSessions().actor(actor.getUUID(), removedSession) == null);
            var replacement = new ServerPlayer(level.getServer(), level, new GameProfile(actor.getUUID(), "BindingReconnect"), ClientInformation.createDefault());
            var nextTransport = new ControllerAcceptance.Transport();
            var nextListener = new ServerGamePacketListenerImpl(level.getServer(), nextTransport, replacement, CommonListenerCookie.createInitial(replacement.getGameProfile(), false));
            replacement.setPos(origin.getX() + 22.5, origin.getY(), origin.getZ() + .5);
            replacement.setItemInHand(InteractionHand.MAIN_HAND, actor.getMainHandItem().copy()); level.addNewPlayer(replacement);
            try {
                state.playerSessions().bind(replacement); LinkedControllerServerHandler.tick(level);
                signal("same UUID new session cannot inherit old managed press", other, 0);
                input(replacement, true); signal("new session with actual device can fresh-press B", other, 15);
                replacement.setGameMode(net.minecraft.world.level.GameType.SPECTATOR); LinkedControllerServerHandler.tick(level);
                signal("spectator transition withdraws managed input", other, 0);
                replacement.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); input(replacement, true);
                signal("valid game mode fresh press recovers B", other, 15);
                var preCloneSession = state.playerSessions().bind(replacement);
                var nonDeathClone = new ServerPlayer(level.getServer(), level,
                    new GameProfile(replacement.getUUID(), "BindingClone"), ClientInformation.createDefault());
                nonDeathClone.setPos(replacement.position());
                nonDeathClone.setItemInHand(InteractionHand.MAIN_HAND, replacement.getMainHandItem().copy());
                var nonDeathEvent = new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(nonDeathClone, replacement, false);
                FieldworkChunkEvents.cloned(nonDeathEvent);
                check.accept("non-death clone event identifies original and replacement", nonDeathEvent.getOriginal() == replacement && nonDeathEvent.getEntity() == nonDeathClone);
                check.accept("non-death clone removes the original session before binding replacement",
                    state.playerSessions().actor(replacement.getUUID(), preCloneSession) == null &&
                    state.playerSessions().actor(nonDeathClone.getUUID(), state.playerSessions().bind(nonDeathClone)) == nonDeathClone);
                signal("non-death clone withdraws original managed output before handler returns", other, 0);
                state.controllers().playerRemoved(nonDeathClone); state.playerSessions().bind(replacement);
                input(replacement, false); input(replacement, true);
                signal("original fixture can establish a fresh session after non-death clone control", other, 15);
                var preDeathCloneSession = state.playerSessions().bind(replacement);
                var deathClone = new ServerPlayer(level.getServer(), level,
                    new GameProfile(replacement.getUUID(), "BindingDeathClone"), ClientInformation.createDefault());
                deathClone.setPos(replacement.position());
                deathClone.setItemInHand(InteractionHand.MAIN_HAND, replacement.getMainHandItem().copy());
                var deathEvent = new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(deathClone, replacement, true);
                FieldworkChunkEvents.cloned(deathEvent);
                check.accept("death clone event preserves its death classification", deathEvent.isWasDeath());
                check.accept("death clone removes the original session before binding replacement",
                    state.playerSessions().actor(replacement.getUUID(), preDeathCloneSession) == null &&
                    state.playerSessions().actor(deathClone.getUUID(), state.playerSessions().bind(deathClone)) == deathClone);
                signal("death clone withdraws original managed output before handler returns", other, 0);
                state.controllers().playerRemoved(deathClone); state.playerSessions().bind(replacement);
                input(replacement, false); input(replacement, true);
                signal("original fixture can establish a fresh session after death clone control", other, 15);
                var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
                var travelSession = state.playerSessions().bind(replacement);
                DimensionTravelFaults.mode = DimensionTravelFaults.Mode.CANCEL;
                var canceledTransfer = replacement.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,
                    new Vec3(0.5, 120, 0.5), Vec3.ZERO, 0, 0, false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                DimensionTravelFaults.mode = DimensionTravelFaults.Mode.NONE;
                check.accept("canceled dimension transfer returns the original player in the old level", canceledTransfer == replacement && replacement.serverLevel() == level);
                check.accept("canceled dimension transfer restores the exact player session", state.playerSessions().actor(replacement.getUUID(), travelSession) == replacement);
                signal("canceled dimension transfer restores managed output before returning", other, 15);
                boolean travelThrew = false;
                DimensionTravelFaults.mode = DimensionTravelFaults.Mode.THROW;
                try {
                    replacement.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,
                        new Vec3(0.5, 120, 0.5), Vec3.ZERO, 0, 0, false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().equals("acceptance dimension transfer failure")) throw expected;
                    travelThrew = true;
                } finally { DimensionTravelFaults.mode = DimensionTravelFaults.Mode.NONE; }
                check.accept("throwing dimension transfer exercises the failure seam", travelThrew);
                check.accept("throwing dimension transfer restores the exact player session", state.playerSessions().actor(replacement.getUUID(), travelSession) == replacement);
                signal("throwing dimension transfer restores managed output before propagating", other, 15);
                var transferredEntity = replacement.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether,
                    new Vec3(0.5, 120, 0.5), Vec3.ZERO, 0, 0, false, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                check.accept("managed session dimension fixture transfers a real player", transferredEntity instanceof ServerPlayer);
                var transferred = (ServerPlayer) transferredEntity;
                signal("dimension transfer withdraws old managed source before transfer returns", other, 0);
                var netherWorld = new BindingAcceptance(nether, check).link(new BlockPos(3, 120, 0));
                ControllerAcceptance.acknowledge(transferred.connection);
                input(transferred, true);
                signal("managed device cannot broadcast WORLD in another dimension", netherWorld, 0);
                level.getServer().getLevel(net.minecraft.world.level.Level.NETHER).removePlayerImmediately(transferred, Entity.RemovalReason.DISCARDED);
            } finally {
                if (!replacement.isRemoved()) level.removePlayerImmediately(replacement, Entity.RemovalReason.DISCARDED);
                nextTransport.embedded.finishAndReleaseAll();
            }
            check.accept("binding mutations leave no failed tokens", state.transitions().failedTokens().isEmpty());
        } finally {
            input(actor, false); LinkedControllerServerHandler.tick(level);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED); transport.embedded.finishAndReleaseAll();
        }
    }
    private net.minecraft.world.InteractionResult useBlock(ServerPlayer actor, BlockPos pos) { return useBlock(actor, pos, InteractionHand.MAIN_HAND); }
    private net.minecraft.world.InteractionResult useBlock(ServerPlayer actor, BlockPos pos, InteractionHand hand) {
        var item = actor.getItemInHand(hand);
        return item.getItem().onItemUseFirst(item, new UseOnContext(actor, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
    private void input(ServerPlayer actor, boolean press) { new LinkedControllerInputPacket(List.of(0), press).handle(actor); }
    private ServerSubLevel craft(BlockPos origin) {
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x <= 7; x++) { var p = origin.east(x).below(); level.setBlock(p, Blocks.STONE.defaultBlockState(), 3); blocks.add(p); }
        level.setBlock(origin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); blocks.add(origin);
        level.setBlock(origin.east(), FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3); blocks.add(origin.east());
        link(origin.east(2)); blocks.add(origin.east(2));
        // Keep native support independent of the secondary smart-entry mutation.
        for (int x : new int[]{5, 7}) { level.setBlock(origin.east(x), Blocks.STONE.defaultBlockState(), 3); blocks.add(origin.east(x)); }
        level.setBlock(origin.east(6), SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST), 3); blocks.add(origin.east(6));
        var sim = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(origin.east(6)); sim.tick(); frequency(sim.getBehaviour(LinkBehaviour.TYPE));
        check.accept("directional fixture has valid native support before assembly", sim.getBlockState().canSurvive(level, sim.getBlockPos()));
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, BoundingBox3i.from(blocks));
        ((dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer) dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level))
            .addForceLoadTicket(craft, dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType.COMMAND_FORCED, net.minecraft.util.Unit.INSTANCE);
        return craft;
    }
    private RedstoneLinkBlockEntity link(BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var be = (RedstoneLinkBlockEntity) level.getBlockEntity(pos); be.tick();
        level.setBlock(pos, be.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); be.tick();
        frequency(be.getBehaviour(LinkBehaviour.TYPE)); return be;
    }
    private void frequency(LinkBehaviour link) { link.setFrequency(true, new ItemStack(Items.AMETHYST_SHARD)); link.setFrequency(false, new ItemStack(Items.LAPIS_LAZULI)); }
    private ItemStack controller() {
        var stack = AllItems.LINKED_CONTROLLER.asStack();
        stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.LAPIS_LAZULI))));
        return stack;
    }
    private void signal(String name, RedstoneLinkBlockEntity receiver, int expected) {
        int actual = receiver.getReceivedSignal(); check.accept(name + " expected=" + expected + " actual=" + actual, actual == expected);
    }
}
