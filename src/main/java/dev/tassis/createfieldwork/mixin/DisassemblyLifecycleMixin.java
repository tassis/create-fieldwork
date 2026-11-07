package dev.tassis.createfieldwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.tassis.createfieldwork.isolation.IsolationRuntime;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SimAssemblyHelper.class, remap = false)
public abstract class DisassemblyLifecycleMixin {
    @WrapMethod(method = "disassembleSubLevel")
    private static void fieldwork$disassemble(Level level, SubLevel craft, BlockPos anchor, BlockPos target,
                                             Rotation rotation, boolean sound, Operation<Void> upstream) {
        if (!(level instanceof ServerLevel server) || !(craft instanceof ServerSubLevel loaded)) {
            upstream.call(level, craft, anchor, target, rotation, sound); return;
        }
        var coordinator = FieldworkStateAccess.get(server.getServer()).transitions();
        coordinator.begin("disassemble", server, IsolationRuntime.blocks(loaded));
        boolean success = false;
        try { coordinator.mutating(); upstream.call(level, craft, anchor, target, rotation, sound); success = true; }
        finally { coordinator.end("disassemble", success); }
    }
}
