package org.orsa.octorace.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.orsa.octorace.Octorace;

public class PlayerMovementListener {

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.player));
		ServerLivingEntityEvents.AFTER_DEATH.register(PlayerMovementListener::onDeath);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onRespawn(newPlayer));
	}

	private static void onDisconnect(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		Octorace.RACE_MANAGER.onPlayerDisconnect(player);
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		if (entity instanceof ServerPlayer sp) {
			Octorace.RACE_MANAGER.onPlayerDied(sp);
		}
	}

	private static void onRespawn(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		Octorace.RACE_MANAGER.onPlayerRespawn(player);
	}

}
