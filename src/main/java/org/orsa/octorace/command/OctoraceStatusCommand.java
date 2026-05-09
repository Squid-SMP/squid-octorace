package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.game.RaceManager;

import static net.minecraft.commands.Commands.literal;

public class OctoraceStatusCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("status")
                .executes(OctoraceStatusCommand::status);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        RaceManager raceManager = Octorace.RACE_MANAGER;
        RaceConfig config = raceManager.getConfig();
        String message = "§6=== Octorace Status ===\n" +
                "§eCheckpoints: §f" + config.getCheckpointCount() + '\n' +
                "§eStart configured: §f" + (config.getStartPosition() != null) + '\n' +
                "§eDimension: §f" + (config.getDimensionId() == null ? "<unset>" : config.getDimensionId()) + '\n' +
                "§eReady to run: §f" + config.isReady();
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
