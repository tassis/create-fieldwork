package dev.tassis.createfieldwork.test.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Carriage.DimensionalCarriageEntity.class, remap = false)
public interface ProbeCarriageAccess {
    @Invoker("removeAndSaveEntity") void probe$removeAndSave(CarriageContraptionEntity entity, boolean dismount);
}
