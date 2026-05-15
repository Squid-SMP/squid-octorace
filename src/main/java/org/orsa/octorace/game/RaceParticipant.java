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

    public Vec3 respawnPos;
    public float respawnYaw;

    private Boolean hasClickedQuitOnce = false;

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

        if (isFinal) {
            finished = true;
            finishTimeMillis = System.currentTimeMillis() - race.raceStartTimeMillis;

            finishPlace = race.finishers.size() + 1;

            race.onParticipantFinished(this);

            playSoundFor(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        else {
            player.sendSystemMessage(Component.literal(String.format("§aCheckpoint §e%d§a/§e%d§a passed!", nextCheckpointIdx, checkpointsCount)));
            playSoundFor(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
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
            var message = Component.literal("Press again to confirm quitting the race.").withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(message);
            hasClickedQuitOnce = true;
            return;
        }

        race.onParticipantDisconnect(this);
    }
}
