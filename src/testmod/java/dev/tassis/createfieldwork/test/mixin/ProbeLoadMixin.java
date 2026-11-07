package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;

@Mixin(value = SubLevelSerializer.class, remap = false)
public abstract class ProbeLoadMixin {
    @WrapMethod(method = "fullyLoad")
    private static ServerSubLevel probe$load(ServerLevel level, SubLevelData data, Operation<ServerSubLevel> original) {
        if (!ProbeLifecycle.enabled) return original.call(level, data);
        if (System.getProperty("fieldwork.probe.phase", "").endsWith("-multi")) {
            // Controlled fixture input: exercise the real lazy/packed BE branch.
            var chunks = data.fullTag().getCompound("plot").getCompound("chunks");
            for (var key : chunks.getAllKeys()) {
                for (var tag : chunks.getCompound(key).getList("block_entities", 10)) {
                    ((CompoundTag) tag).putBoolean("keepPacked", true);
                    ProbeLifecycle.packedNbtInputs++;
                }
            }
        }
        ProbeLifecycle.begin("load", level, List.of());
        ProbeLifecycle.expectLoad(level, data.uuid());
        boolean success = false;
        try {
            var result = original.call(level, data);
            success = result != null;
            return result;
        } finally { ProbeLifecycle.end("load", level, success); }
    }
}
