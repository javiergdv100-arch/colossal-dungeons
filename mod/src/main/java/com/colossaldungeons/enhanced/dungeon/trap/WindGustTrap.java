package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Wind Gust Trap - Rhythmic edge gusts pushing toward void.
 * 
 * Mechanics:
 * - Powerful wind gusts push entities toward ledge/void at rhythmic intervals
 * - Shield reduces push force by 70%
 * - Honey prevents post-push sliding
 * - Blocks placed as railing prevent being pushed off
 * - Wind follows a rhythmic pattern (gust, pause, gust, pause)
 * 
 * Config values used:
 * - damage.amount: fall/void damage if pushed off
 * - damage.radius: gust zone radius
 * - timing.warningTicks: wind buildup before gust
 * - timing.activeTicks: gust active duration per cycle
 */
public class WindGustTrap extends AbstractTrap {

    private static final float GUST_FORCE = 0.4f;
    private static final float SHIELD_PUSH_REDUCTION = 0.70f;
    private static final int GUST_CYCLE_TICKS = 60;
    private static final int GUST_ACTIVE_TICKS = 20;

    private Vec3 pushDirection;
    private boolean railingPlaced;

    public WindGustTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.pushDirection = new Vec3(1, 0, 0); // Default push toward void
        this.railingPlaced = false;
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
        // Wind begins to whistle at the edges
        // Debris starts to move in gust direction
        checkForRailing(level);
    }

    @Override
    protected void arm(ServerLevel level) {
        // Full wind pressure building
    }

    @Override
    protected void trigger(ServerLevel level) {
        // First massive gust hits
    }

    @Override
    protected void damage(ServerLevel level) {
        // Rhythmic pattern: gust for GUST_ACTIVE_TICKS, then pause
        int cyclePosition = tickCounter % GUST_CYCLE_TICKS;
        boolean gustActive = cyclePosition < GUST_ACTIVE_TICKS;

        if (!gustActive) return;

        checkForRailing(level);
        if (railingPlaced) return;

        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());

        for (LivingEntity target : targets) {
            float pushForce = GUST_FORCE;

            // Shield reduces push by 70%
            if (target.isBlocking()) {
                pushForce *= (1.0f - SHIELD_PUSH_REDUCTION);
            }

            // Honey prevents post-push sliding
            if (target.hasEffect(MobEffects.SLOW_FALLING)) {
                pushForce *= 0.1f;
            }

            // Apply gust force
            target.push(
                pushDirection.x * pushForce,
                0.05, // Slight upward to lift off ground
                pushDirection.z * pushForce
            );

            // If pushed past the edge, apply fall/void damage
            if (target.position().distanceTo(Vec3.atCenterOf(position)) > config.damage().radius() + 2) {
                DamageSource fallSource = level.damageSources().fall();
                target.hurt(fallSource, config.damage().amount());
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        railingPlaced = false;
    }

    /**
     * Checks if players have placed blocks as railing to prevent push-off.
     */
    private void checkForRailing(ServerLevel level) {
        // Check for solid blocks at waist height in push direction
        int checkDistance = (int) config.damage().radius();
        for (int i = 1; i <= checkDistance; i++) {
            BlockPos railPos = position.offset(
                (int) (pushDirection.x * i), 1, (int) (pushDirection.z * i));
            BlockState blockState = level.getBlockState(railPos);
            if (blockState.isSolid()) {
                railingPlaced = true;
                return;
            }
        }
        railingPlaced = false;
    }

    /**
     * Sets the direction gusts push entities toward.
     */
    public void setPushDirection(Vec3 direction) {
        this.pushDirection = direction.normalize();
    }

    public Vec3 getPushDirection() {
        return pushDirection;
    }

    public boolean isRailingPlaced() {
        return railingPlaced;
    }
}
