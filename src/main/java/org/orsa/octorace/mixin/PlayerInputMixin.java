package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.octorace.Octorace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class PlayerInputMixin {

    @Shadow
    public ServerPlayer player;

    @Unique
    private boolean wasJumping = false;

    @Inject(method = "handlePlayerInput", at = @At("HEAD"))
    private void onPlayerInputHead(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        wasJumping = player.getLastClientInput().jump();
    }

    @Inject(method = "handlePlayerInput", at = @At("TAIL"))
    private void onPlayerInputTail(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        boolean isJumping = player.getLastClientInput().jump();

        if (!wasJumping && isJumping) {
            onJumpPressed(player);
        }
    }

    @Unique
    private void onJumpPressed(ServerPlayer player) {
        Octorace.JUMP_PAD_BLOCK.onPlayerJumped(player);
        Octorace.BOOST_PAD_BLOCK.onPlayerJumped(player);
        Octorace.SPEED_PAD_BLOCK.onPlayerJumped(player);
    }
}
