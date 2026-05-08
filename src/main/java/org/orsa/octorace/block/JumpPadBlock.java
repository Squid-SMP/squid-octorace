package org.orsa.octorace.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.NonNull;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static org.orsa.octorace.Octorace.MOD_ID;

public class JumpPadBlock extends Block implements PolymerTexturedBlock {
	public static final MapCodec<JumpPadBlock> CODEC = simpleCodec(JumpPadBlock::new);

	private static final double LAUNCH_VELOCITY_Y = 1.6D;
	private static final double HORIZONTAL_BOOST = 1.5D;
	private static final int COOLDOWN_TICKS = 5;
	private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
	private static final Map<UUID, Integer> LAST_LAUNCH_TICK = new WeakHashMap<>();

	private static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "jump_pad");
	private static final Identifier BLOCKSTATE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "block/jump_pad");

	public static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(BuiltInRegistries.BLOCK.key(), ID);

	private static final BlockState POLYMER_STATE = PolymerBlockResourceUtils.requestBlock(
			BlockModelType.FULL_BLOCK, PolymerBlockModel.of(BLOCKSTATE_ID)
	);

	public static final Block BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			BLOCK_KEY,
			new JumpPadBlock(BlockBehaviour.Properties.of()
					.setId(BLOCK_KEY)
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
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
		// triggers static initialization of BLOCK, ITEM, and POLYMER_STATE
	}

	public JumpPadBlock(Properties properties) {
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

	// --- Server-side block geometry / behaviour ---

	@Override
	protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void stepOn(Level level, BlockPos blockPos, BlockState blockState, Entity entity) {
		if (level.isClientSide()) return;

		// Don't bounce dropped items — only living entities (players, mobs).
		if (!(entity instanceof LivingEntity living)) return;

		// Cooldown: skip if we just launched this entity.
		Integer last = LAST_LAUNCH_TICK.get(entity.getUUID());
		if (last != null && entity.tickCount - last < COOLDOWN_TICKS) return;
		LAST_LAUNCH_TICK.put(entity.getUUID(), entity.tickCount);

		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x * HORIZONTAL_BOOST, LAUNCH_VELOCITY_Y, v.z * HORIZONTAL_BOOST);
		entity.hurtMarked = true; // forces velocity sync to client immediately
		entity.resetFallDistance(); // prevent fall damage from the boost itself

		// Players with an elytra equipped should auto-glide so they don't waste the boost.
		if (living instanceof Player player && !player.isFallFlying()) {
			if (hasElytra(player)) {
				player.startFallFlying();
			}
		}

		var pos = entity.position();
		level.playSound(null,
				pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
				SoundEvents.WIND_CHARGE_BURST, SoundSource.BLOCKS,
				0.8f, 1.4f);
	}



	private static boolean hasElytra(Player player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return !chest.isEmpty() && chest.is(Items.ELYTRA);
	}
}
