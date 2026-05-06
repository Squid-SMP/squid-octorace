package org.orsa.octorace.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RaceManager {
	public enum State { IDLE, COUNTDOWN, ACTIVE }

	private static final int COUNTDOWN_TICKS = 60; // 3s at 20 tps

	private static final String NBT_TAG_KEY = "OctoraceItem";

	private final MinecraftServer server;
	private final RaceConfig config;

	private State state = State.IDLE;
	private int countdownTicksRemaining = 0;
	private long raceStartTimeMillis = 0L;

	private final Map<UUID, Participant> participants = new HashMap<>();

	// Ordered finish list for placement
	private final List<UUID> finishers = new ArrayList<>();

	public RaceManager(MinecraftServer server, RaceConfig config) {
		this.server = server;
		this.config = config;
	}

	public RaceConfig getConfig() {
		return config;
	}

	public State getState() {
		return state;
	}

	public boolean isRaceRunning() {
		return state == State.ACTIVE || state == State.COUNTDOWN;
	}

	public boolean isParticipant(UUID uuid) {
		return participants.containsKey(uuid);
	}

	public String startRace(List<ServerPlayer> players) {
		if (isRaceRunning()) {
			return "A race is already running. Use /octorace stop to abort it.";
		}

		if (!config.isReady()) {
			return "Race is not configured. Set start position and at least one checkpoint first.";
		}

		if (players.isEmpty()) {
			return "No players to start the race with.";
		}

		Identifier dimRl = config.getDimensionIdentifier();
		ResourceKey<Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
		ServerLevel targetLevel = server.getLevel(dimKey);
		if (targetLevel == null) {
			return "Race dimension '" + dimRl + "' is not loaded.";
		}

		Vec3 start = config.getStartPosition();
		float yaw = config.getStartYaw();

		participants.clear();
		finishers.clear();

		for (ServerPlayer player : players) {
			teleportPlayer(player, targetLevel, start.x, start.y, start.z, yaw);
			player.setDeltaMovement(Vec3.ZERO);
			equipRaceGear(player);
			participants.put(player.getUUID(), new Participant(player.getUUID(), player.getName().getString()));
			player.sendSystemMessage(Component.literal("§eGet ready..."));
		}

		state = State.COUNTDOWN;
		countdownTicksRemaining = COUNTDOWN_TICKS;
		Octorace.LOGGER.info("Octorace countdown started with {} player(s).", players.size());
		return null;
	}

	// Abort the race and strip gear from all players
	public void stopRace(String reason) {
		if (!isRaceRunning()) {
			return;
		}

		broadcast("§cRace ended: " + reason);
		for (UUID uuid : participants.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(uuid);
			if (p != null) clearRaceGear(p);
		}

		participants.clear();
		finishers.clear();
		state = State.IDLE;
		countdownTicksRemaining = 0;
	}

	// Server tick: drive countdown and checkpoint detection
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

			for (UUID uuid : participants.keySet()) {
				ServerPlayer p = server.getPlayerList().getPlayer(uuid);

				if (p == null) {
					continue;
				}

				playSoundFor(p, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
			}
		}

		// Zero velocity each tick to hold players in place
		freezeParticipants();

		countdownTicksRemaining--;
		if (countdownTicksRemaining <= 0) {
			state = State.ACTIVE;
			raceStartTimeMillis = System.currentTimeMillis();

			broadcast("§a§lGO!");

			for (UUID uuid : participants.keySet()) {
				ServerPlayer p = server.getPlayerList().getPlayer(uuid);

				if (p == null) {
					continue;
				}

				playSoundFor(p, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.5f);
			}
		}
	}

	private void freezeParticipants() {
		for (UUID uuid : participants.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(uuid);
			if (p == null) continue;
			p.setDeltaMovement(0, 0, 0);
			p.hurtMarked = true; // forces velocity sync to client
		}
	}

	private void tickActive() {
		List<Checkpoint> checkpoints = config.getCheckpoints();

		// Snapshot keys so mutating finishers mid-loop is safe
		List<UUID> uuids = new ArrayList<>(participants.keySet());
		for (UUID uuid : uuids) {
			Participant data = participants.get(uuid);
			if (data == null || data.finished) continue;

			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player == null) continue; // disconnect handler cleans up

			int nextIdx = data.nextCheckpoint;
			if (nextIdx >= checkpoints.size()) continue; // all checkpoints done

			Checkpoint next = checkpoints.get(nextIdx);
			AABB box = next.toBox();
			if (box.intersects(player.getBoundingBox())) {
				onCheckpointReached(player, data, nextIdx, checkpoints.size());
			}
		}

		if (!participants.isEmpty() && participants.values().stream().allMatch(p -> p.finished)) {
			endRace();
		}
	}

	private void onCheckpointReached(ServerPlayer player, Participant data, int idx, int total) {
		data.nextCheckpoint = idx + 1;
		boolean isFinal = (idx + 1) == total;

		if (isFinal) {
			data.finished = true;
			data.finishTimeMillis = System.currentTimeMillis() - raceStartTimeMillis;
			finishers.add(data.uuid);
			int place = finishers.size();
			String suffix = ordinalSuffix(place);
			double seconds = data.finishTimeMillis / 1000.0;

			broadcast(String.format("§6§l%s§r§6 finished in §e%d%s§6 place! §7(%.2fs)",
					player.getName().getString(), place, suffix, seconds));

			clearRaceGear(player);
			playSoundFor(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
		}
		else {
			player.sendSystemMessage(Component.literal(
					String.format("§aCheckpoint §e%d§a/§e%d§a passed!", idx + 1, total)
			));
			playSoundFor(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
		}
	}

	private void endRace() {
		broadcast("§6§l=== Race Complete ===");
		for (int i = 0; i < finishers.size(); i++) {
			Participant data = participants.get(finishers.get(i));
			if (data == null) continue;
			broadcast(String.format("§e%d. §f%s §7(%.2fs)",
					i + 1, data.displayName, data.finishTimeMillis / 1000.0));
		}
		state = State.IDLE;
		participants.clear();
		finishers.clear();
	}

	private void broadcast(String msg) {
		Component c = Component.literal(msg);
		for (UUID uuid : participants.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(uuid);
			if (p != null) p.sendSystemMessage(c);
		}
	}

	// Disconnect handler: remove player from the race
	public void onParticipantDisconnect(ServerPlayer player) {
		Participant p = participants.remove(player.getUUID());
		if (p != null) {
			broadcast("§7" + player.getName().getString() + " left the race.");
		}
	}

	// Death handler: flag the player for teleport on respawn
	public void onParticipantDied(ServerPlayer player) {
		Participant data = participants.get(player.getUUID());
		if (data == null || data.finished) return;
		data.needsRespawnTeleport = true;
	}

	// Respawn handler: re-teleport and re-equip a player who died mid-race
	public void onParticipantRespawn(ServerPlayer player) {
		Participant data = participants.get(player.getUUID());
		if (data == null || !data.needsRespawnTeleport) return;
		data.needsRespawnTeleport = false;

		Identifier dimRl = config.getDimensionIdentifier();
		ResourceKey<Level> dimKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimRl);
		ServerLevel targetLevel = server.getLevel(dimKey);
		if (targetLevel == null) return;

		Vec3 start = config.getStartPosition();
		teleportPlayer(player, targetLevel, start.x, start.y, start.z, config.getStartYaw());
		equipRaceGear(player);
		player.sendSystemMessage(Component.literal("§eYou died! Back to the start."));
	}

	// --- helpers ---

	private static void teleportPlayer(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw) {
		Set<Relative> noRelative = Collections.emptySet();
		player.teleportTo(level, x, y, z, noRelative, yaw, 0, true);
	}

	private static void playSoundFor(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		var level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.MASTER, volume, pitch);
	}

	private static void equipRaceGear(ServerPlayer player) {
		ItemStack elytra = new ItemStack(Items.ELYTRA);

		elytra.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);

		CompoundTag tag = new CompoundTag();
		tag.putBoolean(NBT_TAG_KEY, true);
		elytra.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

		ItemStack existing = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!existing.isEmpty()) {
			if (!player.getInventory().add(existing.copy())) {
				player.drop(existing.copy(), false);
			}
		}

		player.setItemSlot(EquipmentSlot.CHEST, elytra);
	}

	private static void clearRaceGear(ServerPlayer player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if (!chest.isEmpty() && chest.is(Items.ELYTRA)) {
			CustomData customData = chest.get(DataComponents.CUSTOM_DATA);
			if (customData != null && customData.copyTag().getBoolean(NBT_TAG_KEY).orElse(false)) {
				player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			}
		}
	}

	private static String ordinalSuffix(int n) {
		if (n % 100 >= 11 && n % 100 <= 13) return "th";
		return switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
	}

	// --- per-player state ---

	private static class Participant {
		final UUID uuid;
		final String displayName;
		int nextCheckpoint = 0;
		boolean finished = false;
		long finishTimeMillis = 0L;
		boolean needsRespawnTeleport = false;

		Participant(UUID uuid, String displayName) {
			this.uuid = uuid;
			this.displayName = displayName;
		}
	}
}
