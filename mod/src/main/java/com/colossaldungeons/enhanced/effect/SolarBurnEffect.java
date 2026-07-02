package com.colossaldungeons.enhanced.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Solar Burn effect - HARMFUL. Color: 0xFFAA00 (orange-gold).
 * 
 * Deals damage when entity is in light level > 12.
 * Stops dealing damage in shadow (light level <= 12).
 * Used in the Solar Palace dungeon theme.
 */
public class SolarBurnEffect extends MobEffect {

    /** Light level threshold above which damage is dealt. */
    public static final int LIGHT_THRESHOLD = 12;

    /** Base damage per tick (0.5 hearts = 1 HP). */
    public static final float BASE_DAMAGE = 1.0f;

    public SolarBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFAA00);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Check every 20 ticks (1 second)
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        BlockPos entityPos = entity.blockPosition();
        int lightLevel = level.getMaxLocalRawBrightness(entityPos);

        // Only deal damage in bright areas
        if (lightLevel > LIGHT_THRESHOLD) {
            float damage = BASE_DAMAGE * (amplifier + 1);
            entity.hurt(entity.damageSources().magic(), damage);

            // Set entity briefly on fire for visual effect
            if (amplifier >= 1) {
                entity.setRemainingFireTicks(20);
            }
        }
        // In shadow: no damage, effect persists but is inactive

        return true;
    }

    /**
     * Checks if the entity is currently in a "safe" shadow zone.
     *
     * @param level The server level
     * @param entity The affected entity
     * @return true if in shadow (light <= threshold)
     */
    public static boolean isInShadow(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        return level.getMaxLocalRawBrightness(pos) <= LIGHT_THRESHOLD;
    }
}
