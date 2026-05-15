package org.orsa.octorace.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.orsa.octorace.Octorace;

public class PlayerEventListener {

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.player));
	}

	private static void onDisconnect(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		Octorace.RACE_MANAGER.onPlayerDisconnect(player);
	}
}
