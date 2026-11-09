package dev.tassis.createfieldwork.schematic.client;

import dev.tassis.createfieldwork.schematic.input.ControllerUsePriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Let vanilla handle controller/lectern use instead of letting an offhand
 * schematic tool consume the click. Do not synthesize a second use packet. */
public final class ControllerGesture {
    private ControllerGesture() {}
    public static boolean preferNative() {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) return false;
        var target = mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? mc.level.getBlockState(hit.getBlockPos()) : null;
        return ControllerUsePriority.nativeUse(player.getMainHandItem(), target);
    }
}
