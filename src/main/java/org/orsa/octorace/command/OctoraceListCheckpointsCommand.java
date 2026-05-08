package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import java.util.List;

import static net.minecraft.commands.Commands.literal;

public class OctoraceListCheckpointsCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("listcheckpoints")
                .executes(OctoraceListCheckpointsCommand::listCheckpoints);
    }

    private static int listCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        List<Checkpoint> checkpoints = config.getCheckpoints();
        if (checkpoints.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No checkpoints set."), false);
            return 1;
        }
        StringBuilder builder = new StringBuilder("§6=== Checkpoints ===\n");
        for (int i = 0; i < checkpoints.size(); i++) {
            builder.append("§e").append(i + 1).append(". §f").append(checkpoints.get(i).describe());
            if (i == checkpoints.size() - 1) {
                builder.append(" §6(finish)");
            }
            builder.append('\n');
        }
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString().trim()), false);
        return 1;
    }
}
