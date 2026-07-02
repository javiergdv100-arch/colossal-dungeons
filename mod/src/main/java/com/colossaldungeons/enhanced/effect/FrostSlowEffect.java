package com.colossaldungeons.enhanced.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Frost Slow effect - HARMFUL. Color: 0x87CEEB (sky blue).
 * 
 * Reduces movement speed by 30% per level.
 * At level 3 (amplifier 2): triggers vision blur client-side shader.
 * Used by Blizzard Bow, Frost Crown, and Veiled Peak dungeon mechanics.
 */
public class FrostSlowEffect extends MobEffect {

    /** Speed reduction per level (30%). */
    public static final double SPEED_REDUCTION_PER_LEVEL = -0.30;

    /** Amplifier threshold for vision blur effect. */
    public static final int VISION_BLUR_THRESHOLD = 2;

    public FrostSlowEffect() {
        super(MobEffectCategory.HARMFUL, 0x87CEEB);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick every 20 ticks for visual/freezing logic
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Movement speed reduction is applied via attribute modifiers on effect application.
        // Here we handle additional freeze mechanics at high levels.

        // At level 3+: entity starts freezing (uses vanilla freeze mechanic as base)
        if (amplifier >= VISION_BLUR_THRESHOLD) {
            int currentFreeze = entity.getTicksFrozen();
            int maxFreeze = entity.getTicksRequiredToFreeze();
            if (currentFreeze < maxFreeze) {
                entity.setTicksFrozen(Math.min(maxFreeze, currentFreeze + 2));
            }
        }

        return true;
    }

    /**
     * Gets the total speed reduction for the given amplifier.
     *
     * @param amplifier The effect amplifier (0-indexed)
     * @return Speed modifier value (negative, e.g., -0.30 for level 1)
     */
    public static double getSpeedModifier(int amplifier) {
        return SPEED_REDUCTION_PER_LEVEL * (amplifier + 1);
    }

    /**
     * Whether the vision blur shader should be active at this amplifier level.
     *
     * @param amplifier The effect amplifier
     * @return true if vision blur should be triggered
     */
    public static boolean shouldBlurVision(int amplifier) {
        return amplifier >= VISION_BLUR_THRESHOLD;
    }
}
