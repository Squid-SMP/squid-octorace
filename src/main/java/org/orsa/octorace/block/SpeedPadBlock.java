package org.orsa.octorace.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.orsa.octorace.block.PadBlock;
import org.orsa.octorace.factory.BlockFactory;

public class SpeedPadBlock extends PadBlock {

	public SpeedPadBlock(Properties properties) {
		super(properties);
	}

	public static SpeedPadBlock register() {
		var properties = BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_LIGHT_BLUE)
				.sound(SoundType.METAL);

		var factory = new BlockFactory<>(SpeedPadBlock::new, "speed_pad", properties);
		var block = factory.block;

		block.init(factory);
		factory.register();

		return block;
	}

	@Override
	public void playerOn(ServerPlayer player) {
		var durationSeconds = 3;
		var durationTicks = durationSeconds * 20;

		var amplifier = 4;

		player.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, amplifier));
	}
}
