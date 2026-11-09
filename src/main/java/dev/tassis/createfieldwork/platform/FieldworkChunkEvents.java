package dev.tassis.createfieldwork.platform;

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
    public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        FieldworkStateAccess.get(event.getServer()).transitions().finishInitialization();
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        Runnable index = () -> {
            var state = FieldworkStateAccess.get(level.getServer());
            state.publications().trackChunk(level, chunk);
            state.refresh().trackChunk(level, chunk);
            state.bindings().trackChunk(level, chunk);
        };
        if (level.getServer().isSameThread()) index.run();
        else level.getServer().execute(index);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var state = FieldworkStateAccess.get(event.getServer());
        state.visuals().retry();
        state.controllers().postTick();
    }

    @SubscribeEvent
    public static void changedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            FieldworkStateAccess.get(player.server).playerSessions().bind(player);
    }

    @SubscribeEvent
    public static void cloned(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof net.minecraft.server.level.ServerPlayer original) ||
            !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer replacement)) return;
        var state = FieldworkStateAccess.get(original.server);
        // The old identity must be gone (and its endpoints withdrawn) before the
        // already-created replacement can become the current UUID session.
        state.controllers().playerRemoved(original);
        state.playerSessions().bind(replacement);
    }

    @SubscribeEvent
    public static void loggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var server = player.server;
        Runnable cleanup = () -> {
            if (!server.isStopped()) FieldworkStateAccess.get(server).controllers().playerRemoved(player);
        };
        if (server.isSameThread()) cleanup.run();
        else server.execute(cleanup);
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunk = event.getChunk().getPos();
        Runnable cleanup = () -> {
            if (!level.getServer().isStopped()) {
                var state = FieldworkStateAccess.get(level.getServer());
                state.publications().unloaded(level, chunk);
                state.refresh().unloaded(level, chunk);
                state.bindings().unloaded(level, chunk);
            }
        };
        if (level.getServer().isSameThread()) cleanup.run();
        else level.getServer().execute(cleanup);
    }
}
