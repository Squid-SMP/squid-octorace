package org.orsa.octorace.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.orsa.octorace.Octorace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotPickupMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void preventUnmoveablePickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer sp)) return;
        var rm = Octorace.RACE_MANAGER;
        if (rm == null) return;
        Slot slot = (Slot) (Object) this;
        ItemStack item = slot.getItem();
        for (var unmoveable : Octorace.unmoveableComponents) {
            if (unmoveable.activePlayers.contains(sp) && unmoveable.isItem(item)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
