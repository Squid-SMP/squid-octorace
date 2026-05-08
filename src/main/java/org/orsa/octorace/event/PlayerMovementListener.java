package org.orsa.octorace.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.command.WandManager;

public class PlayerMovementListener {

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.player));
		ServerLivingEntityEvents.AFTER_DEATH.register(PlayerMovementListener::onDeath);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onRespawn(newPlayer));
		AttackBlockCallback.EVENT.register(PlayerMovementListener::onWandAttackBlock);
		UseBlockCallback.EVENT.register(PlayerMovementListener::onWandUseBlock);
	}

	private static void onDisconnect(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		Octorace.RACE_MANAGER.onParticipantDisconnect(player);
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		if (entity instanceof ServerPlayer sp) {
			Octorace.RACE_MANAGER.onParticipantDied(sp);
		}
	}

	private static void onRespawn(ServerPlayer player) {
		if (Octorace.RACE_MANAGER == null) {
			return;
		}

		Octorace.RACE_MANAGER.onParticipantRespawn(player);
	}

	private static InteractionResult onWandAttackBlock(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
		if (world.isClientSide() || !(player instanceof ServerPlayer)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!WandManager.isWand(held)) {
			return InteractionResult.PASS;
		}
		String dimensionId = world.dimension().identifier().toString();
		WandManager.setPos1(player.getUUID(), pos, dimensionId);
		((ServerPlayer) player).sendSystemMessage(Component.literal(
				"§aCorner 1 set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " in " + dimensionId));
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult onWandUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
		if (world.isClientSide() || !(player instanceof ServerPlayer)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!WandManager.isWand(held)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hitResult.getBlockPos();
		UUID uuid = player.getUUID();
		String currentDimension = world.dimension().identifier().toString();
		String wandDimension = WandManager.getWandDimension(uuid);
		if (wandDimension != null && !currentDimension.equals(wandDimension)) {
			((ServerPlayer) player).sendSystemMessage(Component.literal(
					"§cCorner 2 must be in the same dimension as corner 1 (" + wandDimension + ")."));
			return InteractionResult.SUCCESS;
		}
		WandManager.setPos2(uuid, pos);
		((ServerPlayer) player).sendSystemMessage(Component.literal(
				"§bCorner 2 set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
		return InteractionResult.SUCCESS;
	}
}
