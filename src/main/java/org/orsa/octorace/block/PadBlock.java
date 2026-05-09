package org.orsa.octorace.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.NonNull;
import org.orsa.octorace.factory.BlockFactory;
import org.orsa.octorace.interfaces.OrsaBlock;
import xyz.nucleoid.packettweaker.PacketContext;

import static org.orsa.octorace.Octorace.*;

public class PadBlock extends Block implements PolymerTexturedBlock, OrsaBlock<PadBlock> {

    public VoxelShape shape = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    public MapCodec<BoostPadBlock> codec = simpleCodec(BoostPadBlock::new);

    public Identifier blockstateId;
    public BlockState polymerState;

    public ResourceKey<Item> itemKey;
    public PolymerBlockItem item;

    public AttachmentType<Boolean> isOnBlockAttachment;

    public PadBlock(Properties properties) {
        super(properties);
    }

    public void init(BlockFactory factory) {
        blockstateId = id("block/" + factory.name);
        polymerState = PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(blockstateId));

        itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), factory.id);
        item = new PolymerBlockItem(factory.block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        isOnBlockAttachment = AttachmentRegistry.create(id("is_on_" + factory.name), builder -> builder.initializer(() -> false));
    }

    @Override
    protected @NonNull MapCodec<? extends Block> codec() {
        return codec;
    }

    // --- Polymer ---

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return polymerState;
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return shape;
    }

    // --- Behaviour ---

    public void endOfTick() {
        var players = SERVER.getPlayerList().getPlayers();
        for (var player : players) {
            var level = player.level();
            var blockStateBelow = level.getBlockState(player.blockPosition().below());

            var isOnBlock = blockStateBelow.is(this);
            player.setAttached(isOnBlockAttachment, isOnBlock);

            if (isOnBlock) {
                playerOn(player);
            }
        }
    }

    protected void playerOn(ServerPlayer player) {}

    protected void playerJumpedWhileOn(ServerPlayer player) {
        LOGGER.info("playerJumpedWhileOn");

        var pos = player.position();
        var level = player.level();
        level.playSound(null,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                SoundEvents.WIND_CHARGE_BURST, SoundSource.BLOCKS,
                0.8f, 1.4f);
    }

    protected boolean hasElytra(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return !chest.isEmpty() && chest.is(Items.ELYTRA);
    }

    public void onPlayerJumped(ServerPlayer player) {
        var isOnBlock = player.getAttachedOrElse(isOnBlockAttachment, false);

        if (isOnBlock) {
            playerJumpedWhileOn(player);
        }
    }
}
