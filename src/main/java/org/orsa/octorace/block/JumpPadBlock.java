package org.orsa.octorace.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class JumpPadBlock extends Block implements PolymerBlock {
	public static final MapCodec<JumpPadBlock> CODEC = simpleCodec(JumpPadBlock::new);

	private static final double LAUNCH_VELOCITY_Y = 1.6D;

	private static final double HORIZONTAL_BOOST = 1.5D;

	private static final int COOLDOWN_TICKS = 5;

	private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

	private static final Map<UUID, Integer> LAST_LAUNCH_TICK = new WeakHashMap<>();

	public JumpPadBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	// --- Polymer ---

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
	}

	// --- Server-side block geometry / behaviour ---

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
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

		level.playSound(null,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS,
				0.8f, 1.4f);
	}

	private static boolean hasElytra(Player player) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		return !chest.isEmpty() && chest.is(Items.ELYTRA);
	}
}
