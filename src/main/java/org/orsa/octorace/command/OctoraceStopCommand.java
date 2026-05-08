package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;

import static net.minecraft.commands.Commands.literal;

public class OctoraceStopCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("stop")
                .executes(ctx -> {
                    Octorace.RACE_MANAGER.stopRace("Stopped by admin.");
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Race stopped."), true);
                    return 1;
                });
    }
}
