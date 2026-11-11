package dev.tassis.createfieldwork.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

/** Configuration-phase agreement: both peers must activate the same optional integration mode. */
public final class IntegrationHandshake {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateFieldwork.MOD_ID, "integration_state");

    private IntegrationHandshake() {}

    public record State(boolean active) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, State::active, State::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").configurationBidirectional(State.TYPE, State.STREAM_CODEC,
            new DirectionalPayloadHandler<>(IntegrationHandshake::client, IntegrationHandshake::server));
    }

    public static void registerTask(RegisterConfigurationTasksEvent event) {
        event.register(new Task(AeronauticsIntegration.active()));
    }

    private static void client(State state, IPayloadContext context) {
        boolean local = AeronauticsIntegration.active();
        if (!agrees(state.active(), local)) {
            context.disconnect(mismatch(state.active(), local));
            return;
        }
        context.reply(new State(local));
    }

    private static void server(State state, IPayloadContext context) {
        boolean local = AeronauticsIntegration.active();
        if (!agrees(local, state.active())) {
            context.disconnect(mismatch(local, state.active()));
            return;
        }
        context.finishCurrentTask(Task.TYPE);
    }

    private static Component mismatch(boolean server, boolean client) {
        return Component.literal("Create: Fieldwork integration mismatch (server=" + server + ", client=" + client + ")");
    }

    static boolean agrees(boolean server, boolean client) { return server == client; }

    private record Task(boolean active) implements ICustomConfigurationTask {
        private static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ID);
        @Override public void run(Consumer<CustomPacketPayload> sender) { sender.accept(new State(active)); }
        @Override public Type type() { return TYPE; }
    }
}
