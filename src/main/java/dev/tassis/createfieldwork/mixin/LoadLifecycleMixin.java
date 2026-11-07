package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import java.util.List;

@Mixin(value = SubLevelSerializer.class, remap = false)
public abstract class LoadLifecycleMixin {
    @WrapMethod(method = "fullyLoad")
    private static ServerSubLevel fieldwork$load(ServerLevel level, SubLevelData data, Operation<ServerSubLevel> upstream) {
        var coordinator = FieldworkStateAccess.get(level.getServer()).transitions();
        coordinator.begin("load", level, List.of());
        boolean success = false;
        try {
            coordinator.expectLoad(level, data.uuid());
            coordinator.mutating();
            var craft = upstream.call(level, data);
            success = craft != null;
            return craft;
        } finally { coordinator.end("load", success); }
    }
}
