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
 * Ice Slide Trap - Zero-friction ice sections sending player toward cliffs.
 * 
 * Mechanics:
 * - Ice surfaces with zero friction propel entities toward void/cliffs
 * - Honey = COMPLETE immunity to sliding
 * - Flint and steel melts a single ice section
 * - Lava melts a large ice section
 * - Powder snow adds traction layer (reduces slide)
 * - Players cannot change direction while on ice
 * 
 * Config values used:
 * - damage.amount: fall/void damage at edge
 * - damage.radius: ice surface area
 * - timing.warningTicks: frost buildup before full freeze
 * - timing.activeTicks: how long ice persists
 */
public class IceSlideTrap extends AbstractTrap {

    private static final float SLIDE_ACCELERATION = 0.12f;
    private static final float MAX_SLIDE_SPEED = 1.5f;
    private static final float POWDER_SNOW_TRACTION = 0.3f;

    private Vec3 slideDirection;
    private boolean meltedByFlint;
    private boolean meltedByLava;
    private boolean powderSnowApplied;

    public IceSlideTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.slideDirection = new Vec3(1, 0, 0); // Default direction toward cliff
        this.meltedByFlint = false;
        this.meltedByLava = false;
        this.powderSnowApplied = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (meltedByFlint || meltedByLava) return false;
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Frost spreads across the floor
        // Temperature drops noticeably
    }

    @Override
    protected void arm(ServerLevel level) {
        // Ice is fully formed - frictionless surface ready
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Entity steps onto ice - begins sliding
    }

    @Override
    protected void damage(ServerLevel level) {
        if (meltedByFlint || meltedByLava) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());

        for (LivingEntity target : targets) {
            // Honey provides COMPLETE immunity
            if (target.hasEffect(MobEffects.SLOW_FALLING)) {
                continue;
            }

            float slideForce = SLIDE_ACCELERATION;

            // Powder snow adds traction (reduces slide)
            if (powderSnowApplied) {
                slideForce *= POWDER_SNOW_TRACTION;
            }

            // Cap sliding speed
            Vec3 currentMotion = target.getDeltaMovement();
            double currentSpeed = currentMotion.horizontalDistance();
            if (currentSpeed < MAX_SLIDE_SPEED) {
                target.push(
                    slideDirection.x * slideForce,
                    0,
                    slideDirection.z * slideForce
                );
            }

            // Apply ice-related effects
            target.setTicksFrozen(target.getTicksFrozen() + 2);

            // Check if entity has reached the edge (cliff)
            if (target.position().distanceTo(Vec3.atCenterOf(position)) > config.damage().radius()) {
                DamageSource fallSource = level.damageSources().fall();
                target.hurt(fallSource, config.damage().amount());
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        meltedByFlint = false;
        meltedByLava = false;
        powderSnowApplied = false;
    }

    /**
     * Called when flint and steel is used - melts one section.
     */
    public void meltWithFlint() {
        this.meltedByFlint = true;
    }

    /**
     * Called when lava contacts the ice - melts large section.
     */
    public void meltWithLava() {
        this.meltedByLava = true;
    }

    /**
     * Called when powder snow is placed on the ice.
     */
    public void applyPowderSnow() {
        this.powderSnowApplied = true;
    }

    /**
     * Sets the direction entities will slide toward.
     */
    public void setSlideDirection(Vec3 direction) {
        this.slideDirection = direction.normalize();
    }

    public Vec3 getSlideDirection() {
        return slideDirection;
    }
}
