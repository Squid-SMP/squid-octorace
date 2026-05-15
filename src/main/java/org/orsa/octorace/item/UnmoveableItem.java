package org.orsa.octorace.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static org.orsa.octorace.Octorace.SERVER;

public class UnmoveableItem {
    private static final String UNMOVEABLE_KEY = "octorace_trident";

    private final String itemKey;
    private final int slot;
    private final ItemStack referenceStack;

    public final List<ServerPlayer> activePlayers = new ArrayList<>();

    public UnmoveableItem(String itemKey, int slot, ItemStack referenceStack) {
        this.itemKey = itemKey;
        this.slot = slot;
        this.referenceStack = referenceStack;
    }

    public void tick() {
        for (var player : activePlayers) {
            enforceSlot(player);
        }
    }

    public void enforceSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        var stackInSlot = inventory.getItem(slot);
        if (isItem(stackInSlot)) {
            return;
        }

        for (int i = 1; i <= 40; i++) {
            var stack = inventory.getItem(i);
            if (isItem(stack)) {
                var displaced = inventory.getItem(slot).copy();
                inventory.setItem(slot, inventory.getItem(i).copy());
                inventory.setItem(i, displaced);
                return;
            }
        }

//        var box = player.getBoundingBox().inflate(32);
//        var droppedItems = player.level().getEntitiesOfClass(ItemEntity.class, box, entity -> isItem(entity.getItem()));
//        droppedItems.forEach(ItemEntity::discard);

        giveItem(player);
    }

    public boolean isItem(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != referenceStack.getItem()) {
            return false;
        }

        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(itemKey);
    }

    public boolean playerHasItem(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            if (isItem(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    public void giveItem(ServerPlayer player) {
        player.getInventory().setItem(slot, referenceStack.copy());
    }

    public void removeItem(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            var item = inventory.getItem(i);
            if (isItem(item)) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public void addActivePlayer(ServerPlayer player) {
        removeItem(player);
        activePlayers.add(player);
    }

    public void removeActivePlayer(ServerPlayer player) {
        removeItem(player);
        activePlayers.remove(player);
    }
}
