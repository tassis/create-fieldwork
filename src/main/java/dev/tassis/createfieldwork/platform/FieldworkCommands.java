package dev.tassis.createfieldwork.platform;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CreateFieldwork.MOD_ID)
public final class FieldworkCommands {
    private FieldworkCommands() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fieldwork").requires(source -> source.hasPermission(2))
            .then(Commands.literal("status").executes(context -> {
                var state = FieldworkStateAccess.get(context.getSource().getServer());
                context.getSource().sendSuccess(() -> Component.literal("Fieldwork withheld transactions: " + state.transitions().failedTokens()), false);
                return 1;
            }))
            .then(Commands.literal("repair").then(Commands.argument("token", LongArgumentType.longArg(1)).executes(context -> {
                long token = LongArgumentType.getLong(context, "token");
                boolean repaired = FieldworkStateAccess.get(context.getSource().getServer()).transitions().repair(token);
                if (repaired) context.getSource().sendSuccess(() -> Component.literal("Fieldwork verified and repaired transaction " + token), true);
                else context.getSource().sendFailure(Component.literal("Fieldwork cannot verify transaction " + token + "; restore the required marker blocks and retry. No barrier was removed."));
                return repaired ? 1 : 0;
            }))));
    }
}
