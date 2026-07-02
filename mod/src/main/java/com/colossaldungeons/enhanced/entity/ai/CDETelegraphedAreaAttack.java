package com.colossaldungeons.enhanced.entity.ai;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.List;

/**
 * Custom behavior for telegraphed area-of-effect attacks used by bosses.
 *
 * Sequence:
 * 1. Warning phase: particles and sound telegraph the attack for windupTicks
 * 2. Damage phase: applies damage to all entities within radius
 *
 * Parameters:
 * - radius: the area of effect radius
 * - windupTicks: delay before damage is applied (allows players to dodge)
 * - damage: the amount of damage dealt
 *
 * @param <E> the entity type
 */
public class CDETelegraphedAreaAttack<E extends Mob & IBossPhase> extends ExtendedBehaviour<E> {

    private final float radius;
    private final int windupTicks;
    private final float damage;
    private int ticksRemaining;
    private boolean damageDealt;

    /**
     * Creates a new telegraphed area attack behavior.
     *
     * @param radius the area of effect radius in blocks
     * @param windupTicks the number of ticks to telegraph before dealing damage
     * @param damage the damage to deal to entities in range
     */
    public CDETelegraphedAreaAttack(float radius, int windupTicks, float damage) {
        this.radius = radius;
        this.windupTicks = windupTicks;
        this.damage = damage;
        this.ticksRemaining = 0;
        this.damageDealt = false;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        // Only start if there are valid targets nearby
        AABB searchBox = entity.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            e -> e != entity && !e.isSpectator() && entity.getSensing().hasLineOfSight(e));
        return !targets.isEmpty();
    }

    @Override
    protected void start(ServerLevel level, E entity, long gameTime) {
        ticksRemaining = windupTicks;
        damageDealt = false;

        // Play warning sound
        level.playSound(null, entity.blockPosition(),
            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        ticksRemaining--;

        // Spawn warning particles during windup
        if (ticksRemaining > 0) {
            spawnWarningParticles(level, entity);
        }

        // Deal damage when windup completes
        if (ticksRemaining <= 0 && !damageDealt) {
            dealAreaDamage(level, entity);
            damageDealt = true;
        }
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        return !damageDealt;
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        ticksRemaining = 0;
        damageDealt = false;
    }

    /**
     * Spawns warning particles around the entity to telegraph the attack.
     */
    private void spawnWarningParticles(ServerLevel level, E entity) {
        double x = entity.getX();
        double y = entity.getY() + 0.1;
        double z = entity.getZ();

        // Spawn ring of particles at the edge of the attack radius
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2.0 / 16) * i;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.DUST_PLUME, px, y, pz, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /**
     * Deals damage to all living entities within the attack radius.
     */
    private void dealAreaDamage(ServerLevel level, E entity) {
        AABB damageBox = entity.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageBox,
            e -> e != entity && !e.isSpectator());

        DamageSource source = entity.damageSources().mobAttack(entity);

        for (LivingEntity target : targets) {
            double distance = entity.distanceTo(target);
            if (distance <= radius) {
                // Damage falls off slightly with distance
                float effectiveDamage = damage * (1.0f - (float)(distance / (radius * 2.0)));
                target.hurt(source, effectiveDamage);
            }
        }

        // Impact sound
        level.playSound(null, entity.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.7f);

        // Impact particles
        level.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(),
            8, radius * 0.5, 0.5, radius * 0.5, 0.0);
    }
}
