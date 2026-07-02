package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Rolling Boulder Trap - Large mass rolling by gravity following slope.
 * 
 * Mechanics:
 * - Boulder rolls down slope following gravity
 * - Fishing rod hooks release mechanism for early trigger
 * - Shield reduces knockback from boulder impact
 * - Water slows boulder on wet surfaces
 * - TNT destroys stone/organic versions of boulder
 * - Deals massive crush + knockback damage
 * 
 * Config values used:
 * - damage.amount: boulder crush damage
 * - damage.radius: boulder impact radius
 * - timing.warningTicks: rumbling before boulder release
 * - timing.activeTicks: rolling duration
 */
public class RollingBoulderTrap extends AbstractTrap {

    private static final float KNOCKBACK_STRENGTH = 3.0f;
    private static final float SHIELD_KNOCKBACK_REDUCTION = 0.5f;
    private static final float WATER_SPEED_REDUCTION = 0.5f;

    private Direction rollDirection;
    private int boulderPosition;
    private boolean destroyedByTNT;
    private boolean releasedByFishingRod;

    public RollingBoulderTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.rollDirection = Direction.SOUTH;
        this.boulderPosition = 0;
        this.destroyedByTNT = false;
        this.releasedByFishingRod = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (destroyedByTNT) return false;
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty() || releasedByFishingRod;
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        if (releasedByFishingRod) return true;
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Rumbling from above - boulder mechanism creaks
        // Determine roll direction based on slope
        determineRollDirection(level);
    }

    @Override
    protected void arm(ServerLevel level) {
        // Boulder is ready to release
        boulderPosition = 0;
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Boulder released - begins rolling
        // Loud crash as it starts moving
    }

    @Override
    protected void damage(ServerLevel level) {
        if (destroyedByTNT) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // Advance boulder position
        boolean onWetSurface = isOnWetSurface(level);
        int advanceRate = onWetSurface ? 10 : 5; // Slower on wet surfaces

        if (tickCounter % advanceRate == 0) {
            boulderPosition++;
        }

        // Check for entities at current boulder position
        BlockPos boulderPos = position.relative(rollDirection, boulderPosition);
        AABB boulderArea = new AABB(boulderPos).inflate(config.damage().radius());
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, boulderArea);

        DamageSource crushSource = level.damageSources().generic();

        for (LivingEntity target : targets) {
            target.hurt(crushSource, config.damage().amount());

            // Apply knockback in roll direction
            float knockback = KNOCKBACK_STRENGTH;
            if (target.isBlocking()) {
                knockback *= SHIELD_KNOCKBACK_REDUCTION;
            }

            Vec3 knockDir = Vec3.atLowerCornerOf(rollDirection.getNormal()).scale(knockback);
            target.push(knockDir.x, 0.3, knockDir.z);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        boulderPosition = 0;
        releasedByFishingRod = false;
        destroyedByTNT = false;
    }

    /**
     * Determines the direction the boulder will roll based on terrain slope.
     */
    private void determineRollDirection(ServerLevel level) {
        // Check each horizontal direction for downward slope
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos ahead = position.relative(dir);
            if (level.getBlockState(ahead.below()).isAir()) {
                rollDirection = dir;
                return;
            }
        }
        rollDirection = Direction.SOUTH; // Default
    }

    /**
     * Checks if the boulder path has water on it.
     */
    private boolean isOnWetSurface(ServerLevel level) {
        BlockPos boulderPos = position.relative(rollDirection, boulderPosition);
        return level.getBlockState(boulderPos).is(Blocks.WATER) ||
               level.getBlockState(boulderPos.below()).is(Blocks.WATER);
    }

    /**
     * Called when fishing rod hooks the release mechanism.
     */
    public void releaseWithFishingRod() {
        this.releasedByFishingRod = true;
    }

    /**
     * Called when TNT destroys the boulder.
     */
    public void destroyWithTNT() {
        this.destroyedByTNT = true;
    }

    public Direction getRollDirection() {
        return rollDirection;
    }
}
