package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
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

        var message = Component.literal(String.format("§LStart set: §f%.2f, %.2f, %.2f §7(yaw %.1f) §fin %s", position.x, position.y, position.z, yaw, dimension));
        ctx.getSource().sendSuccess(() -> message, true);

        return 1;
    }
}
