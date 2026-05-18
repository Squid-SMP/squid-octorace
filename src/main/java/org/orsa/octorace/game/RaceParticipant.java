package org.orsa.octorace.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.item.OctoraceTrident;

import java.util.HashSet;
import java.util.UUID;

import static org.orsa.octorace.Octorace.LOGGER;
import static org.orsa.octorace.Octorace.playSoundFor;

public class RaceParticipant {
    public final Race race;
    public final ServerPlayer player;
    public final UUID uuid;
    public final String displayName;

    public final int checkpointsCount;
    public int nextCheckpointIdx = 0;
    public Checkpoint nextCheckpoint;
    public Checkpoint lastCheckpoint;

    public boolean finished = false;
    public long finishTimeMillis = 0L;
    public int finishPlace;
    public boolean dnf = false;

    public long lastCheckpointTimeMillis = 0L;

    public Vec3 respawnPos;
    public float respawnYaw;

    private Boolean hasClickedQuitOnce = false;
    private Boolean hasClickedRestartOnce = false;

    RaceParticipant(Race race, ServerPlayer player) {
        this.race = race;
        this.player = player;
        checkpointsCount = race.getCheckpointCount();

        respawnPos = race.startPos;
        respawnYaw = race.startYaw;

        uuid = player.getUUID();
        displayName = player.getName().getString();
    }

    public void tickCountdown() {
        player.setDeltaMovement(0, 0, 0);
        player.teleportTo(respawnPos.x, respawnPos.y, respawnPos.z);
        player.hurtMarked = true;
    }

    public void tickActive() {
        if (finished) {
            return;
        }

        if (player == null) {
            return;
        }

        if (nextCheckpointIdx >= checkpointsCount) {
            return;
        }

        nextCheckpoint = race.getCheckpoint(nextCheckpointIdx);
        if (nextCheckpoint.playerIntersects(player)) {
            nextCheckpoint.onPlayerCrossed(player);
            onCheckpointReached();
        }
    }

    private void onCheckpointReached() {
        lastCheckpoint = nextCheckpoint;
        nextCheckpointIdx++;
        boolean isFinal = (nextCheckpointIdx == checkpointsCount);

        respawnPos = lastCheckpoint.center();
        respawnYaw = Math.round(player.getYRot() / 90f) * 90f;

        long now = System.currentTimeMillis();
        long splitMillis = (lastCheckpointTimeMillis == 0L) ? (now - race.raceStartTimeMillis) : (now - lastCheckpointTimeMillis);
        long totalMillis = now - race.raceStartTimeMillis;
        lastCheckpointTimeMillis = now;

        var checkpointMessage = Component.empty();
        checkpointMessage.append(Component.literal("Checkpoint ").withStyle(ChatFormatting.GREEN));
        checkpointMessage.append(Component.literal(nextCheckpointIdx + "/" + checkpointsCount).withStyle(ChatFormatting.YELLOW));
        checkpointMessage.append(Component.literal(String.format("  +%.2fs", splitMillis / 1000.0)).withStyle(ChatFormatting.WHITE));
        checkpointMessage.append(Component.literal(String.format("  (%.2fs)", totalMillis / 1000.0)).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(checkpointMessage);
        playSoundFor(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);

        if (isFinal) {
            finished = true;
            finishTimeMillis = System.currentTimeMillis() - race.raceStartTimeMillis;

            finishPlace = race.finishers.size() + 1;

            race.onParticipantFinished(this);
        }
    }

    public void respawnAtLastCheckpoint() {
        if (finished) {
            return;
        }

        player.teleportTo(race.dimension, respawnPos.x, respawnPos.y, respawnPos.z, new HashSet<>(), respawnYaw, 0, true);

        var message = Component.literal("Respawned at last checkpoint.").withStyle(ChatFormatting.GREEN);
        player.sendSystemMessage(message);

        playSoundFor(player, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 1.3f);
    }

    public void quit() {
        if (!hasClickedQuitOnce) {
            var message = Component.literal("Press again to quit the race.").withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(message);
            hasClickedQuitOnce = true;
            return;
        }

        forceQuit();
    }

    public void restart() {
        if (!hasClickedRestartOnce) {
            var message = Component.literal("Press again to restart from the beginning.").withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(message);
            hasClickedRestartOnce = true;
            return;
        }

        forceRestart();
    }

    public void forceQuit() {
        race.onParticipantDisconnect(this);
    }

    public void forceRestart() {
        var message = Component.literal("Restarted from the beginning.").withStyle(ChatFormatting.GRAY);
        player.sendSystemMessage(message);

        hasClickedQuitOnce = false;
        hasClickedRestartOnce = false;
        nextCheckpointIdx = 0;
        lastCheckpointTimeMillis = 0L;

        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        race.onParticipantRestart(this);
    }
}
