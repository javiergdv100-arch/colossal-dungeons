package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Swinging Blade Trap - Pendulum system with various blade types.
 * 
 * Mechanics:
 * - Pendulum weapons: axes, maces, hammers, spiked balls, blades, logs
 * - Shield blocks impact damage completely
 * - Fishing rod hooks blade and pauses it briefly (5 seconds)
 * - Water reduces pendulum speed (less damage, more reaction time)
 * - Swing arc determines damage timing
 * 
 * Config values used:
 * - damage.amount: blade/impact damage per hit
 * - damage.radius: swing arc reach
 * - timing.warningTicks: pendulum wind-up time
 * - timing.activeTicks: active swing duration
 */
public class SwingingBladeTrap extends AbstractTrap {

    private static final float KNOCKBACK_FORCE = 1.5f;
    private static final int HOOK_PAUSE_TICKS = 100; // 5 seconds
    private static final float WATER_SPEED_REDUCTION = 0.6f;
    private static final int SWING_CYCLE_TICKS = 30;

    private boolean hookedByRod;
    private int hookPauseTimer;
    private boolean inWater;

    public SwingingBladeTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.hookedByRod = false;
        this.hookPauseTimer = 0;
        this.inWater = false;
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
        // Creaking chains, pendulum begins to sway
        // Check for water presence
        inWater = level.getBlockState(position).is(Blocks.WATER) ||
                  level.getBlockState(position.below()).is(Blocks.WATER);
    }

    @Override
    protected void arm(ServerLevel level) {
        // Pendulum at full swing momentum
        // Blade gleams in available light
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Full swing begins - whooshing sound
    }

    @Override
    protected void damage(ServerLevel level) {
        // Handle fishing rod hook pause
        if (hookedByRod) {
            hookPauseTimer--;
            if (hookPauseTimer <= 0) {
                hookedByRod = false;
            }
            return; // Paused - no damage
        }

        // Calculate swing cycle - damage only on swing-through
        int cycleLength = inWater ?
            (int) (SWING_CYCLE_TICKS / WATER_SPEED_REDUCTION) : SWING_CYCLE_TICKS;
        int cyclePosition = tickCounter % cycleLength;

        // Blade passes through center at specific points in cycle
        boolean bladePassingThrough = cyclePosition < 5 ||
            (cyclePosition > (cycleLength / 2) - 3 && cyclePosition < (cycleLength / 2) + 3);

        if (!bladePassingThrough) return;

        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource slashSource = level.damageSources().generic();

        float damageAmount = config.damage().amount();
        if (inWater) {
            damageAmount *= WATER_SPEED_REDUCTION;
        }

        for (LivingEntity target : targets) {
            if (target.isBlocking()) {
                // Shield completely blocks blade impact
                continue;
            }

            target.hurt(slashSource, damageAmount);
            // Knockback from blade impact
            Vec3 knockDir = target.position().subtract(Vec3.atCenterOf(position)).normalize();
            target.push(knockDir.x * KNOCKBACK_FORCE, 0.2, knockDir.z * KNOCKBACK_FORCE);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        hookedByRod = false;
        hookPauseTimer = 0;
        inWater = false;
    }

    /**
     * Called when a fishing rod hooks the blade.
     * Pauses the swing for 5 seconds.
     */
    public void hookWithFishingRod() {
        this.hookedByRod = true;
        this.hookPauseTimer = HOOK_PAUSE_TICKS;
    }
}
