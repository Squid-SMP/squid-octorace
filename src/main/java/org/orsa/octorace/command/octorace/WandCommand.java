package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class WandCommand {
    @Command("wand")
    @RequiresPermission(ADMIN_PERM)
    public int giveWand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ItemStack wand = WAND_ITEM.createWand();

        if (!player.getInventory().add(wand)) {
            player.drop(wand, false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§aOctorace Wand given. §7Left-click to set corner 1, right-click to set corner 2."), false);
        return 1;
    }
}