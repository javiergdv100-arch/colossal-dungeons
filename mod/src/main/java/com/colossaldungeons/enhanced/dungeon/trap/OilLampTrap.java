package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Oil Lamp Trap - Ceiling lamp with detection zone (3 blocks).
 * 
 * Mechanics:
 * - 10% chance to fall per player entry into detection zone
 * - Ignites oil below on impact (chain reaction with OilSurfaceTrap)
 * - Crossbow bolt or snowball can trigger premature fall
 * - Falls from ceiling dealing impact damage + fire damage
 * 
 * Config values used:
 * - damage.amount: impact damage on fall
 * - activator.range: detection zone size (default 3 blocks)
 * - timing.warningTicks: flicker warning before fall
 * - timing.activeTicks: burn duration after impact
 */
public class OilLampTrap extends AbstractTrap {

    private static final double FALL_CHANCE = 0.10;
    private static final int DETECTION_ZONE_BLOCKS = 3;
    private static final float IMPACT_DAMAGE_MULTIPLIER = 1.5f;

    private boolean lampFallen;
    private boolean prematurelyTriggered;

    public OilLampTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.lampFallen = false;
        this.prematurelyTriggered = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (lampFallen) return false;
        // Check for players in detection zone below the ceiling lamp
        BlockPos detectionCenter = position.below(2);
        AABB detectionZone = new AABB(detectionCenter).inflate(DETECTION_ZONE_BLOCKS);
        List<Player> players = level.getEntitiesOfClass(Player.class, detectionZone);
        return !players.isEmpty() || prematurelyTriggered;
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        if (prematurelyTriggered) return true;
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Lamp begins flickering - warning sign
        // Play subtle creaking/chain sound
    }

    @Override
    protected void arm(ServerLevel level) {
        // Lamp is unstable - ready to fall
        // Chains are visibly straining
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Roll for fall chance (10% per entry) or forced by projectile
        if (prematurelyTriggered || level.random.nextFloat() < FALL_CHANCE) {
            lampFallen = true;
            // Lamp detaches and falls
        } else {
            // Lamp stabilizes - reset to armed for next check
            state = TrapState.COOLDOWN;
            tickCounter = 0;
        }
    }

    @Override
    protected void damage(ServerLevel level) {
        if (!lampFallen) return;

        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource impactSource = level.damageSources().fall();
        DamageSource fireSource = level.damageSources().onFire();

        for (LivingEntity target : targets) {
            // Impact damage from falling lamp
            target.hurt(impactSource, config.damage().amount() * IMPACT_DAMAGE_MULTIPLIER);
            // Fire damage from burning oil spread
            target.hurt(fireSource, config.damage().amount());
            // Set entities on fire
            target.setRemainingFireTicks(config.timing().activeTicks());
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Lamp is destroyed on the ground - this trap does not repeat
        // Fire eventually burns out
        lampFallen = false;
        prematurelyTriggered = false;
    }

    /**
     * Called when a projectile (crossbow bolt or snowball) hits the lamp.
     * Forces premature triggering.
     */
    public void onProjectileHit() {
        this.prematurelyTriggered = true;
    }
}
