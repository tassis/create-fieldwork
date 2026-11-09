package dev.tassis.createfieldwork.platform.registry;

import dev.tassis.createfieldwork.platform.CreateFieldwork;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class FieldworkDataComponents {
    private static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateFieldwork.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> DEVICE = TYPES.register("controller_device",
        () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RESET_PENDING = TYPES.register("controller_reset_pending",
        () -> DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private FieldworkDataComponents() {}
}
