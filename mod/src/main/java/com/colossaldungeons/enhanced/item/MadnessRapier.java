package com.colossaldungeons.enhanced.item;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Madness Rapier - thin blade that drains sanity on hit.
 * 
 * Stats:
 * - Tier: DIAMOND, +5 attack damage, -2.0 attack speed
 * - On hit: applies SanityDrain effect to target
 * - Right-click ability: lunging strike (teleport 3 blocks forward + attack)
 * - Cooldown: 30 ticks (1.5 seconds)
 */
public class MadnessRapier extends CDEWeaponItem {

    /** Lunge distance in blocks. */
    private static final double LUNGE_DISTANCE = 3.0;

    /** Lunge attack damage. */
    private static final float LUNGE_DAMAGE = 7.0f;

    /** Sanity drain duration on hit (5 seconds). */
    private static final int SANITY_DRAIN_DURATION = 100;

    /** Cooldown in ticks. */
    private static final int COOLDOWN = 30;

    public MadnessRapier() {
        super(Tiers.DIAMOND,
            new Properties().attributes(createAttributes(Tiers.DIAMOND, 5, -2.0f)),
            COOLDOWN);
    }

    @Override
    protected void onHitEffect(LivingEntity target, ServerPlayer player, ItemStack stack) {
        // Apply Sanity Drain to target on hit
        target.addEffect(new MobEffectInstance(
            CDEEffects.SANITY_DRAIN, SANITY_DRAIN_DURATION, 0,
            false, true, true
        ));
    }

    @Override
    protected boolean performAbility(Level level, ServerPlayer player, ItemStack stack) {
        // Lunging strike: teleport 3 blocks forward and attack
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position();
        Vec3 targetPos = startPos.add(lookVec.scale(LUNGE_DISTANCE));

        // Teleport the player forward
        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        // Sound effect for the lunge
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.5f);

        // Attack entities at the landing position
        AABB attackBox = player.getBoundingBox().inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, attackBox,
            entity -> entity != player && entity.isAlive());

        boolean hitSomething = false;
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), LUNGE_DAMAGE);
            onHitEffect(target, player, stack);
            hitSomething = true;
        }

        if (hitSomething) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return true;
    }
}
