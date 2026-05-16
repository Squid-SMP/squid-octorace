package org.orsa.octorace.block.polymer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.orsa.octorace.OctoraceSounds;
import org.orsa.octorace.factory.BlockFactory;

import static org.orsa.octorace.Octorace.playSoundFor;

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

		if (!player.hasEffect(MobEffects.SPEED)) {
			playSoundFor(player, getSoundEffect(), 1.0f, 1.0f);
		}

		var effect = new MobEffectInstance(MobEffects.SPEED, durationTicks, amplifier, false, false);
		player.addEffect(effect);
	}

	@Override
	protected void playerJumpedWhileOn(ServerPlayer player) {}

	@Override
	protected SoundEvent getSoundEffect() {
		return OctoraceSounds.SPEED_PAD;
	}
}
