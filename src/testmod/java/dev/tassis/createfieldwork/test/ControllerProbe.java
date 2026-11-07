package dev.tassis.createfieldwork.test;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.player.ServerboundMovePlayerPacketExtension;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Real packet.handle/server handler and entries, controlled FakePlayer tracking. */
final class ControllerProbe {
    private final ServerLevel level;
    private final ScopeProbeAssertions checks;
    ControllerProbe(ServerLevel level, ScopeProbeAssertions checks) { this.level = level; this.checks = checks; }

    void run() {
        ServerSubLevel a = craft(new BlockPos(0, 120, 0));
        ServerSubLevel b = craft(new BlockPos(0, 120, 16));
        RedstoneLinkBlockEntity insideA = checks.initialized(a.getPlot().getCenterBlock().offset(3, 0, 0));
        RedstoneLinkBlockEntity insideB = checks.initialized(b.getPlot().getCenterBlock().offset(3, 0, 0));
        RedstoneLinkBlockEntity world = checks.link(new BlockPos(0, 120, 8), true);
        ServerPlayer actor = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ScopeController"));
        ItemStack item = controller();
        actor.setItemInHand(InteractionHand.MAIN_HAND, item);
        actor.setPos(0.5, 120.25, 0.5);
        actor.move(MoverType.SELF, new Vec3(0, -0.5, 0));
        ProbeController.naturalTrackingObserved = Sable.HELPER.getTrackingSubLevel(actor) == a;
        // Keep semantic tests deterministic even if this bare FakePlayer move
        // does not exercise the full packet/collision tracking lifecycle.
        actor.setPos(0.5, 121, 0.5);
        var movement = (EntityMovementExtension) actor;
        BlockPos plot = a.getPlot().getCenterBlock();
        var aboardPacket = new ServerboundMovePlayerPacket.Pos(plot.getX() + 0.5, plot.getY() + 1, plot.getZ() + 0.5, true);
        ((ServerboundMovePlayerPacketExtension) aboardPacket).sable$handle(actor);
        checks.check("actual Sable movement packet producer establishes tracking", movement.sable$getTrackingSubLevel() == a);
        checks.check("Sable movement packet converts plot to world coordinates", Math.abs(aboardPacket.getX(0)) < 256 && Math.abs(aboardPacket.getZ(0)) < 256);
        var departurePacket = new ServerboundMovePlayerPacket.Pos(8.5, 120, 0.5, true);
        ((ServerboundMovePlayerPacketExtension) departurePacket).sable$handle(actor);
        checks.check("actual Sable world movement packet clears current tracking", movement.sable$getTrackingSubLevel() == null);
        movement.sable$setTrackingSubLevel(a);
        ProbeController.refresh = checks::refresh;

        // Negative control: the unaugmented positional resolver sees WORLD.
        ProbeController.enabled = false;
        press(actor, true);
        checks.refresh();
        checks.signal("negative control handheld on ship is incorrectly WORLD", insideA, 0);
        checks.signal("negative control raw handheld leaks to world", world, 15);
        press(actor, false);
        LinkedControllerServerHandler.tick(level);

        ProbeController.enabled = true;
        try {
            press(actor, true);
            checks.refresh();
            checks.signal("handheld aboard A controls same private craft", insideA, 15);
            checks.signal("handheld aboard A cannot control world", world, 0);
            checks.signal("handheld aboard A cannot control private B", insideB, 0);
            var entry = entry(actor);
            checks.check("real ManualFrequencyEntry retains upstream world BlockPos", entry.getLocation().equals(actor.blockPosition()));
            checks.check("entry carries server packet provenance", entry instanceof ControllerEndpoint tagged && tagged.probe$source() != null);

            // KNOWN PROTOTYPE GAP: null tracking alone is not confirmed departure.
            // These observations document the current fallback, NOT proof that
            // WORLD is safe during jumping/collision/packet transition windows.
            // Production needs a last-confirmed-private/PENDING state and an
            // accepted movement transition before releasing into WORLD.
            movement.sable$setTrackingSubLevel(null);
            checks.check("current null while last tracking UUID remains A", movement.sable$getTrackingSubLevel() == null && movement.sable$getLastTrackingSubLevelID().equals(a.getUniqueId()));
            LinkedControllerServerHandler.tick(level);
            checks.signal("leave ship while held clears private A without release", insideA, 0);
            checks.signal("leave ship while held becomes WORLD source", world, 15);

            actor.setPos(0.5, 121, 16.5);
            movement.sable$setTrackingSubLevel(b);
            LinkedControllerServerHandler.tick(level);
            checks.signal("switch ship while held controls B", insideB, 15);
            checks.signal("switch ship while held leaves A clear", insideA, 0);
            checks.signal("switch ship while held clears world", world, 0);

            press(actor, false);
            checks.check("release makes upstream manual strength zero immediately", entry.getTransmittedStrength() == 0);
            LinkedControllerServerHandler.tick(level);
            checks.signal("release tick clears B", insideB, 0);
            checks.check("release removes manual input collection", LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID()) == null);

            actor.setPos(0.5, 121, 0.5);
            movement.sable$setTrackingSubLevel(a);
            press(actor, true);
            checks.refresh();
            checks.signal("second press on A works", insideA, 15);
            for (int i = 0; i < 31; i++) LinkedControllerServerHandler.tick(level);
            checks.signal("upstream 30-tick timeout clears A", insideA, 0);

            ProbeScope.ready(a, false);
            press(actor, true);
            checks.refresh();
            checks.signal("marker-free craft controller retains upstream internal", insideA, 15);
            checks.signal("marker-free craft controller retains upstream world", world, 15);
            ProbeScope.ready(a, true);
            checks.refresh();
            checks.signal("scope becomes private while held clears world", world, 0);
            press(actor, false);
            LinkedControllerServerHandler.tick(level);

            actor.setPos(4096, 121, 0);
            movement.sable$setTrackingSubLevel(a);
            press(actor, true);
            checks.refresh();
            checks.signal("owner metadata does not bypass actual world range", insideA, 0);
            press(actor, false);
            LinkedControllerServerHandler.tick(level);

            actor.setPos(0.5, 121, 0.5);
            press(actor, true);
            checks.refresh();
            actor.discard();
            LinkedControllerServerHandler.tick(level);
            checks.signal("removed actor cannot retain private power", insideA, 0);
            checks.signal("removed private actor does not fall back to WORLD", world, 0);
            cleanup(actor, item);

            lectern(a, insideA, insideB, world, item);
            worldHistory(a, insideA, world, item);
            checks.check("controller tick reconciles ownership changes", ProbeController.reconciliations >= 3);
        } finally {
            cleanup(actor, item);
            ProbeController.enabled = false;
            ProbeController.refresh = null;
            for (var craft : List.copyOf(SubLevelContainer.getContainer(level).getAllSubLevels())) {
                SubLevelContainer.getContainer(level).removeSubLevel(craft, SubLevelRemovalReason.REMOVED);
            }
        }
    }

    private void worldHistory(ServerSubLevel a, RedstoneLinkBlockEntity insideA, RedstoneLinkBlockEntity world, ItemStack item) {
        var actor = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "WorldHistory"));
        actor.setPos(0.5, 121, 0.5);
        actor.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        ((EntityMovementExtension) actor).sable$setTrackingSubLevel(a);
        press(actor, true);
        checks.refresh();
        checks.signal("history control starts private", world, 0);
        ProbeScope.ready(a, false);
        checks.refresh();
        checks.signal("removing isolation clears private history semantics", world, 15);
        actor.discard();
        LinkedControllerServerHandler.tick(level);
        checks.signal("unrestricted removed actor retains upstream timeout behavior", world, 15);
        for (int i = 0; i < 31; i++) LinkedControllerServerHandler.tick(level);
        checks.signal("unrestricted upstream timeout still expires", world, 0);
        checks.signal("unrestricted timeout clears internal too", insideA, 0);
        ProbeScope.ready(a, true);
    }

    private void lectern(ServerSubLevel a, RedstoneLinkBlockEntity insideA, RedstoneLinkBlockEntity insideB,
                          RedstoneLinkBlockEntity world, ItemStack item) {
        BlockPos pos = a.getPlot().getCenterBlock().offset(2, 0, 0);
        level.setBlock(pos, AllBlocks.LECTERN_CONTROLLER.getDefaultState(), 3);
        var lectern = (LecternControllerBlockEntity) level.getBlockEntity(pos);
        lectern.setController(item.copy());
        ServerPlayer operator = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ScopeLectern"));
        operator.setPos(2.5, 120, 0.5);
        ((EntityMovementExtension) operator).sable$setTrackingSubLevel(null);
        lectern.tryStartUsing(operator);
        checks.check("actual lectern validates active server user", lectern.isUsedBy(operator));
        new LinkedControllerInputPacket(List.of(0), true, pos).handle(operator);
        checks.refresh();
        checks.signal("lectern source follows its craft not WORLD operator", insideA, 15);
        checks.signal("private lectern rejects world", world, 0);
        checks.signal("private lectern rejects other craft", insideB, 0);
        checks.check("lectern preserves upstream player location for range", entry(operator).getLocation().equals(operator.blockPosition()));
        new LinkedControllerInputPacket(List.of(0), false, pos).handle(operator);
        LinkedControllerServerHandler.tick(level);
        checks.signal("lectern release clears same craft", insideA, 0);
        lectern.tryStopUsing(operator);
        ServerPlayer other = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "NotLecternUser"));
        new LinkedControllerInputPacket(List.of(0), true, pos).handle(other);
        checks.check("non-user lectern packet cannot create input", LinkedControllerServerHandler.receivedInputs.get(level).get(other.getUUID()) == null);
        operator.discard(); other.discard();
    }

    private ServerSubLevel craft(BlockPos origin) {
        checks.link(origin.offset(3, 0, 0), true);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            level.setBlock(origin.offset(x, -1, 0), Blocks.STONE.defaultBlockState(), 3);
            positions.add(origin.offset(x, -1, 0));
        }
        level.setBlock(origin.offset(1, 0, 0), Blocks.OBSIDIAN.defaultBlockState(), 3);
        positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(3, 0, 0));
        return SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
    }

    static ItemStack controller() {
        ItemStack result = AllItems.LINKED_CONTROLLER.asStack();
        result.set(AllDataComponents.LINKED_CONTROLLER_ITEMS,
            ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD))));
        return result;
    }
    private static void press(ServerPlayer actor, boolean pressed) { new LinkedControllerInputPacket(List.of(0), pressed).handle(actor); }
    private IRedstoneLinkable entry(ServerPlayer actor) {
        Collection<?> collection = LinkedControllerServerHandler.receivedInputs.get(level).get(actor.getUUID());
        if (collection == null || collection.isEmpty()) throw new AssertionError("No real manual input for " + actor.getUUID());
        return (IRedstoneLinkable) collection.iterator().next();
    }
    private void cleanup(ServerPlayer actor, ItemStack item) {
        LinkedControllerServerHandler.receivePressed(level, actor.blockPosition(), actor.getUUID(),
            List.of(com.simibubi.create.content.redstone.link.controller.LinkedControllerItem.toFrequency(item, 0)), false);
        LinkedControllerServerHandler.tick(level);
    }
}
