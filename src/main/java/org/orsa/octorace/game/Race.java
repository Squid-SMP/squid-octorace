package org.orsa.octorace.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.item.OctoraceTrident;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.orsa.octorace.Octorace.*;

public class Race {
    private static final int COUNTDOWN_TICKS = 60; // 3s at 20 tps

    public enum Type { VERSUS, TIME_TRIALS }
    public enum State { COUNTDOWN, ACTIVE, ENDING }
    public enum RaceEndReason { ALL_PLAYERS_FINISHED, STOPPED_BY_ADMIN, ALL_PLAYERS_DISQUALIFIED}

    protected final RaceManager manager;
    public final RaceConfig config;
    private final List<Checkpoint> checkpoints;

    public Type type;

    public ServerLevel dimension;
    public Vec3 startPos;
    public float startYaw;

    private static final int END_DELAY_TICKS = 60; // 3s at 20 tps

    private State state = State.COUNTDOWN;
    private int countdownTicksRemaining;
    private int endingTicksRemaining;
    public long raceStartTimeMillis = 0L;

    public List<RaceParticipant> participants;
    public List<RaceParticipant> finishers;
    public List<RaceParticipant> disqualifieds;

    public boolean global;

    public Race(RaceManager manager, List<ServerPlayer> players) {
        this.manager = manager;
        this.config = manager.getConfig();
        this.checkpoints = config.checkpoints;

        dimension = manager.dimension;
        startPos = config.getStartPosition();
        startYaw = config.getStartYaw();

        participants = new ArrayList<>();
        finishers = new ArrayList<>();
        disqualifieds = new ArrayList<>();

        for (var player : players) {
            var participant = new RaceParticipant(this, player);
            participants.add(participant);
        }

        announceNewRace();

        startCountdown();
    }

    protected void announceNewRace() {
        broadcast(Component.literal("Starting versus race.").withStyle(ChatFormatting.GRAY));
    }

    protected boolean aborted = false;

    protected void startCountdown() {
        state = State.COUNTDOWN;
        countdownTicksRemaining = COUNTDOWN_TICKS;

        boolean anyFailed = false;
        for (var participant : participants) {
            sendCountdownStartMessage(participant);
            if (!teleportToStart(participant)) {
                anyFailed = true;
            }
        }

        if (anyFailed) {
            abortStart();
        }
    }

    private void abortStart() {
        aborted = true;
        broadcast(Component.literal("Race cancelled: a player could not be teleported.").withStyle(ChatFormatting.RED));
        for (var participant : participants) {
            manager.clearPlayer(participant.player);
        }
    }

    protected void sendCountdownStartMessage(RaceParticipant participant) {
        participant.player.sendSystemMessage(Component.literal("Get ready...").withStyle(ChatFormatting.YELLOW));
    }

