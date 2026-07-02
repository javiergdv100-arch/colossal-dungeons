package com.colossaldungeons.enhanced.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hemorrhage effect - HARMFUL. Color: 0x8B0000 (dark red).
 * 
 * Damage over time: deals 1 heart (2 HP) per level every 40 ticks.
 * Stacks up to 3 levels. Decays naturally over time.
 */
public class HemorrhageEffect extends MobEffect {

    /** Maximum stack level (amplifier 2 = 3 stacks). */
    public static final int MAX_STACKS = 2;

    /** Ticks between each damage tick. */
    public static final int TICK_INTERVAL = 40;

    /** Base damage per tick (1 heart = 2 HP). */
    public static final float BASE_DAMAGE = 2.0f;

    public HemorrhageEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Clamp amplifier to max stacks
        int effectiveStacks = Math.min(amplifier, MAX_STACKS) + 1;

        // Deal damage: 1 heart per stack
        float damage = BASE_DAMAGE * effectiveStacks;
        entity.hurt(entity.damageSources().magic(), damage);

        return true;
    }

    /**
     * Gets the effective number of stacks for display/logic purposes.
     *
     * @param amplifier The effect amplifier
     * @return Number of active stacks (1-3)
     */
    public static int getEffectiveStacks(int amplifier) {
        return Math.min(amplifier, MAX_STACKS) + 1;
    }
}
