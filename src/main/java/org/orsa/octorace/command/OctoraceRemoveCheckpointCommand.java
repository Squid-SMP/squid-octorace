package org.orsa.octorace.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceRemoveCheckpointCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("removecheckpoint")
                .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(OctoraceRemoveCheckpointCommand::removeCheckpoint));
    }

    private static int removeCheckpoint(CommandContext<CommandSourceStack> ctx) {
        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        if (!config.removeCheckpoint(index)) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid checkpoint index."));
            return 0;
        }
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aRemoved checkpoint #" + (index + 1)), true);
        return 1;
    }
}
