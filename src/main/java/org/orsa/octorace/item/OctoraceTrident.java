package org.orsa.octorace.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import static org.orsa.octorace.Octorace.SERVER;

public class OctoraceTrident {

    private static final int TRIDENT_SLOT = 0;
    private static final String TAG_KEY = "octorace_trident";

    private static ItemStack itemStack;
    public static UnmoveableComponent unmoveable;

    private OctoraceTrident() {}

    public static void init() {
        itemStack = createStack();
        unmoveable = new UnmoveableComponent(TAG_KEY, TRIDENT_SLOT, itemStack);
    }

    public static void tick() {
        unmoveable.tick();
    }

    private static ItemStack createStack() {
        var stack = new ItemStack(Items.TRIDENT);

        var enchantmentRegistry = SERVER.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
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

    public static void togglePlayer(ServerPlayer player) {
        if (unmoveable.playerHasItem(player)) {
            unmoveable.removeActivePlayer(player);
            var waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (waterAttr != null) {
                waterAttr.removeModifier(Identifier.parse("octorace:depth_strider"));
            }
            player.removeEffect(MobEffects.DOLPHINS_GRACE);
            player.removeEffect(MobEffects.WATER_BREATHING);
        }
        else {
            unmoveable.addActivePlayer(player);
            var waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (waterAttr != null) {
                waterAttr.addOrUpdateTransientModifier(new AttributeModifier(
                    Identifier.parse("octorace:depth_strider"), 1.0, AttributeModifier.Operation.ADD_VALUE
                ));
            }

            var effect1 = new MobEffectInstance(MobEffects.DOLPHINS_GRACE, MobEffectInstance.INFINITE_DURATION, 1, false, false);
            player.addEffect(effect1);
            var effect2 = new MobEffectInstance(MobEffects.WATER_BREATHING, MobEffectInstance.INFINITE_DURATION, 0, false, false);
            player.addEffect(effect2);
        }
    }
}
