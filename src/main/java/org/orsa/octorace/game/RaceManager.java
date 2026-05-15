package org.orsa.octorace.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	public Race startRace(List<ServerPlayer> players) {
		if (players.isEmpty()) {
			return null;
		}

		var race = new Race(this, players);
		onAnyRaceStarted(race);
		return race;
	}

	public TimeTrialsRace startTimeTrials(ServerPlayer player) {
		var race = new TimeTrialsRace(this, List.of(player));
		onAnyRaceStarted(race);
		return race;
	}

	private void onAnyRaceStarted(Race race) {
		ongoingRaces.add(race);

		for (var participant : race.participants) {
			allParticipants.put(participant.uuid, participant);
		}
	}

	public void tick() {
		for (var race : List.copyOf(ongoingRaces)) {
			race.tick();
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

		raceManagerStorage.addTimeTrialsEntry(uuid, time, displayName);
	}

	public void clearPlayer(ServerPlayer player) {
		var lobbyPos = config.getLobbyPosition();
		var lobbyYaw = config.getLobbyYaw();

		RESPAWN_ITEM.unmoveable.removeActivePlayer(player);
		QUIT_ITEM.unmoveable.removeActivePlayer(player);
		OctoraceTrident.unmoveable.removeActivePlayer(player);

		player.teleportTo(dimension, lobbyPos.x, lobbyPos.y, lobbyPos.z, new HashSet<>(), lobbyYaw, 0, true);
		player.getInventory().clearContent();
		player.setInvulnerable(false);
	}

	// --- helpers ---

	public String ordinalSuffix(int n) {
		if (n % 100 >= 11 && n % 100 <= 13) return "th";
		return switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
	}
}
