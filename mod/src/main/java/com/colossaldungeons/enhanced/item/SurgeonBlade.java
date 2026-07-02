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
import net.minecraft.world.phys.Vec3;

/**
 * Surgeon Blade - precision dagger with fast attacks and hemorrhage on hit.
 * 
 * Stats:
 * - Tier: DIAMOND, +4 attack damage, -1.8 attack speed (fast)
 * - On hit: applies Hemorrhage effect (stacking up to 3)
 * - Right-click ability: backstab (3x damage from behind target)
 * - Cooldown: 40 ticks (2 seconds)
 */
public class SurgeonBlade extends CDEWeaponItem {

    /** Backstab damage multiplier. */
    private static final float BACKSTAB_MULTIPLIER = 3.0f;

    /** Backstab range in blocks. */
    private static final double BACKSTAB_RANGE = 3.0;

    /** Hemorrhage duration in ticks (8 seconds). */
    private static final int HEMORRHAGE_DURATION = 160;

    /** Cooldown in ticks. */
    private static final int COOLDOWN = 40;

    public SurgeonBlade() {
        super(Tiers.DIAMOND,
            new Properties().attributes(createAttributes(Tiers.DIAMOND, 4, -1.8f)),
            COOLDOWN);
    }

    @Override
    protected void onHitEffect(LivingEntity target, ServerPlayer player, ItemStack stack) {
        // Apply Hemorrhage effect on hit (stacking)
        MobEffectInstance existing = target.getEffect(CDEEffects.HEMORRHAGE);
        int amplifier = (existing != null) ? Math.min(existing.getAmplifier() + 1, 2) : 0;

        target.addEffect(new MobEffectInstance(
            CDEEffects.HEMORRHAGE, HEMORRHAGE_DURATION, amplifier,
            false, true, true
        ));
    }

    @Override
    protected boolean performAbility(Level level, ServerPlayer player, ItemStack stack) {
        // Backstab: find the entity the player is looking at
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(BACKSTAB_RANGE));

        // Find entities in the path
        var entities = level.getEntitiesOfClass(LivingEntity.class,
            player.getBoundingBox().inflate(BACKSTAB_RANGE),
            entity -> entity != player && entity.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            double dist = entity.distanceTo(player);
            if (dist < closestDist && dist <= BACKSTAB_RANGE) {
                closestTarget = entity;
                closestDist = dist;
            }
        }

        if (closestTarget == null) return false;

        // Check if attacking from behind
        Vec3 targetLook = closestTarget.getLookAngle();
        Vec3 toTarget = closestTarget.position().subtract(player.position()).normalize();
        double dot = targetLook.dot(toTarget);

        float baseDamage = 4.0f;
        float damage;

        if (dot > 0.5) {
            // Behind the target - backstab!
            damage = baseDamage * BACKSTAB_MULTIPLIER;
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.5f);
        } else {
            // Not behind - still a strike but normal damage
            damage = baseDamage;
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        closestTarget.hurt(player.damageSources().playerAttack(player), damage);

        // Apply hemorrhage on backstab too
        onHitEffect(closestTarget, player, stack);

        return true;
    }
}
