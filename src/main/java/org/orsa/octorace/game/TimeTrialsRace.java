package org.orsa.octorace.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class TimeTrialsRace extends Race {

    public TimeTrialsRace(RaceManager manager, List<ServerPlayer> players) {
        super(manager, players);
    }

    @Override
    protected void announceNewRace() {
        broadcast(Component.literal("Starting time trials race.").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void sendCountdownStartMessage(RaceParticipant participant) {
        participant.player.sendSystemMessage(Component.literal("Move to start the timer!").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected void tickCountdown() {
        for (var participant : participants) {
            var input = participant.player.getLastClientInput();
            if (input.forward() || input.backward() || input.left() || input.right() || input.jump()) {
                start();
                return;
            }
        }
    }

    @Override
    protected void broadcastParticipantFinish(RaceParticipant participant) {}

    @Override
    protected void onAllPlayersFinished() {
        broadcast(Component.literal(" "));

        for (var finisher : finishers) {
            var timeLine = Component.empty();
            timeLine.append(Component.literal(finisher.displayName + " ").withStyle(ChatFormatting.WHITE));
            timeLine.append(Component.literal(String.format("(%.2fs)", finisher.finishTimeMillis / 1000.0)).withStyle(ChatFormatting.GRAY));
            broadcast(timeLine);
        }

        broadcast(Component.literal(" "));
    }

    @Override
    public void onParticipantRestart(RaceParticipant participant) {
        manager.removeTemporaryEffects(participant.player, true);
        startCountdown();
    }
}
