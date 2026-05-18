package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
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
import static org.orsa.octorace.Octorace.SERVER;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerSwingMixin {

    @Shadow
    public ServerPlayer player;

    @Unique
    private int lastUseItemTick = -100;
    @Unique
    private int lastSwingTick = -100;

    @Inject(method = "handleUseItem", at = @At("HEAD"))
    private void onUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        lastUseItemTick = player.tickCount;
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void onUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        lastUseItemTick = player.tickCount;
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void onSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        var item = player.getItemInHand(packet.getHand());
        if (!(item.getItem() instanceof RespawnItem)) {
            return;
        }

        int tick = player.tickCount;
        if (tick - lastSwingTick < 3) {
            return;
        }
        lastSwingTick = tick;

        SERVER.execute(() -> {
            if (lastUseItemTick == tick) {
                return;
            }
            RESPAWN_ITEM.onAttack(player);
        });
    }
}
