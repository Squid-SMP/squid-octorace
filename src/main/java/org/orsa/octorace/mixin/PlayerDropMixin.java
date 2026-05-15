package org.orsa.octorace.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.orsa.octorace.Octorace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class PlayerDropMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void onDrop(ItemStack stack, boolean throwRandomly, boolean awardStats, CallbackInfoReturnable<ItemEntity> ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        for (var unmoveableItem : Octorace.unmoveableComponents) {
            if (!unmoveableItem.activePlayers.contains(self)) {
                continue;
            }

            if (unmoveableItem.isItem(stack)) {
                unmoveableItem.giveItem(self);
                ci.setReturnValue(null);
                return;
            }
        }
    }
}
