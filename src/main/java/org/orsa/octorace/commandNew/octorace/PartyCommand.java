package org.orsa.octorace.commandNew.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import static org.orsa.octorace.Octorace.PARTY_MANAGER;

@Command("octorace")
public class PartyCommand {
    @Command("party")
    public int party(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayerParty(sourcePlayer);
        if (party == null) {
            source.sendFailure(Component.literal("You are not in a party.").withStyle(ChatFormatting.RED));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Players in your party ==="), false);
        for (ServerPlayer player : party.players) {
            String text = player.getPlainTextName();
            if (player == party.owner) {
                text = "(👑) " + text;
            }

            String finalText = text;
            source.sendSuccess(() -> Component.literal(finalText), false);
        }

        return 1;
    }

    @Command({"party","create"})
    public int partyCreate(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayerParty(sourcePlayer);
        if (party != null) {
            source.sendFailure(Component.literal("You are already in a party.").withStyle(ChatFormatting.RED));
            return 0;
        }

        PARTY_MANAGER.createParty(sourcePlayer);
        source.sendSuccess(() -> Component.literal("Party created successfully!").withStyle(ChatFormatting.GREEN), false);

        return 1;
    }

    @Command({"party","invite"})
    public int partyInvite(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        if (!PARTY_MANAGER.playerOwnsParty(sourcePlayer)) {
            source.sendFailure(Component.literal("You are not the owner of a party.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (player == sourcePlayer) {
            source.sendFailure(Component.literal("Cannot invite yourself.").withStyle(ChatFormatting.RED));
            return 0;
        }

        var party = PARTY_MANAGER.getPlayerParty(player);
        if (party != null) {
            source.sendFailure(Component.literal("Player is already in a party.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Style joinStyle = Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/octorace party join " + sourcePlayer.getPlainTextName()))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to join the party")));

        var joinButton = Component.literal("[Click to Join]").setStyle(joinStyle);

        var inviteMessage = Component.literal(sourcePlayer.getPlainTextName() + " has invited you to their octorace party! ").withStyle(ChatFormatting.GREEN);
        inviteMessage.append(joinButton);

        player.sendSystemMessage(inviteMessage);
        source.sendSuccess(() -> Component.literal("Invited " + player.getPlainTextName() + " to your party.").withStyle(ChatFormatting.GREEN), false);

        PARTY_MANAGER.createInvite(sourcePlayer, player);

        return 1;
    }

    @Command({"party","join"})
    public int partyJoin(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        if (PARTY_MANAGER.tryJoinParty(sourcePlayer, player)) {
            var successMessage = Component.literal("Joined party of " + player.getPlainTextName() + ".").withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> successMessage, false);
            return 1;
        }

        var failureMessage = Component.literal("You have not been invited to this party.").withStyle(ChatFormatting.RED);
        source.sendFailure(failureMessage);

        return 0;
    }

    @Command({"party","leave"})
    public int partyLeave(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();

        var party = PARTY_MANAGER.getPlayerParty(sourcePlayer);
        if (party == null) {
            source.sendFailure(Component.literal("You are not in a party.").withStyle(ChatFormatting.RED));
            return 0;
        }

        party.removePlayer(sourcePlayer);
        var successMessage = Component.literal("Left the party.").withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> successMessage, false);
        return 1;
    }
}
