package org.orsa.octorace.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.orsa.octorace.Octorace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class PlayerChangeDimensionMixin {

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void onTeleport(TeleportTransition teleportTransition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (teleportTransition.newLevel() != self.level()) {
            if (Octorace.RACE_MANAGER == null) {
                return;
            }
            var participant = Octorace.RACE_MANAGER.getParticipant(self);
            if (participant != null) {
                participant.race.disqualify(participant, false);
            }
        }
    }
}
