package org.orsa.octorace.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.orsa.octorace.factory.ManufacturedItem;

import java.util.ArrayList;
import java.util.List;

import static org.orsa.octorace.Octorace.SERVER;

public class UnmoveableComponent {
    private final String itemKey;
    private final int slot;
    private final ItemStack referenceStack;

    public List<ServerPlayer> activePlayers = new ArrayList<>();

    public UnmoveableComponent(String itemKey, int slot, ItemStack referenceStack) {
        this.itemKey = itemKey;
        this.slot = slot;
        this.referenceStack = referenceStack;
    }

    public void tick() {
        activePlayers.removeIf(ServerPlayer::isRemoved);

        for (var player : activePlayers) {
            enforceSlot(player);
        }

        for (var player : SERVER.getPlayerList().getPlayers()) {
            if (!activePlayers.contains(player)) {
                var inventory = player.getInventory();

                for (int i = 1; i <= 40; i++) {
                    if (i == slot) {
                        continue;
                    }

                    var stack = inventory.getItem(i);
                    if (isItem(stack)) {
                        inventory.setItem(i, ItemStack.EMPTY);
                        return;
                    }
                }
            }
        }
    }

    public void enforceSlot(ServerPlayer player) {
        var inventory = player.getInventory();

        for (int i = 1; i <= 40; i++) {
            if (i == slot) {
                continue;
            }

            var stack = inventory.getItem(i);
            if (isItem(stack)) {
                inventory.setItem(i, ItemStack.EMPTY);
                return;
            }
        }

        var stackInSlot = inventory.getItem(slot);
        if (isItem(stackInSlot)) {
            return;
        }

        giveItem(player);
    }

    public boolean isItem(ItemStack stack) {
        var item = stack.getItem();
        var isSameItemAsReference = (item == referenceStack.getItem());

        if (!isSameItemAsReference) {
            return false;
        }

        if (item instanceof ManufacturedItem<?>) {
            return true;
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
        giveItem(player);
        activePlayers.add(player);
    }

    public void removeActivePlayer(ServerPlayer player) {
        activePlayers.remove(player);
        removeItem(player);
    }
}
