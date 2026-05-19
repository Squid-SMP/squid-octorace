package org.orsa.octorace.item.polymer;

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
import org.orsa.octorace.item.UnmoveableComponent;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.orsa.octorace.Octorace.*;

public class RespawnItem extends Item implements PolymerItem, ManufacturedItem<RespawnItem> {

    public UnmoveableComponent unmoveable;
    public final Set<UUID> pendingRightClickSwing = new HashSet<>();

    Item polymerItem;

    public RespawnItem(Properties properties) {
        super(properties);
    }

    public static RespawnItem register() {
        var properties = new Properties();

        var factory = new ItemFactory<>(RespawnItem::new, "respawn", properties);
        var item = factory.item;

        item.init(factory);
        factory.register();

        return item;
    }

    public void init(ItemFactory<RespawnItem> factory) {
        polymerItem = Items.ECHO_SHARD;

        var itemStack = new ItemStack(this);
        unmoveable = new UnmoveableComponent("respawn", 7, itemStack);
    }

    public void tick() {
        unmoveable.tick();
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return polymerItem;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        var participant = RACE_MANAGER.getParticipant(serverPlayer);
        if (participant == null) {
            return InteractionResult.SUCCESS;
        }

        pendingRightClickSwing.add(serverPlayer.getUUID());
        participant.respawnAtLastCheckpoint();

        return InteractionResult.SUCCESS;
    }

    public void onAttack(ServerPlayer player) {
        var participant = RACE_MANAGER.getParticipant(player);
        if (participant == null) {
            return;
        }

        participant.restart();
    }
}
