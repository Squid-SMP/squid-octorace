package org.orsa.octorace.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandManager {

    private static final String NBT_WAND_KEY = "OctoraceWand";

    private static final Map<UUID, BlockPos> pos1 = new HashMap<>();
    private static final Map<UUID, BlockPos> pos2 = new HashMap<>();
    private static final Map<UUID, String> wandDimension = new HashMap<>();

    public static ItemStack createWand() {
        ItemStack wand = new ItemStack(Items.STICK);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_WAND_KEY, true);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        wand.set(DataComponents.ITEM_NAME, Component.literal("§bOctorace Wand"));
        return wand;
    }

    public static boolean isWand(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(NBT_WAND_KEY).orElse(false);
    }

    public static void setPos1(UUID uuid, BlockPos pos, String dimensionId) {
        pos1.put(uuid, pos);
        wandDimension.put(uuid, dimensionId);
        pos2.remove(uuid); // invalidate pos2 whenever pos1 changes dimension
    }

    public static void setPos2(UUID uuid, BlockPos pos) {
        pos2.put(uuid, pos);
    }

    public static BlockPos getPos1(UUID uuid) {
        return pos1.get(uuid);
    }

    public static BlockPos getPos2(UUID uuid) {
        return pos2.get(uuid);
    }

    public static String getWandDimension(UUID uuid) {
        return wandDimension.get(uuid);
    }

    public static boolean hasBothPositions(UUID uuid) {
        return pos1.containsKey(uuid) && pos2.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        pos1.remove(uuid);
        pos2.remove(uuid);
        wandDimension.remove(uuid);
    }
}
