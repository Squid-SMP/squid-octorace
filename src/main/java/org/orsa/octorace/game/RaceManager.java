package org.orsa.octorace.game;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.item.OctoraceTrident;

import java.util.*;

import static org.orsa.octorace.Octorace.*;

public class RaceManager {

	private final RaceConfig config;

	public Map<UUID, RaceParticipant> allParticipants = new HashMap<>();
	public List<Race> ongoingRaces = new ArrayList<>();

	public RaceManagerStorage raceManagerStorage;

	public ServerLevel dimension;

	public RaceManager(RaceConfig config) {
		this.config = config;

		dimension = SERVER.getLevel(config.getDimensionKey());

		raceManagerStorage = RaceManagerStorage.get(Octorace.SERVER);
	}

	public RaceConfig getConfig() {
		return config;
	}

	public Race startRace(List<ServerPlayer> players, boolean global) {
		if (players.isEmpty()) {
			return null;
		}

		vetPlayers(players, false);

		var race = new Race(this, players);
		onAnyRaceStarted(race);

		race.global = global;

		return race;
	}

	public TimeTrialsRace startTimeTrials(ServerPlayer player) {
		var players = List.of(player);
		players = vetPlayers(players, true);

		if (players.isEmpty()) {
			return null;
		}

		var race = new TimeTrialsRace(this, players);
		onAnyRaceStarted(race);

		return race;
	}

	private List<ServerPlayer> vetPlayers(List<ServerPlayer> players, boolean fromTimeTrials) {
		var playersCopy = new ArrayList<>(players);
		for (var player : players) {
			var participant = allParticipants.get(player.getUUID());

			if (participant == null) {
				continue;
			}

			if (participant.race instanceof TimeTrialsRace && fromTimeTrials) {
				playersCopy.remove(player);
				continue;
			}

			participant.forceQuit();
		}

		return playersCopy;
	}

	private void onAnyRaceStarted(Race race) {
		ongoingRaces.add(race);

		var participants = new ArrayList<>(race.participants);
		for (var participant : participants) {
			if (allParticipants.containsKey(participant.uuid)) {
				participant.forceQuit();
			}

			allParticipants.put(participant.uuid, participant);
		}
	}

	public void tick() {
		for (var race : List.copyOf(ongoingRaces)) {
			race.tick();
		}

		for (var player : SERVER.getPlayerList().getPlayers()) {
			if (player.getTags().contains("octorace") && !allParticipants.containsKey(player.getUUID())) {
				clearPlayer(player, false);
			}
		}
	}

	public void onRaceEnded(Race race) {
		ongoingRaces.remove(race);

		for (var participant : race.participants) {
			allParticipants.remove(participant.uuid);
			clearPlayer(participant.player);
		}
	}

	public void onPlayerDisconnect(ServerPlayer player) {
		var uuid = player.getUUID();
		if (!allParticipants.containsKey(uuid)) {
			return;
		}

		var participant = allParticipants.get(uuid);

		var race = participant.race;
		race.onParticipantDisconnect(participant);
	}

	public RaceParticipant getParticipant(ServerPlayer player) {
		var uuid = player.getUUID();
		if (!allParticipants.containsKey(uuid)) {
			return null;
		}

		var participant = allParticipants.get(uuid);
		return participant;
	}

	public void addTimeTrialsResult(RaceParticipant participant) {
		var uuid = participant.uuid;
		var time = participant.finishTimeMillis;
		var displayName = participant.displayName;

		raceManagerStorage.addTimeTrialsEntry(participant, time, displayName);
	}

	public void clearPlayer(ServerPlayer player) {
		clearPlayer(player, true);
	}

	public void setCollisionEnabled(ServerPlayer player, boolean enabled) {
		try {
			SERVER.getCommands().performPrefixedCommand(
				SERVER.createCommandSourceStack(),
				"tab setcollision " + player.getName().getString() + " " + enabled
			);
		} catch (Throwable ignored) {}
	}

	public void clearPlayer(ServerPlayer player, boolean teleport) {
		setCollisionEnabled(player, true);

		removeTemporaryEffects(player, true);

		if (teleport) {
			var lobbyPos = config.getLobbyPosition();
			var lobbyYaw = config.getLobbyYaw();
			player.teleportTo(dimension, lobbyPos.x, lobbyPos.y, lobbyPos.z, new HashSet<>(), lobbyYaw, 0, true);
		}

		player.setInvulnerable(false);
		removeSoulSpeedModifier(player);
		player.removeTag("octorace");
	}

	public void removeTemporaryEffects(ServerPlayer player, boolean respawnAndQuit) {
		OctoraceTrident.unmoveable.removeActivePlayer(player);

		if (respawnAndQuit) {
			RESPAWN_ITEM.unmoveable.removeActivePlayer(player);
			QUIT_ITEM.unmoveable.removeActivePlayer(player);
		}

		player.removeAllEffects();

		var waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
		if (waterAttr != null) {
			waterAttr.removeModifier(Identifier.parse("octorace:depth_strider"));
		}
	}

	// --- helpers ---

	private void removeSoulSpeedModifier(ServerPlayer player) {
		var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr == null) {
			return;
		}
		var modifierId = Identifier.parse("minecraft:enchantment.soul_speed");
		attr.removeModifier(modifierId);
	}

	public String ordinalSuffix(int n) {
		if (n % 100 >= 11 && n % 100 <= 13) {
			return "th";
		}
		return switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
	}
}
