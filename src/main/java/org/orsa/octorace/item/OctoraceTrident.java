package org.orsa.octorace.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OctoraceTrident {

    private static final int TRIDENT_SLOT = 0;
    private static final String TAG_KEY = "octorace_trident";
    private static final Set<UUID> activePlayers = new HashSet<>();

    private OctoraceTrident() {}

    public static void giveTrident(ServerPlayer player) {
        var stack = createStack(player);
        player.getInventory().setItem(TRIDENT_SLOT, stack);
        activePlayers.add(player.getUUID());
    }

    public static void removeTrident(ServerPlayer player) {
        activePlayers.remove(player.getUUID());
        var inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            if (isOctoraceTrident(inventory.getItem(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static boolean playerHasTrident(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i <= 40; i++) {
            if (isOctoraceTrident(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    public static void enforceTridentSlot(ServerPlayer player) {
        if (!activePlayers.contains(player.getUUID())) {
            return;
        }

        var inventory = player.getInventory();
        if (isOctoraceTrident(inventory.getItem(TRIDENT_SLOT))) {
            return;
        }

        for (int i = 1; i <= 40; i++) {
            if (isOctoraceTrident(inventory.getItem(i))) {
                var displaced = inventory.getItem(TRIDENT_SLOT).copy();
                inventory.setItem(TRIDENT_SLOT, inventory.getItem(i).copy());
                inventory.setItem(i, displaced);
                return;
            }
        }

        // Check cursor (item being dragged in open inventory/container)
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.containerMenu.broadcastFullState();

        // Was dropped — discard the entity so it can't be picked up, then give it back
        var box = player.getBoundingBox().inflate(32);
        var droppedItems = player.level().getEntitiesOfClass(ItemEntity.class, box, entity -> isOctoraceTrident(entity.getItem()));
        droppedItems.forEach(ItemEntity::discard);
        giveTrident(player);
    }

    private static ItemStack createStack(ServerPlayer player) {
        var stack = new ItemStack(Items.TRIDENT);

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> riptide = enchantmentRegistry.getOrThrow(Enchantments.RIPTIDE);

        var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(riptide, 2);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        var tag = new CompoundTag();
        tag.putBoolean(TAG_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        stack.set(DataComponents.ITEM_NAME, Component.literal("Race Trident"));

        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);

        return stack;
    }

    public static boolean isOctoraceTrident(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.TRIDENT) {
            return false;
        }

        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(TAG_KEY);
    }
}
