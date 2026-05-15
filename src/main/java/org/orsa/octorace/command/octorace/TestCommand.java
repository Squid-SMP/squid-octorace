package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static org.orsa.octorace.Octorace.RESPAWN_ITEM;
import static org.orsa.octorace.Octorace.WAND_ITEM;

@Command("octorace")
public class TestCommand {
    @Command("test")
    public int test(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        RESPAWN_ITEM.unmoveable.addActivePlayer(player);
        return 1;
    }
}
