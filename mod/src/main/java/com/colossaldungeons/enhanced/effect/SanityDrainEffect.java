package com.colossaldungeons.enhanced.effect;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sanity Drain effect - HARMFUL. Color: 0x4B0082 (indigo).
 * 
 * Slowly increases the MADNESS_LEVEL attachment over time.
 * If MADNESS_LEVEL reaches 100, applies Madness level 5.
 * Cured by Sanity Candle consumable item.
 */
public class SanityDrainEffect extends MobEffect {

    /** Maximum madness level before triggering full madness. */
    public static final int MAX_MADNESS = 100;

    /** Madness increase per tick application. */
    public static final int DRAIN_PER_TICK = 1;

    /** Duration of applied Madness effect at max level (10 minutes). */
    public static final int MADNESS_DURATION = 10 * 60 * 20;

    public SanityDrainEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Drain every 2 seconds (40 ticks) - faster at higher amplifier
        int interval = Math.max(10, 40 - (amplifier * 10));
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return true;

        // Increase madness level
        int currentMadness = player.getData(CDEAttachments.MADNESS_LEVEL);
        int drainAmount = DRAIN_PER_TICK * (amplifier + 1);
        int newMadness = Math.min(MAX_MADNESS, currentMadness + drainAmount);
        player.setData(CDEAttachments.MADNESS_LEVEL, newMadness);

        // If madness reaches maximum, apply full Madness effect level 5
        if (newMadness >= MAX_MADNESS) {
            player.addEffect(new MobEffectInstance(
                CDEEffects.MADNESS, MADNESS_DURATION, MadnessEffect.MAX_LEVEL,
                false, true, true
            ));
        }

        return true;
    }

    /**
     * Gets the current madness percentage for display purposes.
     *
     * @param entity The entity to check
     * @return Madness percentage (0-100)
     */
    public static int getMadnessPercentage(LivingEntity entity) {
        return entity.getData(CDEAttachments.MADNESS_LEVEL);
    }
}
