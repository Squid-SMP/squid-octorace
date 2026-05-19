package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.octorace.item.polymer.RespawnItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.orsa.octorace.Octorace.RESPAWN_ITEM;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerSwingMixin {

    @Shadow
    public ServerPlayer player;

    @Unique
    private int lastSwingTick = -100;

    @Inject(method = "handleAnimate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void onSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        var item = player.getItemInHand(packet.getHand());
        if (!(item.getItem() instanceof RespawnItem)) {
            return;
        }

        if (RESPAWN_ITEM.pendingRightClickSwing.remove(player.getUUID())) {
            return;
        }

        int tick = player.tickCount;
        if (tick - lastSwingTick < 3) {
            return;
        }
        lastSwingTick = tick;

        RESPAWN_ITEM.onAttack(player);
    }
}
