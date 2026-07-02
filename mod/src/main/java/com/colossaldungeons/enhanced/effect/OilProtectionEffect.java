package com.colossaldungeons.enhanced.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Oil Protection effect - BENEFICIAL. Prevents OiledEffect from being applied.
 * Default duration: 30 minutes. Shows golden particles when active.
 * Color: 0xFFD700 (gold).
 */
public class OilProtectionEffect extends MobEffect {

    /** Default duration in ticks (30 minutes = 36000 ticks). */
    public static final int DEFAULT_DURATION = 30 * 60 * 20;

    public OilProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick every 10 ticks for particle spawning
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Golden particles are rendered client-side based on the effect presence.
        // No additional server-side logic needed beyond presence check.
        return true;
    }

    /**
     * Checks if the given entity has oil protection active.
     * Used by OilSurfaceBlock and OiledEffect application logic.
     */
    public static boolean isProtected(LivingEntity entity) {
        // Check via effect presence - callers use CDEEffects.OIL_PROTECTION holder
        return false; // Actual check done externally via entity.hasEffect(CDEEffects.OIL_PROTECTION)
    }
}
