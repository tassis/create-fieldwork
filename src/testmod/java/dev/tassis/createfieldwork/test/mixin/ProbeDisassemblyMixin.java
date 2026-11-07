package dev.tassis.createfieldwork.test.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.tassis.createfieldwork.test.ProbeLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SimAssemblyHelper.class, remap = false)
public abstract class ProbeDisassemblyMixin {
    @WrapMethod(method = "disassembleSubLevel")
    private static void probe$disassemble(Level level, SubLevel craft, BlockPos anchor, BlockPos target,
                                          Rotation rotation, boolean sound, Operation<Void> original) {
        if (!ProbeLifecycle.enabled) { original.call(level, craft, anchor, target, rotation, sound); return; }
        ProbeLifecycle.begin("disassemble", (ServerLevel) level, ProbeLifecycle.blocks((ServerSubLevel) craft));
        boolean success = false;
        try { original.call(level, craft, anchor, target, rotation, sound); success = true; }
        finally { ProbeLifecycle.end("disassemble", (ServerLevel) level, success); }
    }
}
