package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class SetStartCommand {
    @Command("setStart")
    @RequiresPermission(ADMIN_PERM)
    public int setStart(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();

        Vec3 position = player.position();
        float yaw = player.getYRot();
        Identifier dimension = player.level().dimension().identifier();

        RaceConfig config = RACE_MANAGER.getConfig();
        config.setStartPosition(position, yaw);
        config.save();

        var coordText = String.format("%.2f, %.2f, %.2f", position.x, position.y, position.z);
        var message = Component.empty();
        message.append(Component.literal("Start set: ").withStyle(ChatFormatting.BOLD));
        message.append(Component.literal(coordText + " ").withStyle(ChatFormatting.WHITE));
        message.append(Component.literal(String.format("(yaw %.1f) ", yaw)).withStyle(ChatFormatting.GRAY));
        message.append(Component.literal("in " + dimension).withStyle(ChatFormatting.WHITE));
        ctx.getSource().sendSuccess(() -> message, true);

        return 1;
    }
}
