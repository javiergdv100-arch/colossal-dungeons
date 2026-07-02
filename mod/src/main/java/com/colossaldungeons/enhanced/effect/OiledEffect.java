package com.colossaldungeons.enhanced.effect;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Oiled effect - HARMFUL. Entities affected by oil take 2x fire damage
 * and have reduced friction (movement speed penalty).
 * Visual overlay is synced via the OIL_STATUS attachment.
 */
public class OiledEffect extends MobEffect {

    private static final String FRICTION_MODIFIER_ID = "colossal_dungeons_enhanced.oiled_friction";

    public OiledEffect() {
        super(MobEffectCategory.HARMFUL, 0x1A1A1A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick every second (20 ticks)
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // If the entity is on fire, deal 2x fire damage
        if (entity.isOnFire()) {
            DamageSource fireSource = entity.damageSources().onFire();
            float baseDamage = 1.0f;
            float multipliedDamage = baseDamage * 2.0f * (amplifier + 1);
            entity.hurt(fireSource, multipliedDamage);
        }

        // Update the OIL_STATUS attachment for visual sync
        entity.setData(CDEAttachments.OIL_STATUS, amplifier + 1);

        return true;
    }

    /**
     * Applies movement speed reduction to simulate reduced friction.
     * -30% per amplifier level.
     */
    public static double getSpeedReduction(int amplifier) {
        return -0.30 * (amplifier + 1);
    }

    /**
     * Called when the effect is first applied or the entity is loaded.
     * Sets OIL_STATUS attachment.
     */
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        entity.setData(CDEAttachments.OIL_STATUS, amplifier + 1);
    }

    /**
     * Checks if oil can propagate to another entity on contact.
     */
    public static boolean canPropagate(LivingEntity source, LivingEntity target) {
        return source.getData(CDEAttachments.OIL_STATUS) > 0
            && target.getData(CDEAttachments.OIL_STATUS) == 0;
    }
}
