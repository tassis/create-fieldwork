package dev.tassis.createfieldwork.platform;

import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/** Keeps always-registered block entities inert until the optional integration installs its runtime. */
public final class IsolatorIntegrationBridge {
    public interface Runtime {
        void markerChanged(ServerLevel level, BlockPos pos, boolean present);
        void smartLoaded(BrassIsolatorBlockEntity blockEntity);
        void smartRemoved(BrassIsolatorBlockEntity blockEntity);
        CraftKey currentCraft(BrassIsolatorBlockEntity blockEntity);
    }

    private static final Runtime INERT = new Runtime() {
        @Override public void markerChanged(ServerLevel level, BlockPos pos, boolean present) {}
        @Override public void smartLoaded(BrassIsolatorBlockEntity blockEntity) {}
        @Override public void smartRemoved(BrassIsolatorBlockEntity blockEntity) {}
        @Override public CraftKey currentCraft(BrassIsolatorBlockEntity blockEntity) { return null; }
    };
    private static volatile Runtime runtime = INERT;

    private IsolatorIntegrationBridge() {}

    public static void install(Runtime implementation) {
        if (runtime != INERT) throw new IllegalStateException("Fieldwork integration runtime is already installed");
        runtime = Objects.requireNonNull(implementation, "implementation");
    }

    public static void markerChanged(ServerLevel level, BlockPos pos, boolean present) {
        runtime.markerChanged(level, pos, present);
    }

    public static void smartLoaded(BrassIsolatorBlockEntity blockEntity) { runtime.smartLoaded(blockEntity); }
    public static void smartRemoved(BrassIsolatorBlockEntity blockEntity) { runtime.smartRemoved(blockEntity); }
    public static CraftKey currentCraft(BrassIsolatorBlockEntity blockEntity) { return runtime.currentCraft(blockEntity); }
}
