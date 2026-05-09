package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.Octorace;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class OctoraceStartCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("start")
                .executes(OctoraceStartCommand::startWithAllOnline)
                .then(literal("timeTrials"))
                        .executes(OctoraceStartCommand::startTimeTrials);
    }

    private static int startWithAllOnline(CommandContext<CommandSourceStack> ctx) {
        List<ServerPlayer> all = new ArrayList<>(ctx.getSource().getServer().getPlayerList().getPlayers());

        Octorace.RACE_MANAGER.startRace(all);

        final int count = all.size();
        ctx.getSource().sendSuccess(() -> Component.literal("§aRace starting with " + count + " player(s)."), true);

        return 1;
    }

    private static int startTimeTrials(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Octorace.RACE_MANAGER.startTimeTrials(player);

        return 1;
    }
}
