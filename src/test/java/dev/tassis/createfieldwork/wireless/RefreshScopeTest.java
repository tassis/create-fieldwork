package dev.tassis.createfieldwork.wireless;

import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefreshScopeTest {
    @Test void emptyScopeDoesNotWidenARequest() {
        var scope = RefreshScope.level(Level.OVERWORLD);
        assertTrue(RefreshScope.none().isEmpty());
        assertEquals(scope, scope.union(RefreshScope.none()));
        assertEquals(scope, RefreshScope.none().union(scope));
        assertFalse(scope.levels().containsKey(Level.NETHER));
    }

    @Test void fallbackDominatesOnlyItsOwnDimension() {
        var explicit = new RefreshScope(Map.of(Level.OVERWORLD, new RefreshScope.Selection(false, Set.of())));
        var mixed = explicit.union(RefreshScope.level(Level.NETHER));
        assertFalse(mixed.levels().get(Level.OVERWORLD).all());
        assertTrue(mixed.levels().get(Level.NETHER).all());
        var widened = mixed.union(RefreshScope.level(Level.OVERWORLD));
        assertTrue(widened.levels().get(Level.OVERWORLD).all());
        assertFalse(widened.levels().containsKey(Level.END));
        assertEquals(widened, RefreshScope.level(Level.OVERWORLD).union(mixed));
    }

    @Test void selectionMapCannotBeChangedAfterPublication() {
        var input = new HashMap<>(RefreshScope.level(Level.OVERWORLD).levels());
        var scope = new RefreshScope(input);
        input.clear();
        assertEquals(Set.of(Level.OVERWORLD), scope.levels().keySet());
        assertThrows(UnsupportedOperationException.class, () -> scope.levels().clear());
        var additional = scope.union(RefreshScope.level(Level.NETHER));
        assertEquals(Set.of(Level.OVERWORLD), scope.levels().keySet());
        assertEquals(Set.of(Level.OVERWORLD, Level.NETHER), additional.levels().keySet());
    }
}
