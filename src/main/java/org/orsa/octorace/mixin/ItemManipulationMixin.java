package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.item.UnmoveableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ItemManipulationMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;

        for (var unmoveableItem : Octorace.unmoveableComponents) {
            if (!unmoveableItem.activePlayers.contains(player)) {
                continue;
            }

            if (itemInSlot(menu, packet.slotNum(), unmoveableItem)) {
                menu.sendAllDataToRemote();
                ci.cancel();
                continue;
            }

            for (var index : packet.changedSlots().keySet()) {
                if (itemInSlot(menu, index, unmoveableItem)) {
                    menu.sendAllDataToRemote();
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        var action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            return;
        }

        var selectedItem = player.getInventory().getSelectedItem();
        for (var unmoveableItem : Octorace.unmoveableComponents) {
            if (unmoveableItem.isItem(selectedItem)) {
                ci.cancel();
                player.containerMenu.broadcastFullState();
                return;
            }
        }
    }

    @Unique
    private boolean itemInSlot(AbstractContainerMenu menu, int slotIndex, UnmoveableComponent unmoveableComponent) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        var slot = menu.slots.get(slotIndex);
        var item = slot.getItem();
        return unmoveableComponent.isItem(item);
    }
}
