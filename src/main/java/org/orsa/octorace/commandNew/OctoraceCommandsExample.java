package org.orsa.octorace.commandNew;

// To use Admiral, add to build.gradle:
//
// repositories {
//     maven { url = "https://api.modrinth.com/maven" }
// }
// dependencies {
//     include(modImplementation("maven.modrinth:admiral:0.4.10+1.21.11+fabric"))
// }
//
// Then replace OctoraceCommand.register(dispatcher) in Octorace.java with:
//
// MinecraftAdmiral.builder(dispatcher, registryAccess)
//     .addCommandClasses(OctoraceCommandsExample.class)
//     .build();

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Min;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.game.Race;
import org.orsa.octorace.game.RaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Command("octorace")
@RequiresPermissionLevel(2)
public class OctoraceCommandsExample {

    @Command("start")
    public int start(CommandContext<CommandSourceStack> ctx) {
        List<ServerPlayer> all = new ArrayList<>(ctx.getSource().getServer().getPlayerList().getPlayers());
        Octorace.RACE_MANAGER.startRace(all);
        final int count = all.size();
        ctx.getSource().sendSuccess(() -> Component.literal("§aRace starting with " + count + " player(s)."), true);
        return 1;
    }

    @Command({"start", "timeTrials"})
    public int startTimeTrials(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Octorace.RACE_MANAGER.startTimeTrials(player);
        return 1;
    }

    @Command("stop")
    public int stop(CommandContext<CommandSourceStack> ctx) {
        for (Race race : Octorace.RACE_MANAGER.ongoingRaces) {
            race.endRace(Race.RaceEndReason.STOPPED_BY_ADMIN);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§7Races stopped."), true);
        return 1;
    }

    @Command("status")
    public int status(CommandContext<CommandSourceStack> ctx) {
        RaceManager raceManager = Octorace.RACE_MANAGER;
        RaceConfig config = raceManager.getConfig();
        String message = "§6=== Octorace Status ===\n" +
                "§eCheckpoints: §f" + config.getCheckpointCount() + '\n' +
                "§eStart configured: §f" + (config.getStartPosition() != null) + '\n' +
                "§eDimension: §f" + (config.getDimensionId() == null ? "<unset>" : config.getDimensionId()) + '\n' +
                "§eReady to run: §f" + config.isReady();
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    @Command("setstart")
    public int setStart(CommandContext<CommandSourceStack> ctx, Optional<Vec3> position) throws CommandSyntaxException {
        if (position.isPresent()) {
            Identifier dimension = ctx.getSource().getLevel().dimension().identifier();
            applyStart(ctx, position.get(), ctx.getSource().getRotation().y, ctx.getSource().getRotation().x, dimension);
        } else {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            applyStart(ctx, player.position(), player.getYRot(), player.getXRot(), player.level().dimension().identifier());
        }
        return 1;
    }

    private void applyStart(CommandContext<CommandSourceStack> ctx, Vec3 position, float yaw, float pitch, Identifier dimension) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.setStartPosition(position, yaw);
        config.setDimensionId(dimension.toString());
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("§aStart set: §f%.2f, %.2f, %.2f §7(yaw %.1f, pitch %.1f) in §f%s",
                        position.x, position.y, position.z, yaw, pitch, dimension)
        ), true);
    }

    // NOTE: Optional<String> maps to a single-word arg, not greedy. Multi-word names won't work here.
    @Command("addcheckpoint")
    public int addCheckpoint(CommandContext<CommandSourceStack> ctx,
                             @Name("from") BlockPos from,
                             @Name("to") BlockPos to,
                             Optional<String> name) {
//        double minX = Math.min(from.getX(), to.getX());
//        double minY = Math.min(from.getY(), to.getY());
//        double minZ = Math.min(from.getZ(), to.getZ());
//        double maxX = Math.max(from.getX(), to.getX()) + 1.0;
//        double maxY = Math.max(from.getY(), to.getY()) + 1.0;
//        double maxZ = Math.max(from.getZ(), to.getZ()) + 1.0;
//        String dimensionId = ctx.getSource().getLevel().dimension().identifier().toString();
//        Checkpoint checkpoint = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name.orElse(null), dimensionId);
//        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
//        config.addCheckpoint(checkpoint);
//        config.save();
//        final int index = config.getCheckpointCount();
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                "§aAdded checkpoint #" + index + ": " + checkpoint.describe()), true);
        return 1;
    }

    @Command("removecheckpoint")
    public int removeCheckpoint(CommandContext<CommandSourceStack> ctx, @Min("1") int index) {
        int adjustedIndex = index - 1;
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        if (!config.removeCheckpoint(adjustedIndex)) {
            ctx.getSource().sendFailure(Component.literal("§cInvalid checkpoint index."));
            return 0;
        }
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aRemoved checkpoint #" + index), true);
        return 1;
    }

    @Command("clearcheckpoints")
    public int clearCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        config.clearCheckpoints();
        config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aAll checkpoints cleared."), true);
        return 1;
    }

    @Command("listcheckpoints")
    public int listCheckpoints(CommandContext<CommandSourceStack> ctx) {
        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        List<Checkpoint> checkpoints = config.checkpoints;
        if (checkpoints.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No checkpoints set."), false);
            return 1;
        }
        StringBuilder builder = new StringBuilder("§6=== Checkpoints ===\n");
        for (int i = 0; i < checkpoints.size(); i++) {
            builder.append("§e").append(i + 1).append(". §f").append(checkpoints.get(i).describe());
            if (i == checkpoints.size() - 1) {
                builder.append(" §6(finish)");
            }
            builder.append('\n');
        }
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString().trim()), false);
        return 1;
    }

    @Command("save")
    public int save(CommandContext<CommandSourceStack> ctx) {
        Octorace.RACE_MANAGER.getConfig().save();
        ctx.getSource().sendSuccess(() -> Component.literal("§aConfig saved."), true);
        return 1;
    }

    @Command("wand")
    public int giveWand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
