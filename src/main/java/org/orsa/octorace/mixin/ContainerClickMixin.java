package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.orsa.octorace.Octorace;
import org.orsa.octorace.item.OctoraceTrident;
import org.orsa.octorace.item.UnmoveableItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public class ContainerClickMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;

        for (var unmoveableItem : Octorace.unmoveableItems) {
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

    @Unique
    private boolean itemInSlot(AbstractContainerMenu menu, int slotIndex, UnmoveableItem unmoveableItem) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        var slot = menu.slots.get(slotIndex);
        var item = slot.getItem();
        return unmoveableItem.isItem(item);
    }
}
