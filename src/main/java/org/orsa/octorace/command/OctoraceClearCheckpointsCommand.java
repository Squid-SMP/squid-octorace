package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;

import static net.minecraft.commands.Commands.literal;

public class OctoraceClearCheckpointsCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("clearcheckpoints")
                .executes(OctoraceClearCheckpointsCommand::clearCheckpoints);
    }

    private static int clearCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.clearCheckpoints();
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aAll checkpoints cleared."), true);
        return 1;
    }
}
