package org.orsa.octorace.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceSetStartCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("setstart")
                .executes(OctoraceSetStartCommand::setStartHere)
                .then(argument("position", Vec3Argument.vec3())
                        .executes(OctoraceSetStartCommand::setStartAtPos));
    }

    private static int setStartHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 position = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Identifier dimension = player.level().dimension().identifier();
        applyStart(ctx, position, yaw, pitch, dimension);
        return 1;
    }

    private static int setStartAtPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 position = Vec3Argument.getVec3(ctx, "position");
        ServerLevel level = ctx.getSource().getLevel();
        float yaw = ctx.getSource().getRotation().y;
        float pitch = ctx.getSource().getRotation().x;
        applyStart(ctx, position, yaw, pitch, level.dimension().identifier());
        return 1;
    }

    private static void applyStart(CommandContext<CommandSourceStack> ctx, Vec3 position, float yaw, float pitch, Identifier dimension) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.setStartPosition(position, yaw);
        config.setDimensionId(dimension.toString());
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("§aStart set: §f%.2f, %.2f, %.2f §7(yaw %.1f, pitch %.1f) in §f%s",
                        position.x, position.y, position.z, yaw, pitch, dimension)
        ), true);
    }
}
