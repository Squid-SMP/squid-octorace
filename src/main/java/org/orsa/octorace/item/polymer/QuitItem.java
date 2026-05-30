package org.orsa.octorace.item.polymer;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.orsa.octorace.factory.ItemFactory;
import org.orsa.octorace.factory.ManufacturedItem;
import org.orsa.octorace.item.UnmoveableComponent;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import static org.orsa.octorace.Octorace.RACE_MANAGER;

public class QuitItem extends Item implements PolymerItem, ManufacturedItem<QuitItem> {

    public UnmoveableComponent unmoveable;

    Item polymerItem;

    public QuitItem(Properties properties) {
        super(properties);
    }

    public static QuitItem register() {
        var properties = new Properties();

        var factory = new ItemFactory<>(QuitItem::new, "quit", properties);
        var item = factory.item;

        item.init(factory);
        factory.register();

        return item;
    }

    public void init(ItemFactory<QuitItem> factory) {
        polymerItem = Items.CLAY_BALL;

        unmoveable = new UnmoveableComponent("quit", 8, this);
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

        participant.quit();

        return InteractionResult.SUCCESS;
    }
}
