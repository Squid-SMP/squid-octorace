package org.orsa.octorace.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceAddCheckpointCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("addcheckpoint")
                .then(argument("from", BlockPosArgument.blockPos())
                        .then(argument("to", BlockPosArgument.blockPos())
                                .executes(ctx -> addCheckpoint(ctx, null))
                                .then(argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> addCheckpoint(ctx,
                                                StringArgumentType.getString(ctx, "name"))))));
    }

    private static int addCheckpoint(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        BlockPos a = BlockPosArgument.getBlockPos(ctx, "from");
        BlockPos b = BlockPosArgument.getBlockPos(ctx, "to");

        double minX = Math.min(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double minZ = Math.min(a.getZ(), b.getZ());
        double maxX = Math.max(a.getX(), b.getX()) + 1.0;
        double maxY = Math.max(a.getY(), b.getY()) + 1.0;
        double maxZ = Math.max(a.getZ(), b.getZ()) + 1.0;

        String dimensionId = ctx.getSource().getLevel().dimension().identifier().toString();
        Checkpoint checkpoint = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name, dimensionId);
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.addCheckpoint(checkpoint);
        config.save();

        final int index = config.getCheckpointCount();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aAdded checkpoint #" + index + ": " + checkpoint.describe()), true);
        return 1;
    }
}
