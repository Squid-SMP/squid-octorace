package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class StartCommand {
    @Command({"start","versus"})
    @RequiresPermission(PLAYER_PERM)
    public int startVersus(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayedOwnerParty(sourcePlayer);
        if (party == null || party.players.size() == 1) {
            source.sendFailure(Component.literal("Cannot start a versus race alone. Create a party and invite at least one other player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        for (var player : party.players) {
            if (player.level() != RACE_MANAGER.dimension) {
                source.sendFailure(Component.literal("All party members must be in the race dimension to start.").withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        var successMessage = Component.literal("Starting race!").withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> successMessage, false);

        RACE_MANAGER.startRace(party.players, false);

        return 1;
    }

    @Command({"start","timeTrials"})
    @RequiresPermission(PLAYER_PERM)
    public int startTimeTrials(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayedOwnerParty(sourcePlayer);
        if (party != null) {
            for (var player : party.players) {
                if (player.level() != RACE_MANAGER.dimension) {
                    player.sendSystemMessage(Component.literal("You must be in the race dimension to start a time trial.").withStyle(ChatFormatting.RED));
                    continue;
                }
                RACE_MANAGER.startTimeTrials(player);
            }

            return 1;
        }

        if (sourcePlayer.level() != RACE_MANAGER.dimension) {
            source.sendFailure(Component.literal("You must be in the race dimension to start a time trial.").withStyle(ChatFormatting.RED));
            return 0;
        }

        try {
            RACE_MANAGER.startTimeTrials(sourcePlayer);
        } catch (Exception e) {
            LOGGER.error("startTimeTrials failed", e);
            sourcePlayer.sendSystemMessage(Component.literal(e.toString()));
        }

        return 1;
    }

    @Command({"start","globalVersus"})
    @RequiresPermission(ADMIN_PERM)
    public int startGlobalVersus(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        List<ServerPlayer> playersInDimension = new ArrayList<>();
        for (var player : SERVER.getPlayerList().getPlayers()) {
            if (player.level() == RACE_MANAGER.dimension) {
                playersInDimension.add(player);
            }
        }

        if (playersInDimension.isEmpty() || playersInDimension.size() == 1) {
            var message = Component.literal("Not enough player in the correct dimension. Need at least two players.").withStyle(ChatFormatting.RED);
            source.sendFailure(message);
            return 0;
        }

        var successMessage = Component.literal("Starting race!").withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> successMessage, false);

        RACE_MANAGER.startRace(playersInDimension, true);

        return 1;
    }

    @Command({"endGlobal"})
    @RequiresPermission(ADMIN_PERM)
    public int endGlobal(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        for (var race : new ArrayList<>(RACE_MANAGER.ongoingRaces)) {
            if (race.global) {
                race.end(true);
            }
        }

        return 1;
    }
}
