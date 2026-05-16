package org.orsa.octorace.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.orsa.octorace.Octorace;

public class PlayerEventListener {

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onChangeDimension(player));
	}

	private static void onDisconnect(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) return;
		Octorace.RACE_MANAGER.onPlayerDisconnect(player);
	}

	private static void onChangeDimension(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) return;
		var participant = Octorace.RACE_MANAGER.getParticipant(player);
		if (participant != null) {
			participant.race.disqualify(participant, false);
		}
	}
}
