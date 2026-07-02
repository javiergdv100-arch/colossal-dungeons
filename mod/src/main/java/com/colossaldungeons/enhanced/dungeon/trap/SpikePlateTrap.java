package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Concrete trap implementation: a spike plate that extends from the floor.
 * 
 * Behavior:
 * - Prepares when players enter the room
 * - Arms immediately after preparation
 * - Triggers when an entity steps on the plate (pressure activation)
 * - Deals piercing damage to all entities on the plate
 * - Resets after cooldown and can repeat
 */
public class SpikePlateTrap extends AbstractTrap {

    public SpikePlateTrap(BlockPos position, TrapConfig config) {
        super(position, config);
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        // Prepare when any living entity is within detection range
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        // Arm immediately after preparation delay
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Play warning sound / show particles at the plate position
        // Client-side effects would be triggered via network packet
    }

    @Override
    protected void arm(ServerLevel level) {
        // Spikes are ready to extend - slight visual cue (cracks in floor)
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Spikes extend from the floor - play activation sound
        // Send network packet for client-side VFX
    }

    @Override
    protected void damage(ServerLevel level) {
        // Deal damage to all entities standing on the spike plate
        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource damageSource = level.damageSources().generic();

        for (LivingEntity target : targets) {
            target.hurt(damageSource, config.damage().amount());
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Spikes retract back into the floor
        // Play retraction sound, reset visual state
    }
}
