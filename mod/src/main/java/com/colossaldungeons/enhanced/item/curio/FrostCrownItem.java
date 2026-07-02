package com.colossaldungeons.enhanced.item.curio;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Frost Crown curio item - per section 7.11 EXACTLY.
 * 
 * Behavior:
 * - ICurioItem implementation with stacksTo(1) and fireResistant
 * - curioTick: removes FROST_SLOW from the wearer every tick
 * - curioTick: every 20 ticks, applies FROST_SLOW to enemies in 4-block radius
 * - Grants immunity to frost-based effects while worn
 */
public class FrostCrownItem extends Item implements ICurioItem {

    /** Radius for frost aura effect. */
    private static final double FROST_AURA_RADIUS = 4.0;

    /** Frost slow duration applied to enemies (3 seconds). */
    private static final int FROST_SLOW_DURATION = 60;

    /** Tick interval for applying frost to enemies. */
    private static final int FROST_TICK_INTERVAL = 20;

    public FrostCrownItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide()) return;

        // Remove FROST_SLOW from the wearer (immunity)
        if (wearer.hasEffect(CDEEffects.FROST_SLOW)) {
            wearer.removeEffect(CDEEffects.FROST_SLOW);
        }

        // Every 20 ticks: apply FROST_SLOW to enemies in 4-block radius
        if (wearer.tickCount % FROST_TICK_INTERVAL == 0) {
            AABB auraBox = wearer.getBoundingBox().inflate(FROST_AURA_RADIUS);
            List<LivingEntity> nearbyEntities = wearer.level().getEntitiesOfClass(
                LivingEntity.class, auraBox,
                entity -> entity != wearer && entity.isAlive() && entity instanceof Monster
            );

            for (LivingEntity enemy : nearbyEntities) {
                enemy.addEffect(new MobEffectInstance(
                    CDEEffects.FROST_SLOW, FROST_SLOW_DURATION, 0,
                    false, true, true
                ));
            }
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // Remove any existing frost slow when equipped
        LivingEntity wearer = slotContext.entity();
        if (!wearer.level().isClientSide()) {
            wearer.removeEffect(CDEEffects.FROST_SLOW);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // No special unequip logic needed - frost immunity simply stops
    }
}
