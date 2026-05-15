package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class StartCommand {
    @Command({"start","versus"})
    @RequiresPermission(PLAYER_PERM)
    public int startVersus(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayerParty(sourcePlayer);
        if (party == null || party.players.size() == 1) {
            source.sendFailure(Component.literal("Cannot start a versus race alone. Create a party and invite at least one other player.").withStyle(ChatFormatting.RED));
            return 0;
        }

        var successMessage = Component.literal("Starting race!").withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> successMessage, false);

        RACE_MANAGER.startRace(party.players);

        return 1;
    }

    @Command({"start","timeTrials"})
    @RequiresPermission(PLAYER_PERM)
    public int startTimeTrials(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayerParty(sourcePlayer);
        if (party != null) {
            for (var player : party.players) {
                RACE_MANAGER.startTimeTrials(player);
            }

            return 1;
        }

        try {
            RACE_MANAGER.startTimeTrials(sourcePlayer);
        } catch (Exception e) {
            LOGGER.info(e.toString());
            sourcePlayer.sendSystemMessage(Component.literal(e.toString()));
        }

        return 1;
    }
}
