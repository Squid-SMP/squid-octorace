package org.orsa.octorace.event;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.item.polymer.WandItem;

import static org.orsa.octorace.Octorace.WAND_ITEM;

public class PlayerEventListener {

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register(PlayerEventListener::onDisconnect);
		AttackBlockCallback.EVENT.register(PlayerEventListener::onAttackBlock);
	}

	private static void onDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		var player = handler.player;

		if (Octorace.RACE_MANAGER == null) {
			return;
		}
		Octorace.RACE_MANAGER.onPlayerDisconnect(player);
	}

	private static InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, BlockPos blockPos, Direction direction) {
		var itemInHand = player.getItemInHand(hand).getItem();
		if (itemInHand instanceof WandItem) {
			return WAND_ITEM.onAttackBlock(player, world, hand, blockPos, direction);
		}

		return InteractionResult.PASS;
	}
}
