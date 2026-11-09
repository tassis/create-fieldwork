package dev.tassis.createfieldwork.mixin.schematic.client;

import com.simibubi.create.content.schematics.client.SchematicTransformation;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicTransformation.class, remap = false)
public interface SchematicTransformationAccessor {
    @Accessor("rotation") LerpedFloat fieldwork$rotation();
    @Accessor("chasingPos") Vec3 fieldwork$position();
    @Accessor("prevChasingPos") Vec3 fieldwork$previousPosition();
    @Accessor("target") BlockPos fieldwork$positionTarget();
}
