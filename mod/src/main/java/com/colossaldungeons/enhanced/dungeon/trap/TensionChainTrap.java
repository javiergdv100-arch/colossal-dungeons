package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tension Chain Trap - High-speed chain sweeping across zone.
 * 
 * Mechanics:
 * - Chain sweeps horizontally at high speed in a fixed arc
 * - Shield blocks the chain swing completely
 * - Fishing rod can redirect the chain path
 * - Honey prevents drag if chain hooks onto player
 * - Chain can grab and drag entities on contact
 * 
 * Config values used:
 * - damage.amount: chain impact + slash damage
 * - damage.radius: sweep zone radius
 * - timing.warningTicks: chain tension buildup sound
 * - timing.activeTicks: sweep duration per cycle
 */
public class TensionChainTrap extends AbstractTrap {

    private static final float DRAG_FORCE = 2.0f;
    private static final float CHAIN_SPEED = 0.8f;
    private static final int SWEEP_INTERVAL_TICKS = 40;

    private boolean redirectedByRod;
    private float chainAngle;

    public TensionChainTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.redirectedByRod = false;
        this.chainAngle = 0;
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
        // Chain tension builds - metallic creaking sound
        // Vibration visible along the chain
        chainAngle = 0;
    }

    @Override
    protected void arm(ServerLevel level) {
        // Chain at maximum tension - about to release
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Chain sweeps - loud whooshing metallic sound
    }

    @Override
    protected void damage(ServerLevel level) {
        // Chain sweeps in arc
        chainAngle += CHAIN_SPEED;
        if (chainAngle > Math.PI * 2) {
            chainAngle = 0;
        }

        // Only deal damage during active sweep phases
        if (tickCounter % SWEEP_INTERVAL_TICKS > 10) return;

        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource chainSource = level.damageSources().generic();

        for (LivingEntity target : targets) {
            // Shield blocks chain impact
            if (target.isBlocking()) {
                continue;
            }

            target.hurt(chainSource, config.damage().amount());

            // Chain hooks and drags entity (unless honey protection)
            // Honey is checked by presence of slow falling effect (honey block interaction)
            if (!target.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                // Drag toward chain anchor point
                Vec3 dragDir = Vec3.atCenterOf(position).subtract(target.position()).normalize();
                target.push(dragDir.x * DRAG_FORCE, 0.1, dragDir.z * DRAG_FORCE);
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        redirectedByRod = false;
        chainAngle = 0;
    }

    /**
     * Called when a fishing rod redirects the chain.
     */
    public void redirectWithFishingRod() {
        this.redirectedByRod = true;
        // Chain redirects - changes sweep angle
        this.chainAngle += (float) Math.PI;
    }
}
