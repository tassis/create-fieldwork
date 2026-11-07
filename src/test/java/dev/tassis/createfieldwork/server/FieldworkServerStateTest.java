package dev.tassis.createfieldwork.server;

import dev.tassis.createfieldwork.isolation.CraftKey;
import dev.tassis.createfieldwork.isolation.IsolationRegistry;
import dev.tassis.createfieldwork.wireless.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FieldworkServerStateTest {
    private final Thread owner = Thread.currentThread();
    private final CraftKey a = new CraftKey(Level.OVERWORLD, UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final BlockPos first = new BlockPos(1, 2, 3);
    private final BlockPos second = new BlockPos(4, 5, 6);

    private FieldworkServerState state() {
        return new FieldworkServerState(() -> {
            if (Thread.currentThread() != owner) throw new IllegalStateException("wrong thread");
        });
    }

    @Test void unknownKnownCraftIsPrivateWithoutQueryMutation() {
        var state = state();
        long epoch = state.visibilityEpoch();
        assertEquals(IsolationRegistry.Status.INITIALIZING, state.registry().snapshot(a).status());
        assertEquals(Scope.privateCraft(a), state.registry().scope(a));
        assertEquals(epoch, state.visibilityEpoch());
        assertThrows(IllegalStateException.class, () -> state.registry().marker(a, first, false));
        assertEquals(Scope.privateCraft(a), state.registry().scope(a));
    }

    @Test void zeroOneMultipleAndDuplicateEvents() {
        var state = state();
        var registry = state.registry();
        registry.publish(a, List.of());
        assertEquals(Scope.WORLD, registry.scope(a));
        registry.marker(a, first, true);
        assertEquals(Scope.privateCraft(a), registry.scope(a));
        long once = state.visibilityEpoch();
        registry.marker(a, first, true);
        assertEquals(once, state.visibilityEpoch());
        registry.marker(a, second, true);
        registry.marker(a, first, false);
        assertEquals(Set.of(second), registry.snapshot(a).positions());
        assertEquals(Scope.privateCraft(a), registry.scope(a));
        registry.marker(a, second, false);
        assertEquals(Scope.WORLD, registry.scope(a));
        long empty = state.visibilityEpoch();
        registry.marker(a, second, false);
        assertEquals(empty, state.visibilityEpoch());
    }

    @Test void immutableSnapshotAndMutableBlockPositionCannotChangePublishedState() {
        var state = state();
        var mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        var input = new ArrayList<BlockPos>();
        input.add(mutable);
        state.registry().publish(a, input);
        var snapshot = state.registry().snapshot(a);
        input.clear(); mutable.set(9, 9, 9);
        assertEquals(Set.of(first), snapshot.positions());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.positions().clear());
        state.registry().marker(a, second, true);
        assertEquals(Set.of(first), snapshot.positions());
        assertEquals(Set.of(first, second), state.registry().snapshot(a).positions());
    }

    @Test void reconstructionFailureAndRetryAreNotEmptyReady() {
        var state = state();
        var registry = state.registry();
        registry.publish(a, List.of());
        registry.begin(a);
        assertEquals(Scope.privateCraft(a), registry.scope(a));
        registry.fail(a);
        assertEquals(Scope.PENDING, registry.scope(a));
        assertThrows(IllegalStateException.class, () -> registry.marker(a, first, true));
        registry.begin(a);
        registry.publish(a, List.of(first, first));
        assertEquals(Set.of(first), registry.snapshot(a).positions());
        assertEquals(Scope.privateCraft(a), registry.scope(a));
    }

    @Test void serverDimensionAndCraftIdentitiesDoNotShareState() {
        var one = state(); var two = state();
        var nether = new CraftKey(Level.NETHER, a.id());
        var otherCraft = new CraftKey(a.dimension(), UUID.randomUUID());
        one.registry().publish(a, List.of());
        assertEquals(Scope.WORLD, one.registry().scope(a));
        assertEquals(Scope.privateCraft(a), two.registry().scope(a));
        assertEquals(Scope.privateCraft(nether), one.registry().scope(nether));
        assertEquals(Scope.privateCraft(otherCraft), one.registry().scope(otherCraft));
        assertNotEquals(one.session(), two.session());
        assertEquals(0, two.visibilityEpoch());
    }

    @Test void forgettingAndClosingInvalidateOnlyThisServer() {
        var one = state(); var two = state();
        one.registry().publish(a, List.of()); two.registry().publish(a, List.of());
        var staleRegistry = one.registry();
        one.registry().forget(a);
        assertEquals(Scope.privateCraft(a), one.registry().scope(a));
        assertEquals(Scope.WORLD, two.registry().scope(a));
        one.close(); one.close();
        assertThrows(IllegalStateException.class, one::registry);
        assertThrows(IllegalStateException.class, () -> staleRegistry.scope(a));
        assertEquals(Scope.WORLD, two.registry().scope(a));
    }

    @Test void everyMutationAndQueryRequiresTheOwnerThread() throws InterruptedException {
        var state = state();
        var registry = state.registry();
        var failure = new AtomicReference<Throwable>();
        Thread other = new Thread(() -> {
            try {
                assertThrows(IllegalStateException.class, state::session);
                assertThrows(IllegalStateException.class, state::visibilityEpoch);
                assertThrows(IllegalStateException.class, () -> registry.scope(a));
                assertThrows(IllegalStateException.class, () -> registry.publish(a, List.of(first)));
                assertThrows(IllegalStateException.class, () -> registry.begin(a));
                assertThrows(IllegalStateException.class, () -> registry.fail(a));
                assertThrows(IllegalStateException.class, () -> registry.forget(a));
                assertThrows(IllegalStateException.class, registry::clear);
                assertThrows(IllegalStateException.class, state::close);
            } catch (Throwable error) { failure.set(error); }
        });
        other.start(); other.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertEquals(0, state.visibilityEpoch());
    }
}
