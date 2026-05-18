package org.orsa.octorace.command.octorace;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import java.util.List;
import java.util.Optional;

import static org.orsa.octorace.Octorace.*;

@Command("octorace")
public class CheckpointCommand {
    @Command({"checkpoint","clear"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointClear(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.clearCheckpoints();

        ctx.getSource().sendSuccess(() -> Component.literal("All checkpoints cleared.").withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    @Command({"checkpoint","create"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointCreate(CommandContext<CommandSourceStack> ctx, @Name("name") Optional<String> name) {
        var source = ctx.getSource();
        var sourcePlayer = source.getPlayer();
        var uuid = sourcePlayer.getUUID();
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();

        var checkpoint = WAND_ITEM.tryConfirm(uuid, name.orElse(null));
        if (checkpoint == null) {
            var message = Component.literal("Couldn't create checkpoint. Make sure you've selected both corners with the wand.").withStyle(ChatFormatting.RED);
            ctx.getSource().sendFailure(message);
            return 0;
        }

        var message = Component.literal("Added checkpoint #" + config.checkpoints.size() + ": " + checkpoint.describe()).withStyle(ChatFormatting.GREEN);
        ctx.getSource().sendSuccess(() -> message, true);

        return 1;
    }

    @Command({"checkpoint","remove"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointRemove(CommandContext<CommandSourceStack> ctx, @Name("checkpointId") int checkpointId) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();

        if (!config.removeCheckpoint(checkpointId)) {
            ctx.getSource().sendFailure(Component.literal("Invalid checkpoint index.").withStyle(ChatFormatting.RED));
            return 0;
        }

        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Removed checkpoint " + checkpointId).withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    @Command({"checkpoint","list"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointList(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        List<Checkpoint> checkpoints = config.checkpoints;

        if (checkpoints.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No checkpoints set.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        var message = Component.literal("=== Checkpoints ===").withStyle(ChatFormatting.GOLD);

        for (int i = 0; i < checkpoints.size(); i++) {
            var entry = Component.literal("\n")
                    .append(Component.literal((i) + ". ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(checkpoints.get(i).describe()).withStyle(ChatFormatting.WHITE));

            if (i == checkpoints.size() - 1) {
                entry.append(Component.literal(" (finish)").withStyle(ChatFormatting.GOLD));
            }

            message.append(entry);
        }

        ctx.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    @Command({"checkpoint","edit","toggleTrident"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointEditTrident(CommandContext<CommandSourceStack> ctx, @Name("checkpointId") int checkpointId) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();

        var checkpoint = config.getCheckpoint(checkpointId);
        if (checkpoint.isEmpty()) {
            var message = Component.literal("Invalid checkpoint index.").withStyle(ChatFormatting.RED);
            ctx.getSource().sendFailure(message);
            return 0;
        }

        boolean newValue = !checkpoint.get().trident;
        checkpoint.get().trident = newValue;
        config.save();

        var message = Component.literal("Checkpoint " + checkpointId + " trident: " + newValue).withStyle(ChatFormatting.GREEN);
        ctx.getSource().sendSuccess(() -> message, false);

        return 1;
    }

    @Command({"checkpoint","edit","changePosition"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointEditSwitch(CommandContext<CommandSourceStack> ctx, @Name("checkpointId") int checkpointId, @Name("position") int position) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        var checkpoints = config.checkpoints;

        var checkpoint = config.getCheckpoint(checkpointId);
        if (checkpoint.isEmpty()) {
            var message = Component.literal("Invalid checkpoint index.").withStyle(ChatFormatting.RED);
            ctx.getSource().sendFailure(message);
            return 0;
        }

        if (position < 0) {
            var message = Component.literal("Position can't be negative.").withStyle(ChatFormatting.RED);
            ctx.getSource().sendFailure(message);
            return 0;
        }

        checkpoints.add(position, checkpoints.remove(checkpointId));
        var newIndex = checkpoints.indexOf(checkpoint.get());
        config.save();

        var message = Component.literal("Checkpoint " + checkpointId + " moved to position " + newIndex + ".").withStyle(ChatFormatting.GREEN);
        ctx.getSource().sendSuccess(() -> message, false);

        return 1;
    }

    @Command({"checkpoint","edit","setName"})
    @RequiresPermission(ADMIN_PERM)
    public int checkpointSetName(CommandContext<CommandSourceStack> ctx, @Name("checkpointId") int checkpointId, @Name("name") String name) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();

        var checkpoint = config.getCheckpoint(checkpointId);
        if (checkpoint.isEmpty()) {
            var message = Component.literal("Invalid checkpoint index.").withStyle(ChatFormatting.RED);
            ctx.getSource().sendFailure(message);
            return 0;
        }

        checkpoint.get().name = name;
        config.save();

        var message = Component.literal("Changed name of checkpoint " + checkpointId + " to \"" + name + "\".").withStyle(ChatFormatting.GREEN);
        ctx.getSource().sendSuccess(() -> message, false);

        return 1;
    }

}
