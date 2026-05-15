package org.orsa.octorace.game;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class TimeTrialsRace extends Race {

    public TimeTrialsRace(RaceManager manager, List<ServerPlayer> players) {
        super(manager, players);
    }

    @Override
    protected void onAllPlayersFinished() {
        broadcast(" ");

        broadcast("§6§l=== Time Trials Complete ===");

        for (var finisher : finishers) {
            broadcast(String.format("§f%s §7(%.2fs)", finisher.displayName, finisher.finishTimeMillis / 1000.0));
        }

        broadcast(" ");
    }
}
