package dev.tassis.createfieldwork.acceptance.mixin;

import dev.tassis.createfieldwork.acceptance.BindingFaults;
import dev.tassis.createfieldwork.isolator.binding.BindingRevocations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(value = BindingRevocations.class, remap = false)
public abstract class BindingLedgerFaultMixin {
    @Inject(method = "issue", at = @At("RETURN"), require = 1, allow = 1)
    private void acceptance$issued(CallbackInfoReturnable<UUID> cir) { BindingFaults.hit("issue"); }
    @Inject(method = "revoke", at = @At("RETURN"), require = 2, allow = 2)
    private void acceptance$revoked(UUID id, CallbackInfo ci) { BindingFaults.hit("revocation"); }
}
