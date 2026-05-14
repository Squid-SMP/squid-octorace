package org.orsa.octorace.commandNew.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.Octorace;

import java.util.ArrayList;
import java.util.List;

import static org.orsa.octorace.Octorace.PARTY_MANAGER;
import static org.orsa.octorace.Octorace.RACE_MANAGER;

@Command("octorace")
public class StartCommand {
    @Command({"start","versus"})
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

        RACE_MANAGER.startTimeTrials(sourcePlayer);

        return 1;
    }
}
