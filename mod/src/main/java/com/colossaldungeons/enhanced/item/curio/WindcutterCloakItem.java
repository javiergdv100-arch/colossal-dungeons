package com.colossaldungeons.enhanced.item.curio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Windcutter Cloak curio - increases movement speed and reduces fall damage.
 * 
 * Behavior:
 * - onEquip: +20% movement speed via attribute modifier
 * - curioTick: reduces fall damage (sets fallDistance to 0 if < 5 blocks)
 * - onUnequip: removes the speed modifier
 */
public class WindcutterCloakItem extends Item implements ICurioItem {

    private static final ResourceLocation SPEED_MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "windcutter_speed");

    /** Speed boost: +20% */
    private static final double SPEED_BOOST = 0.20;

    /** Fall distance threshold below which fall damage is negated. */
    private static final float FALL_NEGATE_THRESHOLD = 5.0f;

    public WindcutterCloakItem() {
        super(new Properties().stacksTo(1).durability(256));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide()) return;

        // Reduce fall damage: reset fallDistance if below threshold
        if (wearer.fallDistance > 0 && wearer.fallDistance < FALL_NEGATE_THRESHOLD) {
            wearer.fallDistance = 0;
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide()) return;

        // Apply speed modifier
        AttributeInstance speedAttribute = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Remove existing modifier first to avoid stacking
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);

            AttributeModifier speedModifier = new AttributeModifier(
                SPEED_MODIFIER_ID, SPEED_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            speedAttribute.addPermanentModifier(speedModifier);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide()) return;

        // Remove speed modifier
        AttributeInstance speedAttribute = wearer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
