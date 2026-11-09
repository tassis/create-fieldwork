package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.wireless.refresh.RefreshScope;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Pure selection algebra using real Create key values, not an authorization oracle. */
final class ReceiverSelectionAcceptance {
    static void run(BiConsumer<String, Boolean> check) {
        var a = new CraftKey(Level.OVERWORLD, UUID.randomUUID());
        var b = new CraftKey(Level.OVERWORLD, UUID.randomUUID());
        var key = Couple.create(Frequency.of(new ItemStack(Items.DIAMOND)), Frequency.of(new ItemStack(Items.EMERALD)));
        var other = Couple.create(Frequency.of(new ItemStack(Items.GOLD_INGOT)), Frequency.of(new ItemStack(Items.IRON_INGOT)));
        var pa = RefreshScope.privateReceivers(Level.OVERWORLD, key, a);
        var pb = RefreshScope.privateReceivers(Level.OVERWORLD, key, b);
        var sa = pa.levels().get(Level.OVERWORLD);
        check.accept("private receiver selection retains exact single network footprint", pa.isFrequency(Level.OVERWORLD, key));
        check.accept("private selection accepts only A, not B WORLD or PENDING", sa.accepts(key, Scope.privateCraft(a))
            && !sa.accepts(key, Scope.privateCraft(b)) && !sa.accepts(key, Scope.WORLD) && !sa.accepts(key, Scope.PENDING));
        check.accept("private receiver selection excludes another frequency", !sa.accepts(other, Scope.privateCraft(a)));
        var both = pa.union(pb).levels().get(Level.OVERWORLD);
        check.accept("same-frequency private union retains both crafts without WORLD", both.accepts(key, Scope.privateCraft(a))
            && both.accepts(key, Scope.privateCraft(b)) && !both.accepts(key, Scope.WORLD));
        var all = pa.union(RefreshScope.frequency(Level.OVERWORLD, key)).levels().get(Level.OVERWORLD);
        check.accept("unrestricted frequency dominates private filters including withdrawal targets", all.accepts(key, Scope.WORLD) && all.accepts(key, Scope.PENDING));
        check.accept("private/unrestricted merge is commutative", pa.union(RefreshScope.frequency(Level.OVERWORLD, key))
            .equals(RefreshScope.frequency(Level.OVERWORLD, key).union(pa)));
        var disjoint = pa.union(RefreshScope.frequency(Level.OVERWORLD, other)).levels().get(Level.OVERWORLD);
        check.accept("unrestricted disjoint key does not widen private A key", !disjoint.accepts(key, Scope.WORLD)
            && disjoint.accepts(other, Scope.WORLD));
        var separate = pa.union(RefreshScope.privateReceivers(Level.OVERWORLD, other, b)).levels().get(Level.OVERWORLD);
        check.accept("disjoint private keys preserve separate receiver sets", separate.accepts(key, Scope.privateCraft(a))
            && !separate.accepts(key, Scope.privateCraft(b)) && separate.accepts(other, Scope.privateCraft(b))
            && !separate.accepts(other, Scope.privateCraft(a)));
        var none = new RefreshScope(Map.of(Level.OVERWORLD, new RefreshScope.Selection(false, Set.of(key), Map.of(key, Set.of()))));
        check.accept("empty receiver filter means NONE, not unrestricted", !none.levels().get(Level.OVERWORLD).accepts(key, Scope.privateCraft(a))
            && !none.levels().get(Level.OVERWORLD).acceptsUnknown(Scope.privateCraft(a)));
        check.accept("NONE receiver set can union with A without widening", none.union(pa).equals(pa));
        check.accept("dimension-wide fallback dominates every receiver filter", pa.union(RefreshScope.level(Level.OVERWORLD))
            .levels().get(Level.OVERWORLD).accepts(other, Scope.WORLD));
        check.accept("unknown key matches only selected private union", sa.acceptsUnknown(Scope.privateCraft(a))
            && !sa.acceptsUnknown(Scope.privateCraft(b)) && !sa.acceptsUnknown(Scope.WORLD) && !sa.acceptsUnknown(Scope.PENDING));
        check.accept("unknown key remains conservative when any selected key is unrestricted", disjoint.acceptsUnknown(Scope.WORLD)
            && disjoint.acceptsUnknown(Scope.PENDING));
        Set<CraftKey> mutableCrafts = new HashSet<>(Set.of(a));
        Map<Couple<Frequency>, Set<CraftKey>> mutableFilters = new HashMap<>(); mutableFilters.put(key, mutableCrafts);
        var copied = new RefreshScope.Selection(false, Set.of(key), mutableFilters);
        mutableCrafts.clear(); mutableFilters.clear();
        check.accept("receiver filter map and nested sets are defensively copied", copied.accepts(key, Scope.privateCraft(a)));
        boolean immutable = false;
        try { copied.receiverFilters().get(key).clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check.accept("published receiver filter set cannot be mutated", immutable);
        boolean rejected = false;
        try { new RefreshScope.Selection(false, Set.of(key), Map.of(other, Set.of(a))); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check.accept("receiver filter cannot name an unselected frequency", rejected);
        rejected = false;
        try { RefreshScope.privateReceivers(Level.NETHER, key, a); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check.accept("private receiver factory rejects foreign craft dimension", rejected);
    }
    private ReceiverSelectionAcceptance() {}
}
