package dev.tassis.createfieldwork.mixin.wireless;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import dev.tassis.createfieldwork.wireless.publication.SourcePublications;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RedstoneLinkBlockEntity.class, remap = false)
public abstract class LinkPublicationMixin {
    @Inject(method = "read", at = @At("TAIL"), require = 1, allow = 1)
    private void fieldwork$pending(CompoundTag tag, HolderLookup.Provider registries, boolean client, CallbackInfo ci) {
        var entity = (RedstoneLinkBlockEntity) (Object) this;
        if (!client && entity.getLevel() instanceof ServerLevel level && level.getServer().isSameThread()
            && SourcePublications.transmitter(entity.getBlockState()))
            FieldworkStateAccess.get(level.getServer()).publications().expect(level, entity.getBlockPos(), false);
    }
}
