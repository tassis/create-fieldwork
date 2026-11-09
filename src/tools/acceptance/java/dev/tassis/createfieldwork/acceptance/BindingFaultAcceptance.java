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
import dev.tassis.createfieldwork.isolator.binding.ControllerDevice;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.controller.reset.ControllerReset;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Binding-specific mutation failure boundaries and real named repair, main answers only. */
final class BindingFaultAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;
    BindingFaultAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }
    void run() {
        int i = 0;
        for (String point : List.of("issue", "grant", "pre-frame-publication", "initial-withdrawal", "withdrawal", "revocation", "final-refresh", "final-refresh-error", "publication")) scenario(point, i++);
    }
    private void scenario(String point, int index) {
        var state = FieldworkStateAccess.get(level.getServer());
        var origin = new BlockPos(index * 96, 120, 128);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 2; x++) { var p = origin.east(x).below(); level.setBlock(p, Blocks.STONE.defaultBlockState(), 3); positions.add(p); }
        level.setBlock(origin, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); positions.add(origin);
        level.setBlock(origin.east(), FieldworkContent.BRASS_ISOLATOR.get().defaultBlockState(), 3); positions.add(origin.east());
        link(origin.east(2)); positions.add(origin.east(2));
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        var inside = (RedstoneLinkBlockEntity) level.getBlockEntity(craft.getPlot().getCenterBlock().east(2)); inside.tick();
        var world = link(origin.offset(6, 0, 8));
        var actor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "BindingFault" + index), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        new ServerGamePacketListenerImpl(level.getServer(), transport, actor, CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        actor.setPos(origin.getX() - 1.5, origin.getY(), origin.getZ() + .5); level.addNewPlayer(actor);
        var item = AllItems.LINKED_CONTROLLER.asStack();
        item.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND))));
        actor.setItemInHand(InteractionHand.MAIN_HAND, item);
        boolean bindFault = point.equals("issue") || point.equals("grant");
        try {
            if (bindFault) {
                input(actor, true);
                check.accept(point + " baseline ordinary source is public15", world.getReceivedSignal() == 15 && inside.getReceivedSignal() == 0);
            } else {
                actor.setShiftKeyDown(true); bind(actor, craft.getPlot().getCenterBlock().east()); actor.setShiftKeyDown(false);
                input(actor, false); input(actor, true);
                check.accept(point + " baseline managed source controls A15 only", inside.getReceivedSignal() == 15 && world.getReceivedSignal() == 0);
            }
            actor.setShiftKeyDown(true); actor.setXRot(-90);
            boolean deferUntilResetTake = point.equals("pre-frame-publication") || point.equals("initial-withdrawal");
            if (!deferUntilResetTake) BindingFaults.arm(point);
            AcceptanceFaults.throwPublication = point.equals("publication"); AcceptanceFaults.publicationReached = false;
            boolean threw = false;
            try {
                if (bindFault) bind(actor, craft.getPlot().getCenterBlock().east());
                else item = ControllerResetCrafting.craft(actor, InteractionHand.MAIN_HAND,
                    deferUntilResetTake ? () -> BindingFaults.arm(point) : () -> {});
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().equals("binding acceptance fault: " + point) &&
                    !(point.equals("publication") && expected.getMessage().equals("acceptance final publication failure"))) throw expected;
                threw = true;
            } finally { BindingFaults.point = ""; AcceptanceFaults.throwPublication = false; }
            if (point.equals("pre-frame-publication")) {
                check.accept("pre-frame-publication leaves crafted result pending", ControllerReset.pending(item));
                check.accept("pre-frame-publication injection was reached", BindingFaults.reached);
            }
            check.accept(point + " exact binding fault reached", (bindFault ? threw : ControllerReset.pending(item)) &&
                (point.equals("publication") ? AcceptanceFaults.publicationReached : BindingFaults.reached));
            if (point.equals("pre-frame-publication")) check.accept("no-view publication fault occurs before a transaction frame exists", BindingFaults.preFrameObserved);
            check.accept(point + " keeps one named failure token", state.transitions().failedTokens().size() == 1);
            check.accept(point + " retains affected private craft barrier", state.transitions().blocked(IsolationRuntime.key(craft), null));
            check.accept(point + " withdrawal leaves actual private output0", inside.getReceivedSignal() == 0);
            check.accept(point + " never turns old press into ordinary WORLD", world.getReceivedSignal() == 0);
            if (point.equals("revocation")) check.accept("revoke fault retains managed item while veto already denies", ControllerDevice.read(item) != null && state.bindingRevocations().revoked(ControllerDevice.read(item)));
            if (point.equals("final-refresh") || point.equals("publication")) {
                check.accept(point + " failed reset retains pending managed identity until full commit", ControllerReset.pending(item) && ControllerDevice.read(item) != null);
                actor.setShiftKeyDown(false); input(actor, true);
                check.accept(point + " held key is still latched after failed final publication", world.getReceivedSignal() == 0);
            }
            long token = state.transitions().failedTokens().iterator().next();
            if (!bindFault) {
                item = ControllerResetCrafting.craft(actor, InteractionHand.MAIN_HAND);
                check.accept(point + " repeated craft retry stays pending without another token", ControllerReset.pending(item) && state.transitions().failedTokens().size() == 1);
                var beforeMove = actor.position(); actor.setPos(beforeMove.add(16, 0, 16));
                actor.setShiftKeyDown(false);
                item.getItem().use(level, actor, InteractionHand.MAIN_HAND);
                check.accept(point + " moved native-use retry cannot escape named repair", ControllerReset.pending(item) && state.transitions().failedTokens().size() == 1);
                actor.setPos(beforeMove);
            }
            check.accept(point + " unrelated repair token cannot release quarantine", !state.transitions().repair(token + 1000) && state.transitions().failedTokens().contains(token));
            check.accept(point + " named repair validates/saves and republishes", state.transitions().repair(token));
            check.accept(point + " repair releases only validated token", state.transitions().failedTokens().isEmpty());
            if (!bindFault) {
                actor.setShiftKeyDown(false); item.getItem().use(level, actor, InteractionHand.MAIN_HAND);
                item = actor.getMainHandItem();
                check.accept(point + " repaired pending result finishes through native use", !ControllerReset.pending(item) && ControllerDevice.read(item) == null);
                check.accept(point + " repaired unbind preserves its original frequency program", item.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY).equals(
                    ItemContainerContents.fromItems(List.of(new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)))));
            }
            if (ControllerDevice.read(item) == null) {
                actor.setShiftKeyDown(false); input(actor, false); LinkedControllerServerHandler.tick(level); input(actor, true);
                check.accept(point + " only fresh ordinary press works after repair", world.getReceivedSignal() == 15 && inside.getReceivedSignal() == 0);
            } else {
                check.accept(point + " unchanged/revoked managed identity never becomes public", world.getReceivedSignal() == 0);
                if (state.bindingRevocations().revoked(ControllerDevice.read(item))) check.accept(point + " repaired revocation does not revive grant", inside.getReceivedSignal() == 0);
            }
        } finally {
            BindingFaults.point = ""; AcceptanceFaults.throwPublication = false;
            input(actor, false); LinkedControllerServerHandler.tick(level);
            level.removePlayerImmediately(actor, Entity.RemovalReason.DISCARDED); transport.embedded.finishAndReleaseAll();
        }
    }
    private void bind(ServerPlayer actor, BlockPos pos) {
        var item = actor.getMainHandItem();
        item.getItem().onItemUseFirst(item, new UseOnContext(actor, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
    private void input(ServerPlayer actor, boolean press) { new LinkedControllerInputPacket(List.of(0), press).handle(actor); }
    private RedstoneLinkBlockEntity link(BlockPos pos) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var be = (RedstoneLinkBlockEntity) level.getBlockEntity(pos); be.tick();
        level.setBlock(pos, be.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3); be.tick();
        var frequency = be.getBehaviour(LinkBehaviour.TYPE); frequency.setFrequency(true, new ItemStack(Items.EMERALD)); frequency.setFrequency(false, new ItemStack(Items.DIAMOND));
        return be;
    }
}
