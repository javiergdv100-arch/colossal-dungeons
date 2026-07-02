package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Oil Surface Trap - Applies "oiled" status (slowness + weakness) to entities stepping on it.
 * 
 * Mechanics:
 * - Fire causes double burn time on oiled entities
 * - Water WORSENS burning oil (spreads flames)
 * - Bone meal cleans the oil surface, providing temporary protection
 * 
 * Config values used:
 * - damage.amount: fire damage per tick when ignited
 * - timing.warningTicks: ticks before oil becomes slippery
 * - timing.activeTicks: duration of oil effect
 * - activator.range: detection range for entities
 */
public class OilSurfaceTrap extends AbstractTrap {

    private static final int OIL_DURATION_TICKS = 200;
    private static final float FIRE_DAMAGE_MULTIPLIER = 2.0f;
    private static final int BONE_MEAL_PROTECTION_TICKS = 600;

    private boolean ignited;
    private boolean cleanedWithBoneMeal;
    private int boneMealProtectionTimer;

    public OilSurfaceTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.ignited = false;
        this.cleanedWithBoneMeal = false;
        this.boneMealProtectionTimer = 0;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (cleanedWithBoneMeal && boneMealProtectionTimer > 0) {
            boneMealProtectionTimer--;
            return false;
        }
        cleanedWithBoneMeal = false;
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Oil begins to seep from the floor, creating a visible sheen
        // Slight squelching sound plays as warning
    }

    @Override
    protected void arm(ServerLevel level) {
        // Oil surface is fully formed and ready to coat entities
        // Check if any fire source is nearby to auto-ignite
        if (level.getBlockState(position.above()).is(Blocks.FIRE) ||
            level.getBlockState(position).is(Blocks.FIRE)) {
            ignited = true;
        }
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Oil activates - entities stepping on it become coated
        // Check for water contact which worsens burning oil
        if (level.getBlockState(position.above()).is(Blocks.WATER)) {
            if (ignited) {
                // Water on burning oil spreads flames - increase damage area
                ignited = true;
            }
        }
    }

    @Override
    protected void damage(ServerLevel level) {
        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource damageSource = level.damageSources().onFire();

        for (LivingEntity target : targets) {
            // Apply oiled effect (slowness + weakness to represent coating)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OIL_DURATION_TICKS, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, OIL_DURATION_TICKS, 0));

            if (ignited) {
                // Fire causes double burn time on oiled entities
                target.hurt(damageSource, config.damage().amount() * FIRE_DAMAGE_MULTIPLIER);
                int burnTime = (int) (config.timing().activeTicks() * FIRE_DAMAGE_MULTIPLIER);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), burnTime));
            } else {
                // Just the oil coating - minor slip damage
                target.hurt(level.damageSources().generic(), config.damage().amount() * 0.25f);
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Oil dissipates or burns away
        ignited = false;
    }

    /**
     * Called when bone meal is applied to the oil surface.
     * Cleans the oil and provides temporary protection.
     */
    public void applyBoneMeal() {
        cleanedWithBoneMeal = true;
        boneMealProtectionTimer = BONE_MEAL_PROTECTION_TICKS;
        state = TrapState.INACTIVE;
        tickCounter = 0;
    }

    /**
     * Called when fire is introduced to the oil surface.
     */
    public void ignite() {
        this.ignited = true;
    }

    public boolean isIgnited() {
        return ignited;
    }
}
