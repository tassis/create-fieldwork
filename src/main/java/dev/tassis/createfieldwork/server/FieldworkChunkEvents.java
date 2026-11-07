package dev.tassis.createfieldwork.server;

import dev.tassis.createfieldwork.CreateFieldwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Index supported loaded transmitters on the cold chunk path, including packed block entities. */
@EventBusSubscriber(modid = CreateFieldwork.MOD_ID)
public final class FieldworkChunkEvents {
    private FieldworkChunkEvents() {}

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        Runnable index = () -> FieldworkStateAccess.get(level.getServer()).publications().trackChunk(level, chunk);
        if (level.getServer().isSameThread()) index.run();
        else level.getServer().execute(index);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        FieldworkStateAccess.get(event.getServer()).visuals().retry();
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunk = event.getChunk().getPos();
        Runnable cleanup = () -> {
            if (!level.getServer().isStopped()) FieldworkStateAccess.get(level.getServer()).publications().unloaded(level, chunk);
        };
        if (level.getServer().isSameThread()) cleanup.run();
        else level.getServer().execute(cleanup);
    }
}