    protected boolean teleportToStart(RaceParticipant participant) {
        var player = participant.player;

        manager.setCollisionEnabled(player, false);
        player.setInvulnerable(true);

        player.removeAllEffects();

        var effect = new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false);
        player.addEffect(effect);

        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);

        player.addTag("octorace");

        player.teleportTo(dimension, startPos.x, startPos.y, startPos.z, new HashSet<>(), startYaw, 0, true);
        player.setDeltaMovement(Vec3.ZERO);

        if (!player.level().dimension().equals(dimension.dimension())) {
            return false;
        }

        player.getInventory().clearContent();
        return true;
    }

    protected void start() {
        state = State.ACTIVE;
        raceStartTimeMillis = System.currentTimeMillis();

        broadcast(Component.literal("GO!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        for (var participant : participants) {
            var player = participant.player;

            playSoundFor(participant.player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
            RESPAWN_ITEM.unmoveable.addActivePlayer(player);
            QUIT_ITEM.unmoveable.addActivePlayer(player);

            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    public void tick() {
        if (state == State.COUNTDOWN) {
            tickCountdown();
        }
        else if (state == State.ACTIVE) {
            tickActive();
        }
        else if (state == State.ENDING) {
            tickEnding();
        }
    }

    protected void tickCountdown() {
        // Show the countdown number once per second
        int secondsLeft = (countdownTicksRemaining + 19) / 20;
        int prevSecondsLeft = (countdownTicksRemaining + 1 + 19) / 20;

        if (secondsLeft != prevSecondsLeft && secondsLeft > 0) {
            broadcast(Component.literal(secondsLeft + "...").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

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

    protected void tickActive() {
        for (var participant : participants) {
            participant.tickActive();
        }
    }

    private void tickEnding() {
        int secondsLeft = (endingTicksRemaining + 19) / 20;
        int previousSecondsLeft = (endingTicksRemaining + 1 + 19) / 20;

        if (secondsLeft != previousSecondsLeft && secondsLeft > 0) {
            var lobbyMessage = Component.empty();
            lobbyMessage.append(Component.literal("Returning to lobby in ").withStyle(ChatFormatting.GRAY));
            lobbyMessage.append(Component.literal(String.valueOf(secondsLeft)).withStyle(ChatFormatting.YELLOW));
            lobbyMessage.append(Component.literal("...").withStyle(ChatFormatting.GRAY));
            broadcast(lobbyMessage);
        }

        endingTicksRemaining--;

        if (endingTicksRemaining <= 0) {
            manager.onRaceEnded(this);
        }
    }

    public Checkpoint getCheckpoint(int idx) {
        return checkpoints.get(idx);
    }

    public int getCheckpointCount() {
        return checkpoints.size();
    }

    protected void broadcast(Component message) {
        for (var participant : participants) {
            participant.player.sendSystemMessage(message);
        }
    }

    public void onParticipantFinished(RaceParticipant participant) {
        finishers.add(participant);

        broadcastParticipantFinish(participant);

        manager.addTimeTrialsResult(participant);

        RESPAWN_ITEM.unmoveable.removeActivePlayer(participant.player);
        OctoraceTrident.unmoveable.removeActivePlayer(participant.player);

        playSoundFor(participant.player, SoundEvents.ARROW_HIT_PLAYER, 1.0f, 1.0f);

        checkRaceEnd();
    }

    protected void broadcastParticipantFinish(RaceParticipant participant) {
        String suffix = manager.ordinalSuffix(participant.finishPlace);
        double seconds = participant.finishTimeMillis / 1000.0;

        var finishedMessage = Component.empty();
        finishedMessage.append(Component.literal(participant.displayName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        finishedMessage.append(Component.literal(String.format(" finished in %d%s place! ", participant.finishPlace, suffix)).withStyle(ChatFormatting.GOLD));
        finishedMessage.append(Component.literal(String.format("(%.2fs)", seconds)).withStyle(ChatFormatting.GRAY));
        broadcast(finishedMessage);
    }

    private Boolean checkRaceEnd() {
        if (finishers.size() >= participants.size()) {
            endRace(RaceEndReason.ALL_PLAYERS_FINISHED);
            return true;
        }

        if (participants.isEmpty()) {
            endRace(RaceEndReason.ALL_PLAYERS_DISQUALIFIED);
            return true;
        }

        return false;
    }

    public void endRace(RaceEndReason reason) {
        switch (reason) {
            case ALL_PLAYERS_FINISHED:
                onAllPlayersFinished();
                break;

            default:
        }

        state = State.ENDING;
        endingTicksRemaining = END_DELAY_TICKS;
    }

    protected void onAllPlayersFinished() {
        broadcast(Component.literal(" "));

        broadcast(Component.literal("=== Race Complete ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        for (var finisher : finishers) {
            var finisherLine = Component.empty();
            finisherLine.append(Component.literal(finisher.finishPlace + ". ").withStyle(ChatFormatting.YELLOW));
            finisherLine.append(Component.literal(finisher.displayName + " ").withStyle(ChatFormatting.WHITE));
            finisherLine.append(Component.literal(String.format("(%.2fs)", finisher.finishTimeMillis / 1000.0)).withStyle(ChatFormatting.GRAY));
            broadcast(finisherLine);
        }

        for (var disqualified : disqualifieds) {
            var dnfLine = Component.empty();
            dnfLine.append(Component.literal("-. ").withStyle(ChatFormatting.YELLOW));
            dnfLine.append(Component.literal(disqualified.displayName + " ").withStyle(ChatFormatting.WHITE));
            dnfLine.append(Component.literal("(DNF)").withStyle(ChatFormatting.GRAY));
            broadcast(dnfLine);
        }

        broadcast(Component.literal(" "));
    }

    public void onParticipantDisconnect(RaceParticipant participant) {
        var playerName = participant.player.getName().getString();
        broadcast(Component.literal(playerName + " left the race.").withStyle(ChatFormatting.GRAY));

        disqualify(participant);
    }

    public void disqualify(RaceParticipant participant) {
        disqualify(participant, true);
    }

    public void disqualify(RaceParticipant participant, boolean teleport) {
        if (!participant.finished) {
            removeParticipant(participant);
            finishers.remove(participant);
            disqualifieds.add(participant);
            participant.dnf = true;
        }

        manager.clearPlayer(participant.player, teleport);

        checkRaceEnd();
    }

    private void removeParticipant(RaceParticipant participant) {
        participants.remove(participant);
        manager.allParticipants.remove(participant.uuid);
    }

    public void end(boolean teleport) {
        for (var participant : new ArrayList<>(participants)) {
            if (!participant.finished) {
                disqualify(participant, teleport);
            }
        }
    }

    public void onParticipantRestart(RaceParticipant participant) {
        manager.removeTemporaryEffects(participant.player, false);
        teleportToStart(participant);
    }
}
