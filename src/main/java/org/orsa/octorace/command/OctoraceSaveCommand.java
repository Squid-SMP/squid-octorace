package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;

import static net.minecraft.commands.Commands.literal;

public class OctoraceSaveCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("save")
                .executes(ctx -> {
                    Octorace.RACE_MANAGER.getConfig().save();
                    ctx.getSource().sendSuccess(() -> Component.literal("§aConfig saved."), true);
                    return 1;
                });
    }
}