//        ServerPlayer player = ctx.getSource().getPlayerOrException();
//        ItemStack wand = WandManager.createWand();
//        if (!player.getInventory().add(wand)) {
//            player.drop(wand, false);
//        }
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                "§aOctorace Wand given. §7Left-click to set corner 1, right-click to set corner 2."), false);
        return 1;
    }

    // NOTE: same single-word limitation as addcheckpoint above.
    @Command({"wand", "confirm"})
    public int confirmWand(CommandContext<CommandSourceStack> ctx, Optional<String> name) throws CommandSyntaxException {
//        ServerPlayer player = ctx.getSource().getPlayerOrException();
//        UUID uuid = player.getUUID();
//        if (!WandManager.hasBothPositions(uuid)) {
//            ctx.getSource().sendFailure(Component.literal(
//                    "§cSet both corners first (left-click and right-click with the wand)."));
//            return 0;
//        }
//        BlockPos a = WandManager.getPos1(uuid);
//        BlockPos b = WandManager.getPos2(uuid);
//        double minX = Math.min(a.getX(), b.getX());
//        double minY = Math.min(a.getY(), b.getY());
//        double minZ = Math.min(a.getZ(), b.getZ());
//        double maxX = Math.max(a.getX(), b.getX()) + 1.0;
//        double maxY = Math.max(a.getY(), b.getY()) + 1.0;
//        double maxZ = Math.max(a.getZ(), b.getZ()) + 1.0;
//        Checkpoint checkpoint = new Checkpoint(minX, minY, minZ, maxX, maxY, maxZ, name.orElse(null), WandManager.getWandDimension(uuid));
//        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
//        config.addCheckpoint(checkpoint);
//        config.save();
//        WandManager.clear(uuid);
//        final int index = config.getCheckpointCount();
//        ctx.getSource().sendSuccess(() -> Component.literal(
//                "§aCheckpoint #" + index + " saved: " + checkpoint.describe()), true);
        return 1;
    }
}
