package dev.tassis.createfieldwork.mixin.isolator;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.tassis.createfieldwork.isolator.lifecycle.IsolationRuntime;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.UUID;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class ContainerLifecycleMixin {
    @Unique
    private UUID fieldwork$discardingAllocation;

    @WrapMethod(method = "allocateSubLevel")
    private SubLevel fieldwork$allocated(UUID id, int x, int z, Pose3d pose, Operation<SubLevel> upstream) {
        var container = (SubLevelContainer) (Object) this;
        if (!(container.getLevel() instanceof ServerLevel level)) {
            return upstream.call(id, x, z, pose);
        }
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        if (coordinator.active()) {
            if (container.getSubLevel(id) != null) {
                return upstream.call(id, x, z, pose);
            }
            coordinator.expectAllocation(level, id);
            try {
                var craft = upstream.call(id, x, z, pose);
                if (craft != null) coordinator.allocated(craft);
                return craft;
            } catch (RuntimeException | Error error) {
                coordinator.allocationFailed();
                fieldwork$discardIncompleteAllocation(container, id, x, z, error);
                throw error;
            }
        }
        if (container.getSubLevel(id) != null) {
            return upstream.call(id, x, z, pose);
        }

        coordinator.beginAllocation(level, id);
        boolean success = false;
        Throwable allocationFailure = null;
        try {
            try {
                var craft = upstream.call(id, x, z, pose);
                if (craft != null) {
                    coordinator.allocated(craft);
                    success = true;
                } else {
                    success = fieldwork$allocationAbsent(container, id, x, z);
                }
                return craft;
            } catch (RuntimeException | Error error) {
                allocationFailure = error;
                success = fieldwork$discardIncompleteAllocation(container, id, x, z, error);
                throw error;
            }
        } finally {
            try {
                coordinator.end("allocate", success);
            } catch (RuntimeException | Error error) {
                if (allocationFailure == null) throw error;
                fieldwork$suppress(allocationFailure, error);
            }
        }
    }

    @Unique
    private boolean fieldwork$discardIncompleteAllocation(SubLevelContainer container, UUID id, int x, int z,
                                                           Throwable allocationFailure) {
        boolean discarded = false;
        try {
            var partial = container.getSubLevel(id);
            if (partial == null) {
                var plotted = container.getSubLevel(x, z);
                if (plotted != null && plotted.getUniqueId().equals(id)) partial = plotted;
            }

            if (partial != null) {
                var previous = fieldwork$discardingAllocation;
                fieldwork$discardingAllocation = id;
                try {
                    container.removeSubLevel(partial, SubLevelRemovalReason.REMOVED);
                } finally {
                    fieldwork$discardingAllocation = previous;
                }
            }
            discarded = fieldwork$allocationAbsent(container, id, x, z);
        } catch (RuntimeException | Error error) {
            fieldwork$suppress(allocationFailure, error);
        }

        try {
            if (discarded) {
                LogUtils.getLogger().error(
                    "Fieldwork discarded incomplete Sable allocation {} at plot ({}, {}) after allocation failure",
                    id, x, z, allocationFailure);
            } else {
                LogUtils.getLogger().error(
                    "Fieldwork could not discard incomplete Sable allocation {} at plot ({}, {}); transaction remains quarantined",
                    id, x, z, allocationFailure);
            }
        } catch (RuntimeException | Error error) {
            fieldwork$suppress(allocationFailure, error);
        }
        return discarded;
    }

    @Unique
    private void fieldwork$suppress(Throwable original, Throwable secondary) {
        if (secondary != original) original.addSuppressed(secondary);
    }

    @Unique
    private boolean fieldwork$allocationAbsent(SubLevelContainer container, UUID id, int x, int z) {
        return container.getSubLevel(id) == null && container.getSubLevel(x, z) == null
            && !container.getOccupancy().get(container.getIndex(x, z));
    }

    @WrapMethod(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V")
    private void fieldwork$removed(int x, int z, SubLevelRemovalReason reason, Operation<Void> upstream) {
        var craft = ((SubLevelContainer) (Object) this).getSubLevel(x, z);
        if (craft != null && craft.getUniqueId().equals(fieldwork$discardingAllocation)) {
            upstream.call(x, z, reason);
            return;
        }
        if (!(craft instanceof ServerSubLevel loaded) || !(craft.getLevel() instanceof ServerLevel level) ||
            !level.getServer().isRunning()) {
            upstream.call(x, z, reason); return;
        }
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        var markers = IsolationRuntime.markers(loaded);
        coordinator.begin("remove", level, markers);
        for (var marker : markers) coordinator.expectMarkerDetached(level, marker);
        boolean success = false;
        try {
            coordinator.mutating();
            upstream.call(x, z, reason);
            success = ((SubLevelContainer) (Object) this).getSubLevel(loaded.getUniqueId()) == null;
        } finally { coordinator.end("remove", success); }
    }
}
