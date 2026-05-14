package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.orsa.octorace.item.OctoraceTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ContainerClickMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        AbstractContainerMenu menu = player.containerMenu;

        if (tridentInSlot(menu, packet.slotNum())) {
            menu.sendAllDataToRemote();
            ci.cancel();
            return;
        }

        for (var index : packet.changedSlots().keySet()) {
            if (tridentInSlot(menu, index)) {
                menu.sendAllDataToRemote();
                ci.cancel();
                return;
            }
        }
    }

    @Unique
    private boolean tridentInSlot(AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        var slot = menu.slots.get(slotIndex);
        var item = slot.getItem();
        return OctoraceTrident.isOctoraceTrident(item);
    }
}
