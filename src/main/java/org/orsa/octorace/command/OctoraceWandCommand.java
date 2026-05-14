package org.orsa.octorace.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceWandCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("wand")
                .executes(OctoraceWandCommand::giveWand)
                .then(literal("confirm")
                        .executes(ctx -> confirmWand(ctx, null))
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> confirmWand(ctx,
                                        StringArgumentType.getString(ctx, "name")))));
    }

    private static int giveWand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
//        ServerPlayer player = ctx.getSource().getPlayerOrException();
//        ItemStack wand = WandManager.createWand();
//        if (!player.getInventory().add(wand)) {
//            player.drop(wand, false);
//        }
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                "§aOctorace Wand given. §7Left-click to set corner 1, right-click to set corner 2."), false);
        return 1;
    }

    private static int confirmWand(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
//        ServerPlayer player = ctx.getSource().getPlayerOrException();
//        UUID uuid = player.getUUID();
//
//        if (!WandManager.hasBothPositions(uuid)) {
//            ctx.getSource().sendFailure(Component.literal(
//                    "§cSet both corners first (left-click and right-click with the wand)."));
//            return 0;
//        }
//
//        BlockPos a = WandManager.getPos1(uuid);
//        BlockPos b = WandManager.getPos2(uuid);
//
//        double minX = Math.min(a.getX(), b.getX());
//        double minY = Math.min(a.getY(), b.getY());
//        double minZ = Math.min(a.getZ(), b.getZ());
//        double maxX = Math.max(a.getX(), b.getX()) + 1.0;
//        double maxY = Math.max(a.getY(), b.getY()) + 1.0;
//        double maxZ = Math.max(a.getZ(), b.getZ()) + 1.0;
//
//        Checkpoint checkpoint = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name, WandManager.getWandDimension(uuid));
//        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
//        config.addCheckpoint(checkpoint);
//        config.save();
//
//        WandManager.clear(uuid);
//
//        final int index = config.getCheckpointCount();
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                "§aCheckpoint #" + index + " saved: " + checkpoint.describe()), true);
        return 1;
    }
}
