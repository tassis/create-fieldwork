package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.BindingFaults;
import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(value = BrassIsolatorBlockEntity.class, remap = false)
public abstract class BindingGrantFaultMixin {
    @Inject(method = "grant", at = @At("RETURN"), require = 1, allow = 1)
    private void acceptance$grant(CraftKey craft, UUID id, long generation, CallbackInfo ci) { BindingFaults.hit("grant"); }
}
