package com.colossaldungeons.enhanced.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Parasitic Poison effect - HARMFUL. Color: 0x2ECC40 (green).
 * 
 * Deals damage and applies nausea. Inflicted by Leviathan dungeon creatures.
 * More severe than standard poison - deals actual HP damage.
 */
public class ParasiticPoisonEffect extends MobEffect {

    /** Damage per tick (0.5 hearts = 1 HP). */
    public static final float DAMAGE_PER_TICK = 1.0f;

    /** Nausea duration applied per tick (2 seconds). */
    public static final int NAUSEA_DURATION = 40;

    public ParasiticPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x2ECC40);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick every 30 ticks (1.5 seconds)
        int interval = Math.max(10, 30 - (amplifier * 5));
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Deal damage
        float damage = DAMAGE_PER_TICK * (amplifier + 1);
        entity.hurt(entity.damageSources().magic(), damage);

        // Apply nausea (Confusion in code)
        entity.addEffect(new MobEffectInstance(
            MobEffects.CONFUSION, NAUSEA_DURATION, 0,
            false, false, false
        ));

        return true;
    }
}
