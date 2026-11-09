package dev.tassis.createfieldwork.wireless.scope;

import dev.tassis.createfieldwork.isolator.state.CraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PairPolicyTest {
    private static final ResourceKey<Level> OVERWORLD = dimension("overworld");
    private static final ResourceKey<Level> NETHER = dimension("the_nether");
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private record Example(String name, Scope scope, String authorizedGroup) {}

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("minecraft", name));
    }

    static Stream<Arguments> pairs() {
        var examples = List.of(
            new Example("world", Scope.WORLD, "world"),
            new Example("pending", Scope.PENDING, null),
            new Example("private A", Scope.privateCraft(new CraftKey(OVERWORLD, A)), "A"),
            new Example("independent A value", Scope.privateCraft(new CraftKey(dimension("overworld"), UUID.fromString(A.toString()))), "A"),
            new Example("private B", Scope.privateCraft(new CraftKey(OVERWORLD, B)), "B"),
            new Example("same UUID other dimension", Scope.privateCraft(new CraftKey(NETHER, A)), "nether A")
        );
        return examples.stream().flatMap(source -> examples.stream().flatMap(target ->
            Stream.of(false, true).map(upstream -> Arguments.of(source.name, target.name, source.scope, target.scope,
                upstream, source.authorizedGroup != null && source.authorizedGroup.equals(target.authorizedGroup)))));
    }

    @ParameterizedTest(name = "{0} -> {1}, upstream={4}")
    @MethodSource("pairs")
    void authorizationTruthTable(String sourceName, String targetName, Scope source, Scope target,
                                 boolean upstream, boolean scopeAllowed) {
        assertEquals(scopeAllowed, PairPolicy.allows(source, target));
        assertEquals(scopeAllowed, PairPolicy.allows(target, source), "policy must be symmetric");
        assertEquals(scopeAllowed && upstream, PairPolicy.allows(source, target, upstream));
        assertEquals(scopeAllowed && upstream, PairPolicy.allows(target, source, upstream));
    }

    @Test
    void craftIdentityIsAValueRatherThanAnObjectReference() {
        var first = new CraftKey(OVERWORLD, A);
        var second = new CraftKey(dimension("overworld"), UUID.fromString(A.toString()));
        assertNotSame(first, second);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(Scope.privateCraft(first), Scope.privateCraft(second));
        assertNotEquals(first, new CraftKey(NETHER, A));
        assertNotEquals(first, new CraftKey(OVERWORLD, B));
    }

    @Test
    void missingIdentityIsRejectedInsteadOfBecomingPublic() {
        assertThrows(NullPointerException.class, () -> new CraftKey(null, A));
        assertThrows(NullPointerException.class, () -> new CraftKey(OVERWORLD, null));
        assertThrows(NullPointerException.class, () -> Scope.privateCraft(null));
    }

    @Test
    void missingScopeIsAProgrammingErrorEvenWhenUpstreamDenies() {
        assertThrows(NullPointerException.class, () -> PairPolicy.allows(null, Scope.WORLD));
        assertThrows(NullPointerException.class, () -> PairPolicy.allows(Scope.WORLD, null));
        assertThrows(NullPointerException.class, () -> PairPolicy.allows(null, Scope.WORLD, false));
        assertThrows(NullPointerException.class, () -> PairPolicy.allows(Scope.WORLD, null, false));
    }
}
