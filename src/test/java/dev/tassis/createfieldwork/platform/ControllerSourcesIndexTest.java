package dev.tassis.createfieldwork.platform;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.tassis.createfieldwork.controller.routing.ControllerEndpoint;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerSourcesIndexTest {
    @Test void immutableCraftAssociationIsReplacedDirtyMarksDeduplicateAndClearDropsEverything() throws Exception {
        var state = new FieldworkServerState(() -> {});
        var sources = state.controllers();
        var metadata = new HashMap<String, Object>();
        var endpoint = (IRedstoneLinkable) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IRedstoneLinkable.class, ControllerEndpoint.class}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "test-controller-endpoint";
                    default -> null;
                };
                if (method.getDeclaringClass() == ControllerEndpoint.class) {
                    if (args == null || args.length == 0) {
                        if (method.getReturnType() == boolean.class) return metadata.getOrDefault(method.getName(), false);
                        return metadata.get(method.getName());
                    }
                    metadata.put(method.getName(), args[0]);
                }
                return defaultValue(method.getReturnType());
            });
        var tagged = (ControllerEndpoint) endpoint;
        var register = sources.getClass().getDeclaredMethod("registerOrUpdate", ServerLevel.class,
            IRedstoneLinkable.class, ControllerEndpoint.class, Scope.class);
        register.setAccessible(true);
        var a = new CraftKey(Level.OVERWORLD, UUID.randomUUID());
        var b = new CraftKey(Level.OVERWORLD, UUID.randomUUID());

        register.invoke(sources, null, endpoint, tagged, Scope.privateCraft(a));
        assertEquals(1, selected(sources, "byCraft", a));
        register.invoke(sources, null, endpoint, tagged, Scope.privateCraft(b));
        assertEquals(0, selected(sources, "byCraft", a));
        assertEquals(1, selected(sources, "byCraft", b));

        sources.markEndpoint(endpoint);
        sources.markEndpoint(endpoint);
        assertEquals(1, ((Set<?>) field(sources, "dirty")).size());

        sources.clear();
        for (var name : Set.of("endpoints", "byPlayer", "byDevice", "byLectern", "byLevel", "byCraft"))
            assertEquals(0, ((Map<?, ?>) field(sources, name)).size(), name);
        assertEquals(0, ((Set<?>) field(sources, "dirty")).size());
    }

    private static int selected(Object target, String name, Object key) throws Exception {
        var value = ((Map<?, ?>) field(target, name)).get(key);
        return value instanceof Set<?> set ? set.size() : 0;
    }

    private static Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
