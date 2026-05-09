package org.orsa.octorace.game;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.orsa.octorace.Octorace.SERVER;
import static org.orsa.octorace.Octorace.playSoundFor;

public class Race {
    private static final int COUNTDOWN_TICKS = 60; // 3s at 20 tps

    public enum Type { VERSUS, TIME_TRIALS }
    public enum State { COUNTDOWN, ACTIVE }
    public enum RaceEndReason { ALL_PLAYERS_FINISHED, STOPPED_BY_ADMIN, ALL_PLAYERS_DISCONNECTED }

    private final RaceManager manager;
    private final RaceConfig config;
    private final List<Checkpoint> checkpoints;

    public Type type;

    public ServerLevel dimension;
    public Vec3 startPos;
    public float startYaw;

    private State state = State.COUNTDOWN;
    private int countdownTicksRemaining;
    public long raceStartTimeMillis = 0L;

    public List<RaceParticipant> participants;
    public List<RaceParticipant> finishers;

    public Race(RaceManager manager, List<ServerPlayer> players) {
        this.manager = manager;
        this.config = manager.getConfig();
        this.checkpoints = config.getCheckpoints();

        dimension = SERVER.getLevel(config.getDimensionKey());
        startPos = config.getStartPosition();
        startYaw = config.getStartYaw();

        participants = new ArrayList<>();
        finishers = new ArrayList<>();

        for (var player : players) {
            var participant = new RaceParticipant(this, player);
            participants.add(participant);
        }

        startCountdown();
    }

    private void startCountdown() {
        countdownTicksRemaining = COUNTDOWN_TICKS;

        Octorace.LOGGER.info("Octorace countdown started with {} player(s).", participants.size());

        for (var participant : participants) {
            teleportToStart(participant);
        }
    }

    private void teleportToStart(RaceParticipant participant) {
        var player = participant.player;
        player.teleportTo(dimension, startPos.x, startPos.y, startPos.z, new HashSet<>(), startYaw, 0, true);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("§eGet ready..."));
    }

    private void start() {
        state = State.ACTIVE;
        raceStartTimeMillis = System.currentTimeMillis();

        broadcast("§a§lGO!");

        for (var participant : participants) {
            playSoundFor(participant.player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
        }
    }

    public void tick() {
        if (state == State.COUNTDOWN) {
            tickCountdown();
        }
        else if (state == State.ACTIVE) {
            tickActive();
        }
    }

    private void tickCountdown() {
        // Show the countdown number once per second
        int secondsLeft = (countdownTicksRemaining + 19) / 20;
        int prevSecondsLeft = (countdownTicksRemaining + 1 + 19) / 20;

        if (secondsLeft != prevSecondsLeft && secondsLeft > 0) {
            broadcast("§e§l" + secondsLeft + "...");

            for (var participant : participants) {
                playSoundFor(participant.player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
            }
        }

        for (var participant : participants) {
            participant.tickCountdown();
        }

        countdownTicksRemaining--;
        if (countdownTicksRemaining <= 0) {
            start();
        }
    }

    private void tickActive() {
        for (var participant : participants) {
            participant.tickActive();
        }
    }

    public Checkpoint getCheckpoint(int idx) {
        return checkpoints.get(idx);
    }

    public int getCheckpointCount() {
        return checkpoints.size();
    }

    protected void broadcast(String msg) {
        Component c = Component.literal(msg);
        for (var participant : participants) {
            participant.player.sendSystemMessage(c);
        }
    }

    public void onParticipantFinished(RaceParticipant participant) {
        finishers.add(participant);

        String suffix = manager.ordinalSuffix(participant.finishPlace);
        double seconds = participant.finishTimeMillis / 1000.0;

        var message = String.format("§6§l%s§r§6 finished in §e%d%s§6 place! §7(%.2fs)", participant.displayName, participant.finishPlace, suffix, seconds);
        broadcast(message);

        manager.addTimeTrialsResult(participant);

        if (finishers.size() >= participants.size()) {
            endRace(RaceEndReason.ALL_PLAYERS_FINISHED);
        }
    }

    public void endRace(RaceEndReason reason) {
        switch (reason) {
            case ALL_PLAYERS_FINISHED:
                onAllPlayersFinished();
                break;

            default:
        }

        manager.onRaceEnded(this);
    }

    protected void onAllPlayersFinished() {
        broadcast("§6§l=== Race Complete ===");

        for (var finisher : finishers) {
            broadcast(String.format("§e%d. §f%s §7(%.2fs)", finisher.finishPlace, finisher.displayName, finisher.finishTimeMillis / 1000.0));
        }
    }

    public void onParticipantDisconnect(RaceParticipant participant) {
        broadcast("§7" + participant.player.getName().getString() + " left the race.");
        participants.remove(participant);
        finishers.remove(participant);

        if (participants.isEmpty()) {
            endRace(RaceEndReason.ALL_PLAYERS_DISCONNECTED);
        }
    }

    public void onParticipantRespawn(RaceParticipant participant) {
        if (!participant.needsRespawnTeleport) {
            return;
        }

        participant.needsRespawnTeleport = false;

        var respawnPos = participant.respawnPos;
        participant.player.teleportTo(dimension, respawnPos.x, respawnPos.y, respawnPos.z, new HashSet<>(), participant.respawnYaw, 0, true);

        participant.player.sendSystemMessage(Component.literal("§eYou died! Back to the start."));
    }
}
