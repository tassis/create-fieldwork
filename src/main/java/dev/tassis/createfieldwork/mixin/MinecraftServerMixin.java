package dev.tassis.createfieldwork.mixin;

import dev.tassis.createfieldwork.server.FieldworkServerState;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements FieldworkStateAccess {
    @Unique private FieldworkServerState fieldwork$state;

    @Override
    public FieldworkServerState fieldwork$state() {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!server.isSameThread()) throw new IllegalStateException("Fieldwork state requires the server thread");
        if (fieldwork$state == null) fieldwork$state = new FieldworkServerState(server);
        fieldwork$state.checkThread();
        return fieldwork$state;
    }

    @Inject(method = "stopServer()V", at = @At("RETURN"), require = 1, allow = 1)
    private void fieldwork$stopped(CallbackInfo ci) {
        if (fieldwork$state != null) fieldwork$state.close();
    }
}
