package org.orsa.octorace.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.orsa.octorace.Octorace;
import xyz.nucleoid.packettweaker.PacketContext;

import static org.orsa.octorace.Octorace.*;

public class SpeedPadBlock extends Block implements PolymerTexturedBlock {
	public static final MapCodec<SpeedPadBlock> CODEC = simpleCodec(SpeedPadBlock::new);

	private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

	private static final Identifier ID = id("speed_pad");
	private static final Identifier BLOCKSTATE_ID = id("block/speed_pad");

	public static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(BuiltInRegistries.BLOCK.key(), ID);

	private static final BlockState POLYMER_STATE = PolymerBlockResourceUtils.requestBlock(
			BlockModelType.FULL_BLOCK, PolymerBlockModel.of(BLOCKSTATE_ID)
	);

	public static final Block BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			BLOCK_KEY,
			new SpeedPadBlock(Properties.of()
					.setId(BLOCK_KEY)
					.mapColor(MapColor.COLOR_LIGHT_GREEN)
					.strength(1.5f, 6.0f)
					.sound(SoundType.METAL)
					.requiresCorrectToolForDrops()
			)
	);

	public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), ID);

	public static final Item ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			ITEM_KEY,
			new PolymerBlockItem(
					BLOCK,
					new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()
			)
	);

	public static void register() {
		// triggers static initialization
	}

	public SpeedPadBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @NonNull MapCodec<? extends Block> codec() {
		return CODEC;
	}

	// --- Polymer ---

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return POLYMER_STATE;
	}

	// --- Server-side block geometry ---

	@Override
	protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return SHAPE;
	}

	// --- Behaviour ---

	public static void endOfTick() {
		var players = SERVER.getPlayerList().getPlayers();
		for (var player : players) {
			var level = player.level();
			var blockStateBelow = level.getBlockState(player.blockPosition().below());
			var isOnBlock = blockStateBelow.is(BLOCK);

			if (isOnBlock) {
				playerOnIt(player);
			}
		}
	}

	public static void playerOnIt(ServerPlayer player) {
		LOGGER.info(player.toString());

		var durationSeconds = 3;
		var durationTicks = durationSeconds * 20;

		var amplifier = 4;

		player.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, amplifier));
	}
}
