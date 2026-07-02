package com.colossaldungeons.enhanced.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Counterweight Hammer - heavy weapon with AoE slam ability.
 * 
 * Stats:
 * - Tier: NETHERITE, +7 attack damage, -3.0 attack speed (slow)
 * - Right-click ability: AoE slam dealing damage in 3-block radius
 * - Strong knockback on hit
 * - Cooldown: 60 ticks (3 seconds)
 */
public class CounterweightHammer extends CDEWeaponItem {

    /** AoE radius for slam ability. */
    private static final double SLAM_RADIUS = 3.0;

    /** Damage dealt by the slam ability. */
    private static final float SLAM_DAMAGE = 10.0f;

    /** Knockback strength of the slam. */
    private static final double SLAM_KNOCKBACK = 1.5;

    /** Cooldown in ticks. */
    private static final int COOLDOWN = 60;

    public CounterweightHammer() {
        super(Tiers.NETHERITE,
            new Properties().attributes(createAttributes(Tiers.NETHERITE, 7, -3.0f)),
            COOLDOWN);
    }

    @Override
    protected boolean performAbility(Level level, ServerPlayer player, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        // AoE slam: damage all entities in radius
        AABB aoe = player.getBoundingBox().inflate(SLAM_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aoe,
            entity -> entity != player && entity.isAlive());

        if (targets.isEmpty()) {
            // Still perform the slam for visual effect even if no targets
        }

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), SLAM_DAMAGE);

            // Knockback away from player
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0) {
                target.knockback(SLAM_KNOCKBACK, -dx / distance, -dz / distance);
            }
        }

        // Visual effects: ground slam particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
            player.getX(), player.getY(), player.getZ(),
            5, SLAM_RADIUS * 0.5, 0.2, SLAM_RADIUS * 0.5, 0.0);

        serverLevel.sendParticles(ParticleTypes.CLOUD,
            player.getX(), player.getY(), player.getZ(),
            20, SLAM_RADIUS * 0.5, 0.1, SLAM_RADIUS * 0.5, 0.05);

        // Sound
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.6f);

        return true;
    }
}
