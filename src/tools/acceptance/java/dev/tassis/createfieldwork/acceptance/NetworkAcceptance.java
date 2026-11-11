package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlock;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.isolator.ordinary.IsolatorBlockEntity;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import net.createmod.catnip.data.Couple;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;

/** Actual main marker, resolver, lifecycle and radio hooks. No research behavior is loaded. */
final class NetworkAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;

    NetworkAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) { this.level = level; this.check = check; }

    void run() {
        var state = FieldworkStateAccess.get(level.getServer());
        check.accept("actual shaped isolator recipe loaded", level.getRecipeManager()
            .byKey(ResourceLocation.fromNamespaceAndPath("create_fieldwork", "redstone_link_isolator")).isPresent());
        content();
        var worldSource = link(new BlockPos(0, 120, 0), false);
        var outside = link(new BlockPos(8, 120, 0), true);
        worldSource.transmit(7);
        state.refresh().refresh(null);
        signal("public network keeps upstream strength", outside, 7);
        BlockPos origin = new BlockPos(32, 120, 0);
        var source = link(origin, false);
        link(origin.offset(3, 0, 0), true);
        level.setBlock(origin.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 3; x++) {
            BlockPos floor = origin.offset(x, -1, 0);
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            positions.add(floor);
        }
        positions.add(origin); positions.add(origin.offset(1, 0, 0)); positions.add(origin.offset(3, 0, 0));
        source.transmit(15);
        var craft = SubLevelAssemblyHelper.assembleBlocks(level, origin, positions, BoundingBox3i.from(positions));
        check.accept("actual main lifecycle assembled craft", craft != null);
        BlockPos stored = craft.getPlot().getCenterBlock();
        var insideSource = initialized(stored);
        var inside = initialized(stored.offset(3, 0, 0));
        insideSource.transmit(15);
        state.refresh().refresh(null);
        check.accept("main reconstruction finds actual isolator", state.registry().snapshot(IsolationRuntime.key(craft)).positions().contains(stored.offset(1, 0, 0)));
        check.accept("actual marker ACTIVE derives from registry", level.getBlockState(stored.offset(1, 0, 0)).getValue(IsolatorBlock.ACTIVE));
        signal("same craft keeps signal", inside, 15);
        signal("private15 does not replace public7", outside, 7);
        insideSource.transmit(0); worldSource.transmit(15);
        state.refresh().refresh(null);
        signal("world15 cannot feed private receiver", inside, 0);
        insideSource.transmit(15); worldSource.transmit(7);
        // Both sources are west of this directional receiver; do not mistake
        // upstream directional rejection for a scope-refresh failure.
        var simPos = new BlockPos(40, 120, 0);
        level.setBlock(simPos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(simPos, SimBlocks.DIRECTIONAL_LINKED_RECEIVER.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST), 3);
        var simulated = (AbstractLinkedReceiverBlockEntity) level.getBlockEntity(simPos);
        simulated.tick();
        frequency(simulated.getBehaviour(LinkBehaviour.TYPE));
        simulated.updateSignal();
        check.accept("main Simulated consumer rejects private15 and keeps world7", simulated.receivedSignal == 7);

        level.removeBlock(stored.offset(1, 0, 0), false);
        check.accept("last actual marker removal publishes WORLD", state.registry().scope(IsolationRuntime.key(craft)) == Scope.WORLD);
        signal("last-marker return already refreshed outside", outside, 15);
        check.accept("last-marker return refreshed Simulated expected=15 actual=" + simulated.receivedSignal, simulated.receivedSignal == 15);

        level.setBlock(stored.offset(1, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        signal("first-marker return already withdrew private15", outside, 7);
        check.accept("first-marker return withdrew Simulated", simulated.receivedSignal == 7);
        level.setBlock(stored.offset(2, 0, 0), FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        level.removeBlock(stored.offset(1, 0, 0), false);
        signal("two-to-one stays private", outside, 7);
        check.accept("registry contains only remaining actual marker", state.registry().snapshot(IsolationRuntime.key(craft)).positions().size() == 1);
        check.accept("normal network transitions leave no failed tokens", state.transitions().failedTokens().isEmpty());
        train(worldSource, insideSource, stored.offset(2, 0, 0));
        trainEdgeCases(worldSource);
        callbacksAndFailure(craft, worldSource, insideSource, stored.offset(2, 0, 0));
        refreshRange(worldSource, insideSource, outside, stored.offset(2, 0, 0));
        lateRegistration(worldSource);
        afterPublicationFault(craft, insideSource, outside, stored.offset(2, 0, 0));
    }

    private RedstoneLinkBlockEntity link(BlockPos pos, boolean receiver) {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.RECEIVER, false)
            .setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var entity = initialized(pos);
        if (receiver) {
            level.setBlock(pos, entity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, true), 3);
            entity.tick();
        }
        frequency(entity.getBehaviour(LinkBehaviour.TYPE));
        return entity;
    }

    private RedstoneLinkBlockEntity initialized(BlockPos pos) {
        var entity = (RedstoneLinkBlockEntity) level.getBlockEntity(pos);
        if (entity == null) throw new AssertionError("Missing actual link at " + pos);
        entity.tick();
        return entity;
    }

    private void frequency(LinkBehaviour behaviour) {
        behaviour.setFrequency(true, new ItemStack(Items.DIAMOND));
        behaviour.setFrequency(false, new ItemStack(Items.EMERALD));
    }

    private void signal(String name, RedstoneLinkBlockEntity receiver, int expected) {
        check.accept(name + " expected=" + expected + " actual=" + receiver.getReceivedSignal(), receiver.getReceivedSignal() == expected);
    }

    private RedstoneLinkCondition condition(boolean low) {
        var condition = new RedstoneLinkCondition();
        condition.setItem(0, new ItemStack(Items.DIAMOND));
        condition.setItem(1, new ItemStack(Items.EMERALD));
        condition.getData().putInt("Inverted", low ? 1 : 0);
        return condition;
    }

    private void train(RedstoneLinkBlockEntity world, RedstoneLinkBlockEntity privateSource, BlockPos marker) {
        var state = FieldworkStateAccess.get(level.getServer());
        world.transmit(0); privateSource.transmit(15);
        // Train is intentionally not consulted by the approved public-observer contract.
        check.accept("fixed WORLD train HIGH ignores private-only", !condition(false).tickCompletion(level, null, new CompoundTag()));
        check.accept("fixed WORLD train LOW accepts private-only absence", condition(true).tickCompletion(level, null, new CompoundTag()));
        var high = condition(false);
        var context = new CompoundTag();
        check.accept("train public absence is cached", !high.tickCompletion(level, null, context) && context.contains("LastChecked"));
        level.removeBlock(marker, false);
        check.accept("train source scope-only change invalidates absence", high.tickCompletion(level, null, context));
        check.accept("unchanged decisive train cache preserves upstream false", !high.tickCompletion(level, null, context));
        context.putUUID("FieldworkSession", UUID.randomUUID());
        check.accept("train session mismatch rechecks", high.tickCompletion(level, null, context));
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        state.transitions().begin("acceptance-source-pending", level, java.util.List.of(privateSource.getBlockPos()));
        try {
            var pendingContext = new CompoundTag();
            check.accept("train pending-only HIGH waits", !condition(false).tickCompletion(level, null, pendingContext));
            check.accept("train PENDING does not stamp checked cache", !pendingContext.contains("LastChecked") && !pendingContext.contains("FieldworkVisibility"));
            check.accept("train pending-only LOW also waits", !condition(true).tickCompletion(level, null, new CompoundTag()));
            world.transmit(7);
            check.accept("known public positive overrides pending for HIGH", condition(false).tickCompletion(level, null, new CompoundTag()));
            check.accept("known public positive prevents LOW", !condition(true).tickCompletion(level, null, new CompoundTag()));
        } finally { state.transitions().end("acceptance-source-pending", true); }
        check.accept("unrelated global caller remains unfiltered", Create.REDSTONE_LINK_NETWORK_HANDLER.hasAnyLoadedPower(privateSource.getBehaviour(LinkBehaviour.TYPE).getNetworkKey()));
    }

    private void callbacksAndFailure(ServerSubLevel craft, RedstoneLinkBlockEntity world, RedstoneLinkBlockEntity source, BlockPos marker) {
        var state = FieldworkStateAccess.get(level.getServer());
        var frequency = source.getBehaviour(LinkBehaviour.TYPE).getNetworkKey();
        var child = new Listener(new BlockPos(8, 120, 0), frequency);
        var callback = new Listener(new BlockPos(8, 120, 1), frequency);
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, child);
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, callback);
        boolean[] observed = {false};
        int[] childValue = {-1};
        callback.callback = signal -> {
            if (signal == 15 && !observed[0]) {
                observed[0] = true;
                check.accept("ordinary callback query cannot borrow final candidate view", ScopeResolver.at(level, source.getBlockPos(), null) == Scope.PENDING);
                // Create does not set a custom non-LinkBehaviour anchor's own
                // received field. Recompute via the real registered WORLD source
                // and observe child as a distinct receiver in that nested call.
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, world.getBehaviour(LinkBehaviour.TYPE));
                childValue[0] = child.signal;
            }
        };
        level.removeBlock(marker, false);
        check.accept("final refresh callback was exercised", observed[0]);
        check.accept("reentrant network call receives normal barrier view, not candidate15; actual=" + childValue[0], childValue[0] == 7);
        level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        callback.callback = signal -> {};
        callback.throwOnce = true;
        boolean prevented = false;
        try { level.removeBlock(marker, false); }
        catch (IllegalStateException expected) { prevented = expected.getMessage().equals("acceptance receiver failure"); }
        check.accept("BEGIN withdrawal failure prevents marker write", prevented && level.getBlockState(marker).is(FieldworkContent.ISOLATOR.get()));
        check.accept("failed refresh retains a named transaction", state.transitions().failedTokens().size() == 1);
        long failed = state.transitions().failedTokens().iterator().next();
        check.accept("failed scope remains PENDING", ScopeResolver.at(level, source.getBlockPos(), null) == Scope.PENDING);

        BlockPos independent = new BlockPos(160, 120, 0);
        level.setBlock(independent.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(independent, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
        var blocks = java.util.List.of(independent, independent.below());
        var other = SubLevelAssemblyHelper.assembleBlocks(level, independent, blocks, BoundingBox3i.from(blocks));
        check.accept("failed A does not prevent independent B commit", state.registry().scope(IsolationRuntime.key(other)).equals(Scope.privateCraft(IsolationRuntime.key(other))));
        check.accept("B commit does not release A's barrier", state.transitions().failedTokens().contains(failed));
        var unrelatedLow = condition(true);
        unrelatedLow.setItem(0, new ItemStack(Items.GOLD_INGOT));
        check.accept("known unrelated frequency is not globally frozen by A failure", unrelatedLow.tickCompletion(level, null, new CompoundTag()));
        check.accept("named repair verifies blocks and publishes only failed A", state.transitions().repair(failed));
        check.accept("successful named repair releases its token", !state.transitions().failedTokens().contains(failed));
        check.accept("named repair preserves actual private marker", state.registry().scope(IsolationRuntime.key(craft)).equals(Scope.privateCraft(IsolationRuntime.key(craft))));
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, callback);
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, child);
    }

    private static final class Listener implements IRedstoneLinkable {
        final BlockPos position;
        final Couple<Frequency> frequency;
        int signal;
        boolean throwOnce;
        IntConsumer callback = value -> {};
        Listener(BlockPos position, Couple<Frequency> frequency) { this.position = position; this.frequency = frequency; }
        @Override public int getTransmittedStrength() { return 0; }
        @Override public boolean isListening() { return true; }
        @Override public void setReceivedStrength(int value) {
            if (throwOnce) { throwOnce = false; throw new IllegalStateException("acceptance receiver failure"); }
            signal = value; callback.accept(value);
        }
        @Override public boolean isAlive() { return true; }
        @Override public BlockPos getLocation() { return position; }
        @Override public Couple<Frequency> getNetworkKey() { return frequency; }
    }

    private void refreshRange(RedstoneLinkBlockEntity world, RedstoneLinkBlockEntity source,
                              RedstoneLinkBlockEntity outside, BlockPos marker) {
        var range = com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange;
        int previous = range.get();
        var middle = link(new BlockPos(20, 120, 0), true);
        try {
            range.set(16);
            level.removeBlock(marker, false);
            signal("cold refresh keeps only in-range WORLD7 at receiver8", outside, 7);
            signal("cold refresh gives public craft15 only to in-range receiver20", middle, 15);
            level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3);
            signal("private transition withdraws in-range15 without granting distant7", middle, 0);
            signal("private transition preserves still-legal WORLD7", outside, 7);
        } finally {
            range.set(previous);
            FieldworkStateAccess.get(level.getServer()).refresh().refresh(null);
        }
    }

    private void lateRegistration(RedstoneLinkBlockEntity world) {
        var state = FieldworkStateAccess.get(level.getServer());
        world.transmit(0);
        BlockPos pos = new BlockPos(224, 120, 0);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        check.accept("new not-yet-registered WORLD transmitter is indexed pending", state.publications().pendingWorld());
        check.accept("train LOW does not complete in missing-registration window", !condition(true).tickCompletion(level, null, new CompoundTag()));
        var source = initialized(pos);
        frequency(source.getBehaviour(LinkBehaviour.TYPE));
        source.transmit(7);
        check.accept("registered WORLD positive completes HIGH", condition(false).tickCompletion(level, null, new CompoundTag()));
        level.removeBlock(pos, false);
        check.accept("removing late source clears publication pending", !state.publications().pendingWorld());
        world.transmit(7);
    }

    private void content() {
        var tab = dev.tassis.createfieldwork.platform.registry.OptionalFieldworkContent.FIELDWORK_TAB.get();
        check.accept("Fieldwork has a registered dedicated creative tab", ResourceLocation.fromNamespaceAndPath("create_fieldwork", "main")
            .equals(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)));
        check.accept("Fieldwork creative tab uses the isolator icon", tab.getIconItem().is(FieldworkContent.ISOLATOR_ITEM.get()));
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
        check.accept("Fieldwork tab is visible and contains both isolators without operator permissions", tab.shouldDisplay()
            && tab.getDisplayItems().size() == 2 && tab.getDisplayItems().stream().anyMatch(stack -> stack.is(FieldworkContent.ISOLATOR_ITEM.get()))
            && tab.getDisplayItems().stream().anyMatch(stack -> stack.is(FieldworkContent.BRASS_ISOLATOR_ITEM.get())));
        check.accept("Fieldwork tab also exposes the isolator to creative search", tab.getSearchTabDisplayItems().stream()
            .anyMatch(stack -> stack.is(FieldworkContent.ISOLATOR_ITEM.get())));
        var recipeId = ResourceLocation.fromNamespaceAndPath("create_fieldwork", "redstone_link_isolator");
        var holder = level.getRecipeManager().byKey(recipeId).orElseThrow();
        check.accept("isolator recipe is ordinary shaped crafting", holder.value() instanceof ShapedRecipe);
        var recipe = (ShapedRecipe) holder.value();
        var ingredients = new ArrayList<>(java.util.List.of(
            new ItemStack(Items.OBSIDIAN), AllItems.ANDESITE_ALLOY.asStack(), new ItemStack(Items.OBSIDIAN),
            AllItems.ELECTRON_TUBE.asStack(), AllBlocks.REDSTONE_LINK.asStack(), AllItems.ELECTRON_TUBE.asStack(),
            AllItems.ANDESITE_ALLOY.asStack(), AllItems.PRECISION_MECHANISM.asStack(), AllItems.ANDESITE_ALLOY.asStack()));
        var input = CraftingInput.of(3, 3, ingredients);
        check.accept("approved OBO/ELE/BPB ingredients match actual recipe", recipe.matches(input, level));
        var output = recipe.assemble(input, level.registryAccess());
        check.accept("approved recipe outputs exactly one actual isolator item", output.is(FieldworkContent.ISOLATOR_ITEM.get()) && output.getCount() == 1);
        ingredients.set(4, ItemStack.EMPTY);
        check.accept("recipe cannot omit the Redstone Link", !recipe.matches(CraftingInput.of(3, 3, ingredients), level));
        var pos = new BlockPos(16, 120, 16);
        level.setBlock(pos, FieldworkContent.ISOLATOR.get().defaultBlockState().setValue(IsolatorBlock.ACTIVE, true), 3);
        var block = level.getBlockState(pos);
        check.accept("WORLD placement normalizes misleading saved ACTIVE", !block.getValue(IsolatorBlock.ACTIVE));
        check.accept("actual isolator has registered block entity", level.getBlockEntity(pos) instanceof IsolatorBlockEntity);
        boolean noSignal = true;
        for (var direction : Direction.values()) noSignal &= block.getSignal(level, pos, direction) == 0;
        check.accept("isolator emits no ordinary redstone", noSignal && !block.isSignalSource());
        var drops = net.minecraft.world.level.block.Block.getDrops(block, level, pos, level.getBlockEntity(pos), null, new ItemStack(Items.DIAMOND_PICKAXE));
        check.accept("actual loot table drops one isolator", drops.size() == 1 && drops.getFirst().is(FieldworkContent.ISOLATOR_ITEM.get()) && drops.getFirst().getCount() == 1);
        check.accept("diamond pickaxe satisfies registered tool tags", new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(block));
        check.accept("wooden pickaxe does not satisfy registered tier", !new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(block));
        check.accept("recipe unlock advancement loaded", level.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("create_fieldwork", "recipes/redstone_link_isolator")) != null);
        level.removeBlock(pos, false);
    }

    private void trainEdgeCases(RedstoneLinkBlockEntity world) {
        var state = FieldworkStateAccess.get(level.getServer());
        var key = Couple.create(Frequency.of(new ItemStack(Items.REDSTONE)), Frequency.of(new ItemStack(Items.GLOWSTONE_DUST)));
        var source = new CountingSource(new BlockPos(240, 120, 0), key);
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, source);
        var high = condition(false); var low = condition(true);
        for (var condition : java.util.List.of(high, low)) {
            condition.setItem(0, new ItemStack(Items.REDSTONE)); condition.setItem(1, new ItemStack(Items.GLOWSTONE_DUST));
        }
        var context = new CompoundTag();
        state.transitions().begin("acceptance-expanded-root", level, java.util.List.of(new BlockPos(900, 120, 0)));
        try {
            check.accept("known public positive remains decisive in unrelated root", high.tickCompletion(level, null, context));
            int reads = source.reads;
            state.transitions().destinations(level, java.util.List.of(source.position));
            check.accept("same-root expansion invalidates decisive cache and re-reads source", !high.tickCompletion(level, null, context) && source.reads > reads);
        } finally { state.transitions().end("acceptance-expanded-root", true); }
        var lowContext = new CompoundTag();
        check.accept("sole public source prevents LOW before unregister", !low.tickCompletion(level, null, lowContext));
        int version = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, source);
        check.accept("upstream last-member removal leaves power version unchanged", Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get() == version);
        check.accept("local visibility epoch handles last-member removal cache", low.tickCompletion(level, null, lowContext));

        var range = com.simibubi.create.infrastructure.config.AllConfigs.server().logistics.linkRange;
        int previous = range.get();
        try {
            range.set(2);
            check.accept("WORLD train query introduces no range restriction", condition(false).tickCompletion(level, null, new CompoundTag()));
        } finally { range.set(previous); }
        world.transmit(0);
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        BlockPos pos = new BlockPos(0, 200, 0);
        nether.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        nether.setBlock(pos, AllBlocks.REDSTONE_LINK.getDefaultState().setValue(RedstoneLinkBlock.FACING, Direction.UP), 3);
        var remote = (RedstoneLinkBlockEntity) nether.getBlockEntity(pos); remote.tick();
        frequency(remote.getBehaviour(LinkBehaviour.TYPE)); remote.transmit(9);
        check.accept("WORLD train still observes public sources across dimensions", condition(false).tickCompletion(level, null, new CompoundTag()));
        nether.removeBlock(pos, false);
        check.accept("PRIVATE-only remains invisible after remote WORLD removal", condition(true).tickCompletion(level, null, new CompoundTag()));
        world.transmit(7);
    }

    private static final class CountingSource implements IRedstoneLinkable {
        final BlockPos position;
        final Couple<Frequency> key;
        int reads;
        CountingSource(BlockPos position, Couple<Frequency> key) { this.position = position; this.key = key; }
        @Override public int getTransmittedStrength() { reads++; return 5; }
        @Override public void setReceivedStrength(int value) {}
        @Override public boolean isListening() { return false; }
        @Override public boolean isAlive() { return true; }
        @Override public BlockPos getLocation() { return position; }
        @Override public Couple<Frequency> getNetworkKey() { return key; }
    }

    private void afterPublicationFault(ServerSubLevel craft, RedstoneLinkBlockEntity source,
                                       RedstoneLinkBlockEntity outside, BlockPos marker) {
        var state = FieldworkStateAccess.get(level.getServer());
        var key = IsolationRuntime.key(craft);
        AcceptanceFaults.throwPublication = true;
        boolean rejected = false;
        try { level.removeBlock(marker, false); }
        catch (IllegalStateException expected) { rejected = expected.getMessage().equals("acceptance final publication failure"); }
        finally { AcceptanceFaults.throwPublication = false; }
        check.accept("fault occurs after marker write and candidate registry publication", rejected && AcceptanceFaults.publicationReached && !level.getBlockState(marker).is(FieldworkContent.ISOLATOR.get()));
        check.accept("post-publication failure retains named token", state.transitions().failedTokens().size() == 1);
        long token = state.transitions().failedTokens().iterator().next();
        check.accept("post-publication failure retains craft barrier index", state.transitions().blocked(key, null));
        check.accept("post-publication failure retains affected position barrier", state.transitions().blocked(level, marker, null));
        check.accept("post-publication failure cannot expose source as WORLD", ScopeResolver.at(level, source.getBlockPos(), null) == Scope.PENDING);
        check.accept("post-publication named repair verifies actual removed marker intent", state.transitions().repair(token));
        check.accept("repair releases both position and craft barriers", !state.transitions().blocked(key, null) && !state.transitions().blocked(level, marker, null));

        AcceptanceFaults.throwVisual = true;
        try { level.setBlock(marker, FieldworkContent.ISOLATOR.get().defaultBlockState(), 3); }
        finally { AcceptanceFaults.throwVisual = false; }
        check.accept("post-commit visual failure was exercised", AcceptanceFaults.visualReached);
        check.accept("visual failure does not fabricate an authoritative failed token", state.transitions().failedTokens().isEmpty());
        check.accept("visual failure leaves radio private and committed", state.registry().scope(key).equals(Scope.privateCraft(key)) && !state.transitions().blocked(key, null));
        signal("visual failure cannot leave private source publicly authorized", outside, 7);
        check.accept("failed visual is queued for cold retry", state.visuals().pendingCount() > 0);
        state.visuals().retry();
        check.accept("cold visual retry derives ACTIVE from committed registry", state.visuals().pendingCount() == 0 && level.getBlockState(marker).getValue(IsolatorBlock.ACTIVE));
    }
}
