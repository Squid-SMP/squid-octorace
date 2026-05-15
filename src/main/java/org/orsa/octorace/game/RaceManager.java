package org.orsa.octorace.game;

import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.RaceConfig;

import java.util.*;

import static org.orsa.octorace.Octorace.LOGGER;

public class RaceManager {

	private final RaceConfig config;

	public Map<UUID, RaceParticipant> allParticipants = new HashMap<>();
	public List<Race> ongoingRaces = new ArrayList<>();

	public RaceManagerStorage raceManagerStorage;

	public RaceManager(RaceConfig config) {
		this.config = config;

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
			raceManagerStorage.playersInRace.add(participant.uuid);
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
			raceManagerStorage.playersInRace.remove(participant.uuid);
		}
	}

	public void onPlayerDisconnect(ServerPlayer player) {
		var uuid = player.getUUID();
		if (!allParticipants.containsKey(uuid)) {
			return;
		}

		var participant = allParticipants.remove(uuid);

		var race = participant.race;
		race.onParticipantDisconnect(participant);
	}

	public void onPlayerDied(ServerPlayer player) {
		var uuid = player.getUUID();
		if (!allParticipants.containsKey(uuid)) {
			return;
		}

		var participant = allParticipants.get(uuid);
		participant.onDied();
	}

	public void onPlayerRespawn(ServerPlayer player) {
		var uuid = player.getUUID();
		if (!allParticipants.containsKey(uuid)) {
			return;
		}

		var participant = allParticipants.get(uuid);

		if (!participant.needsRespawnTeleport) {
			return;
		}

		var race = participant.race;
		race.onParticipantRespawn(participant);
	}

	public void addTimeTrialsResult(RaceParticipant participant) {
		var uuid = participant.uuid;
		var time = participant.finishTimeMillis;
		var displayName = participant.displayName;

		raceManagerStorage.addTimeTrialsEntry(uuid, time, displayName);
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
