package org.orsa.octorace.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

import org.orsa.octorace.factory.BlockFactory;

public class BoostPadBlock extends PadBlock {

	public BoostPadBlock(Properties properties) {
		super(properties);
	}

	public static BoostPadBlock register() {
		var properties = BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_RED)
				.sound(SoundType.METAL);

		var factory = new BlockFactory<>(BoostPadBlock::new, "boost_pad", properties);
		var block = factory.block;

		block.init(factory);
		factory.register();

		return block;
	}

	@Override
	protected void playerJumpedWhileOn(ServerPlayer player) {
		super.playerJumpedWhileOn(player);
		Vec3 look = player.getLookAngle();

		var yMomentum = 1.3;
		var speed = 1.6;

		player.setDeltaMovement(look.x * speed, yMomentum, look.z * speed);
		player.hurtMarked = true;
		player.resetFallDistance();
	}
}
