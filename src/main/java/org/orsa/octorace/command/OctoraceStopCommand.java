package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.game.Race;
import org.orsa.octorace.game.RaceManager;

import static net.minecraft.commands.Commands.literal;

public class OctoraceStopCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("stop")
                .executes(OctoraceStopCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        for (Race race : Octorace.RACE_MANAGER.ongoingRaces) {
            race.endRace(Race.RaceEndReason.STOPPED_BY_ADMIN);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§7Races stopped."), true);
        return 1;
    }
}
