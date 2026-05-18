package org.orsa.octorace.mixin;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = LivingEntity.class, priority = 2000)
public class PlayerCollisionMixin {

    @Redirect(
        method = "checkAutoSpinAttack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
    )
    private List<Entity> filterRacePlayersFromSpinAttack(Level level, Entity excluded, AABB box) {
        List<Entity> entities = level.getEntities(excluded, box);
        LivingEntity self = (LivingEntity)(Object) this;
        if (!(self instanceof ServerPlayer attacker) || !attacker.getTags().contains("octorace")) {
            return entities;
        }

        List<Entity> filtered = entities.stream().filter(entity -> !(entity instanceof Player player)).toList();

        if (filtered.size() < entities.size()) {
            attacker.connection.send(new ClientboundSetEntityMotionPacket(attacker));
        }

        return filtered;
    }
}
