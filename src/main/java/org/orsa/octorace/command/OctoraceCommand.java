package org.orsa.octorace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.game.RaceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OctoraceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("octorace")
                .requires(src -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(src))

                .then(literal("start")
                        .executes(OctoraceCommand::startWithAllOnline)
                        .then(argument("players", EntityArgument.players())
                                .executes(OctoraceCommand::startWithSelected)))

                .then(literal("stop")
                        .executes(ctx -> {
                            Octorace.RACE_MANAGER.stopRace("Stopped by admin.");
                            ctx.getSource().sendSuccess(() -> Component.literal("§7Race stopped."), true);
                            return 1;
                        }))

                .then(literal("status")
                        .executes(OctoraceCommand::status))

                .then(literal("setstart")
                        .executes(OctoraceCommand::setStartHere)
                        .then(argument("position", Vec3Argument.vec3())
                                .executes(OctoraceCommand::setStartAtPos)))

                .then(literal("addcheckpoint")
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .executes(ctx -> addCheckpoint(ctx, null))
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> addCheckpoint(ctx,
                                                        StringArgumentType.getString(ctx, "name"))))))
                )

                .then(literal("removecheckpoint")
                        .then(argument("index", IntegerArgumentType.integer(1))
                                .executes(OctoraceCommand::removeCheckpoint)))

                .then(literal("clearcheckpoints")
                        .executes(OctoraceCommand::clearCheckpoints))

                .then(literal("listcheckpoints")
                        .executes(OctoraceCommand::listCheckpoints))

                .then(literal("save")
                        .executes(ctx -> {
                            Octorace.RACE_MANAGER.getConfig().save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aConfig saved."), true);
                            return 1;
                        }))
        );
    }

    // --- handlers ---

    private static int startWithAllOnline(CommandContext<CommandSourceStack> ctx) {
        List<ServerPlayer> all = new ArrayList<>(ctx.getSource().getServer().getPlayerList().getPlayers());
        return doStart(ctx, all);
    }

    private static int startWithSelected(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> sel = EntityArgument.getPlayers(ctx, "players");
        return doStart(ctx, new ArrayList<>(sel));
    }

    private static int doStart(CommandContext<CommandSourceStack> ctx, List<ServerPlayer> players) {
        String err = Octorace.RACE_MANAGER.startRace(players);
        if (err != null) {
            ctx.getSource().sendFailure(Component.literal("§c" + err));
            return 0;
        }
        final int n = players.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§aRace starting with " + n + " player(s)."), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        RaceManager rm = Octorace.RACE_MANAGER;
        RaceConfig cfg = rm.getConfig();
        String sb = "§6=== Octorace Status ===\n" +
                "§eState: §f" + rm.getState().name() + '\n' +
                "§eCheckpoints: §f" + cfg.getCheckpointCount() + '\n' +
                "§eStart configured: §f" + (cfg.getStartPosition() != null) + '\n' +
                "§eDimension: §f" + (cfg.getDimensionId() == null ? "<unset>" : cfg.getDimensionId()) + '\n' +
                "§eReady to run: §f" + cfg.isReady();
        ctx.getSource().sendSuccess(() -> Component.literal(sb), false);
        return 1;
    }

    private static int setStartHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 pos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Identifier dim = player.level().dimension().identifier();
        applyStart(ctx, pos, yaw, pitch, dim);
        return 1;
    }

    private static int setStartAtPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = Vec3Argument.getVec3(ctx, "position");
        ServerLevel level = ctx.getSource().getLevel();
        float yaw = ctx.getSource().getRotation().y;
        float pitch = ctx.getSource().getRotation().x;
        applyStart(ctx, pos, yaw, pitch, level.dimension().identifier());
        return 1;
    }

    private static void applyStart(CommandContext<CommandSourceStack> ctx, Vec3 pos, float yaw, float pitch, Identifier dim) {
        RaceConfig cfg = Octorace.RACE_MANAGER.getConfig();
        cfg.setStartPosition(pos, yaw);
        cfg.setDimensionId(dim.toString());
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("§aStart set: §f%.2f, %.2f, %.2f §7(yaw %.1f, pitch %.1f) in §f%s",
                        pos.x, pos.y, pos.z, yaw, pitch, dim)
        ), true);
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

        Checkpoint cp = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name);
        RaceConfig cfg = Octorace.RACE_MANAGER.getConfig();

        cfg.addCheckpoint(cp);
        cfg.save();

        final int idx = cfg.getCheckpointCount();

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aAdded checkpoint #" + idx + ": " + cp.describe()), true);
        return 1;
    }

    private static int removeCheckpoint(CommandContext<CommandSourceStack> ctx) {
        int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
        RaceConfig cfg = Octorace.RACE_MANAGER.getConfig();
        if (!cfg.removeCheckpoint(idx)) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid checkpoint index."));
            return 0;
        }
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aRemoved checkpoint #" + (idx + 1)), true);
        return 1;
    }

    private static int clearCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig cfg = Octorace.RACE_MANAGER.getConfig();
        cfg.clearCheckpoints();
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aAll checkpoints cleared."), true);
        return 1;
    }

    private static int listCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig cfg = Octorace.RACE_MANAGER.getConfig();
        List<Checkpoint> cps = cfg.getCheckpoints();
        if (cps.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No checkpoints set."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6=== Checkpoints ===\n");
        for (int i = 0; i < cps.size(); i++) {
            sb.append("§e").append(i + 1).append(". §f").append(cps.get(i).describe());
            if (i == cps.size() - 1) sb.append(" §6(finish)");
            sb.append('\n');
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
}
