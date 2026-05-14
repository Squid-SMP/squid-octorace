package org.orsa.octorace.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.config.Checkpoint;
import org.orsa.octorace.config.RaceConfig;
import org.orsa.octorace.factory.ItemFactory;
import org.orsa.octorace.factory.ManufacturedItem;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.orsa.octorace.Octorace.RACE_MANAGER;

public class WandItem extends Item implements PolymerItem, ManufacturedItem<WandItem> {

    private final Map<UUID, BlockPos> pos1 = new HashMap<>();
    private final Map<UUID, BlockPos> pos2 = new HashMap<>();

    Item polymerItem;

    public WandItem(Properties properties) {
        super(properties);
    }

    public static WandItem register() {
        var properties = new Item.Properties();

        var factory = new ItemFactory<>(WandItem::new, "wand", properties);
        var item = factory.item;

        item.init(factory);
        factory.register();

        return item;
    }

    public void init(ItemFactory factory) {
        polymerItem = Items.STICK;

        AttackBlockCallback.EVENT.register(this::onAttackBlock);
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return polymerItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        var blockPos = context.getClickedPos();

        return trySetPos(2, player, world, blockPos);
    }

    private InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, BlockPos blockPos, Direction direction) {
        var itemInHand = player.getItemInHand(hand).getItem();
        if (!(itemInHand instanceof WandItem)) {
            return InteractionResult.PASS;
        }
        return trySetPos(1, player, world, blockPos);
    }

    private InteractionResult trySetPos(int pos, Player player, Level world, BlockPos blockPos) {
        if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        var dimensionId = world.dimension().identifier().toString();
        var configDimensionId = RACE_MANAGER.getConfig().getDimensionId();
        if (!dimensionId.equals(configDimensionId)) {
            var message = Component.literal("Checkpoints must be placed in the same dimension as the start of the race.").withStyle(ChatFormatting.RED);
            serverPlayer.sendSystemMessage(message);
            return InteractionResult.SUCCESS;
        }

        var uuid = serverPlayer.getUUID();
        if (pos == 1) setPos1(uuid, blockPos);
        else if (pos == 2) setPos2(uuid, blockPos);

        var message = Component.literal("Corner " + pos + " set to " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ()).withStyle(ChatFormatting.AQUA);
        serverPlayer.sendSystemMessage(message);
        return InteractionResult.SUCCESS;
    }

    public void setPos1(UUID uuid, BlockPos pos) {
        pos1.put(uuid, pos);
    }

    public void setPos2(UUID uuid, BlockPos pos) {
        pos2.put(uuid, pos);
    }

    public BlockPos getPos1(UUID uuid) {
        return pos1.get(uuid);
    }

    public BlockPos getPos2(UUID uuid) {
        return pos2.get(uuid);
    }

    public boolean hasBothPositions(UUID uuid) {
        return pos1.containsKey(uuid) && pos2.containsKey(uuid);
    }

    public Checkpoint tryConfirm(UUID uuid, String name) {
        if (!hasBothPositions(uuid)) {
            return null;
        }

        RaceConfig config = Octorace.RACE_MANAGER.getConfig();
        var checkpoint = config.addCheckpoint(getPos1(uuid), getPos2(uuid), name);
        clear(uuid);

        return checkpoint;
    }

    public void clear(UUID uuid) {
        pos1.remove(uuid);
        pos2.remove(uuid);
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Octorace.WAND_ITEM);
        wand.set(DataComponents.ITEM_NAME, Component.literal("Octorace Wand").withStyle(ChatFormatting.BOLD));
        return wand;
    }
}
