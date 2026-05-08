package org.orsa.octorace.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static net.minecraft.commands.Commands.literal;

public class OctoraceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("octorace")
                .requires(src -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(src))
                .then(OctoraceStartCommand.build())
                .then(OctoraceStopCommand.build())
                .then(OctoraceStatusCommand.build())
                .then(OctoraceSetStartCommand.build())
                .then(OctoraceAddCheckpointCommand.build())
                .then(OctoraceRemoveCheckpointCommand.build())
                .then(OctoraceClearCheckpointsCommand.build())
                .then(OctoraceListCheckpointsCommand.build())
                .then(OctoraceSaveCommand.build())
                .then(OctoraceWandCommand.build())
        );
    }
}
