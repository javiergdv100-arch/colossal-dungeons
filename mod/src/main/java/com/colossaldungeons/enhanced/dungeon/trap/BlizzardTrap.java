package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Blizzard Trap - Sudden blinding snow reducing vision and pushing in one direction.
 * 
 * Mechanics:
 * - Sudden whiteout reduces vision to near-zero
 * - Strong wind pushes entities in one direction
 * - Shield reduces push force by 70%
 * - Torches create 3-block visibility bubbles
 * - Honey prevents ice sliding on frozen floor
 * - Freezing damage over time
 * 
 * Config values used:
 * - damage.amount: freezing damage per tick
 * - damage.radius: blizzard area radius
 * - timing.warningTicks: temperature drop warning
 * - timing.activeTicks: blizzard duration
 */
public class BlizzardTrap extends AbstractTrap {

    private static final float PUSH_FORCE = 0.15f;
    private static final float SHIELD_PUSH_REDUCTION = 0.70f;
    private static final int TORCH_VISIBILITY_RADIUS = 3;
    private static final int FREEZE_BUILDUP_TICKS = 40;

    private Vec3 windDirection;

    public BlizzardTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.windDirection = new Vec3(1, 0, 0); // Default eastward
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Temperature drops rapidly
        // Frost begins forming on surfaces
        // Wind picks up gradually

        // Randomize wind direction
        float angle = level.random.nextFloat() * (float) Math.PI * 2;
        windDirection = new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    @Override
    protected void arm(ServerLevel level) {
        // Blizzard is imminent - ice crystals visible in air
        // Visibility begins to drop
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Full whiteout - blinding snow engulfs the area
        // Deafening wind sound
    }

    @Override
    protected void damage(ServerLevel level) {
        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());
        DamageSource freezeSource = level.damageSources().freeze();

        for (LivingEntity target : targets) {
            // Apply blindness (reduced vision)
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));

            // Freezing damage
            if (tickCounter % FREEZE_BUILDUP_TICKS == 0) {
                target.hurt(freezeSource, config.damage().amount());
                target.setTicksFrozen(target.getTicksFrozen() + 40);
            }

            // Wind push force
            float pushForce = PUSH_FORCE;
            if (target.isBlocking()) {
                pushForce *= (1.0f - SHIELD_PUSH_REDUCTION);
            }

            // Honey prevents ice sliding (check for slow falling effect)
            if (target.hasEffect(MobEffects.SLOW_FALLING)) {
                pushForce = 0;
            }

            target.push(
                windDirection.x * pushForce,
                0,
                windDirection.z * pushForce
            );

            // Apply slowness from cold
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Blizzard subsides - visibility returns
        // Temperature normalizes
    }

    /**
     * Sets the wind push direction for this blizzard instance.
     */
    public void setWindDirection(Vec3 direction) {
        this.windDirection = direction.normalize();
    }

    public Vec3 getWindDirection() {
        return windDirection;
    }
}
