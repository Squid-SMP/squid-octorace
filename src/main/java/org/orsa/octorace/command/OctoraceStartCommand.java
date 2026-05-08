package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.Octorace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceStartCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("start")
                .executes(OctoraceStartCommand::startWithAllOnline)
                .then(argument("players", EntityArgument.players())
                        .executes(OctoraceStartCommand::startWithSelected));
    }

    private static int startWithAllOnline(CommandContext<CommandSourceStack> ctx) {
        List<ServerPlayer> all = new ArrayList<>(ctx.getSource().getServer().getPlayerList().getPlayers());
        return doStart(ctx, all);
    }

    private static int startWithSelected(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> selected = EntityArgument.getPlayers(ctx, "players");
        return doStart(ctx, new ArrayList<>(selected));
    }

    private static int doStart(CommandContext<CommandSourceStack> ctx, List<ServerPlayer> players) {
        String error = Octorace.RACE_MANAGER.startRace(players);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal("§c" + error));
            return 0;
        }
        final int count = players.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§aRace starting with " + count + " player(s)."), true);
        return 1;
    }
}
