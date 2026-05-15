package org.orsa.octorace.block.polymer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import org.orsa.octorace.factory.BlockFactory;

public class JumpPadBlock extends PadBlock {

	public JumpPadBlock(Properties properties) {
		super(properties);
	}

	public static JumpPadBlock register() {
		var properties = BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_LIGHT_BLUE)
				.sound(SoundType.METAL);

		var factory = new BlockFactory<>(JumpPadBlock::new, "jump_pad", properties);
		var block = factory.block;

		block.init(factory);
		factory.register();

		return block;
	}

	@Override
	protected void playerJumpedWhileOn(ServerPlayer player) {
		super.playerJumpedWhileOn(player);
		Vec3 v = player.getDeltaMovement();

		var xzBoost = 1.5;
		var yMomentum = 1.6;

		player.setDeltaMovement(v.x * xzBoost, yMomentum, v.z * xzBoost);
		player.hurtMarked = true;
		player.resetFallDistance();
	}
}