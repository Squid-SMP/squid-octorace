package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.command.PlayerNameArg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class RankingsCommand {
    @Command("rankings")
    @RequiresPermission(PLAYER_PERM)
    public int rankings(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var entries = RACE_MANAGER.raceManagerStorage.timeTrialsEntries;
        var sortedAll = entries.stream().sorted(Comparator.comparingDouble(e -> e.timeMs)).toList();
        var top30 = sortedAll.subList(0, Math.min(30, sortedAll.size()));

        if (top30.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No time trials results yet.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        var message = Component.literal("=== Time Trials Rankings ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        source.sendSuccess(() -> message, false);

        var rankings = Component.literal("");

        for (int i = 0; i < top30.size(); i++) {
            var entry = top30.get(i);
            var rank = Component.literal("\n" + (i + 1) + ". ").withStyle(ChatFormatting.YELLOW);
            var name = Component.literal(entry.displayName).withStyle(ChatFormatting.WHITE);
            var time = Component.literal(String.format(" (%.2fs)", entry.timeMs / 1000.0)).withStyle(ChatFormatting.GRAY);
            rankings.append(rank).append(name).append(time);
        }

        source.sendSuccess(() -> rankings, false);
        return 1;
    }

    @Command({"rankings", "player"})
    @RequiresPermission(PLAYER_PERM)
    public int rankingsPlayer(CommandContext<CommandSourceStack> ctx, @Name("player") PlayerNameArg player) {
        var source = ctx.getSource();
        var playerName = player.name();
        var entries = RACE_MANAGER.raceManagerStorage.timeTrialsEntries;
        var sortedAll = entries.stream().sorted(Comparator.comparingDouble(e -> e.timeMs)).toList();
        var rank = -1;

        for (int i = 0; i < sortedAll.size(); i++) {
            if (sortedAll.get(i).displayName.equalsIgnoreCase(playerName)) {
                rank = i + 1;
                break;
            }
        }

        if (rank == -1) {
            source.sendSuccess(() -> Component.literal("No result found for " + playerName + ".").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        var playerEntry = sortedAll.get(rank - 1);
        var finalRank = rank;
        var message = Component.literal(playerEntry.displayName).withStyle(ChatFormatting.WHITE);
        message.append(Component.literal(" — rank ").withStyle(ChatFormatting.GRAY));
        message.append(Component.literal("#" + finalRank).withStyle(ChatFormatting.YELLOW));
        message.append(Component.literal(String.format(" (%.2fs)", playerEntry.timeMs / 1000.0)).withStyle(ChatFormatting.GRAY));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    @Command({"rankings", "remove"})
    @RequiresPermission(ADMIN_PERM)
    public int rankingsRemove(CommandContext<CommandSourceStack> ctx, @Name("player") ServerPlayer player) {
        var source = ctx.getSource();
        var entries = RACE_MANAGER.raceManagerStorage.timeTrialsEntries;
        var uuid = player.getUUID();

        for (var entry : new ArrayList<>(entries)) {
            if (Objects.equals(entry.uuid.toString(), uuid.toString())) {
                entries.remove(entry);
                var message = Component.literal("Removed ranking of " + player.getPlainTextName() + ".").withStyle(ChatFormatting.GRAY);
                source.sendSuccess(() -> message, false);
                return 1;
            }
        }

        var message = Component.literal("No ranking for that player.").withStyle(ChatFormatting.GRAY);
        source.sendSuccess(() -> message, false);

        return 1;
    }
}
