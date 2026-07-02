package com.colossaldungeons.enhanced.effect;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Resonance Charge effect - NEUTRAL. Color: 0x9B59B6.
 * 
 * Increases damage from resonance overload events.
 * Increases crystal charge speed when near Resonance Crystal blocks.
 * Stacks with amplifier affecting charge rate multiplier.
 */
public class ResonanceChargeEffect extends MobEffect {

    public ResonanceChargeEffect() {
        super(MobEffectCategory.NEUTRAL, 0x9B59B6);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick every 20 ticks to update resonance interactions
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Increase resonance level attachment when effect is active
        int currentResonance = entity.getData(CDEAttachments.RESONANCE_LEVEL);
        int maxResonance = 10;

        if (currentResonance < maxResonance) {
            int chargeRate = 1 + amplifier;
            int newLevel = Math.min(maxResonance, currentResonance + chargeRate);
            entity.setData(CDEAttachments.RESONANCE_LEVEL, newLevel);
        }

        return true;
    }

    /**
     * Gets the damage multiplier for resonance overload events.
     * Each level adds 50% more damage.
     *
     * @param amplifier The effect amplifier
     * @return Damage multiplier (1.5 at level 1, 2.0 at level 2, etc.)
     */
    public static float getOverloadDamageMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.5f;
    }

    /**
     * Gets the crystal charge speed multiplier.
     * Each level doubles the charge speed.
     *
     * @param amplifier The effect amplifier
     * @return Charge speed multiplier
     */
    public static float getCrystalChargeMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1);
    }
}
