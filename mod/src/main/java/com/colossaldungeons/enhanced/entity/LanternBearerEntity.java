package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Lantern Bearer - Hunched servant with a lantern that blinds via mirror reflections.
 *
 * Carries a lantern whose light, reflected in mirrors, applies Blindness to players
 * in line of sight. Extinguishing its lantern renders it harmless and darkens the room.
 *
 * Stats: 22 HP, 2 armor, 5 damage + blindness effect.
 * Size: 0.6 x 1.6 blocks (hunched humanoid).
 *
 * Behavior:
 * - Reflected lantern light applies brief Blindness in line of sight.
 * - Seeks mirrors to amplify its radiance.
 * - Extinguishing the lantern (water/arrow) weakens it and changes room lighting.
 * - Water bucket extinguishes lantern instantly from range.
 * - Snowball impacts extinguish lantern temporarily (5 seconds).
 * - Shield blocks the Blindness effect if held up in line of sight.
 *
 * Drops: Silver lantern (portable light source).
 */
public class LanternBearerEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> LANTERN_LIT =
        SynchedEntityData.defineId(LanternBearerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> NEAR_MIRROR =
        SynchedEntityData.defineId(LanternBearerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double BLINDNESS_RANGE = 10.0;
    private static final double AMPLIFIED_BLINDNESS_RANGE = 16.0;
    private static final int BLINDNESS_DURATION = 40; // 2 seconds
    private static final int AMPLIFIED_BLINDNESS_DURATION = 80; // 4 seconds
    private static final int BLIND_COOLDOWN = 60; // 3 seconds between blind applications
    private static final int LANTERN_RELIGHT_TIME = 100; // 5 seconds for temp extinguish

    private int blindCooldown = 0;
    private int relightTimer = 0;

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lantern_bearer.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.lantern_bearer.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.lantern_bearer.attack");
    private static final RawAnimation FLASH = RawAnimation.begin().thenPlay("animation.lantern_bearer.flash");
    private static final RawAnimation EXTINGUISHED = RawAnimation.begin().thenLoop("animation.lantern_bearer.extinguished");
    private static final RawAnimation RELIGHT = RawAnimation.begin().thenPlay("animation.lantern_bearer.relight");

    public LanternBearerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Lantern Bearer.
     * 22 HP, 2 armor, 5 damage, 0.24 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 22.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LANTERN_LIT, true);
        builder.define(NEAR_MIRROR, false);
    }

    public boolean isLanternLit() {
        return this.entityData.get(LANTERN_LIT);
    }

    private void setLanternLit(boolean lit) {
        this.entityData.set(LANTERN_LIT, lit);
    }

    public boolean isNearMirror() {
        return this.entityData.get(NEAR_MIRROR);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Handle blindness cooldown
            if (blindCooldown > 0) blindCooldown--;

            // Handle lantern relight timer
            if (!isLanternLit() && relightTimer > 0) {
                relightTimer--;
                if (relightTimer <= 0) {
                    setLanternLit(true);
                }
            }

            // Check for nearby mirror surfaces
            boolean nearMirror = this.level().getBlockStates(this.getBoundingBox().inflate(3.0))
                .anyMatch(state -> state.is(net.minecraft.tags.BlockTags.IMPERMEABLE));
            this.entityData.set(NEAR_MIRROR, nearMirror);

            // Apply blindness to players in line of sight
            if (isLanternLit() && blindCooldown <= 0) {
                applyBlindnessToNearbyPlayers();
            }
        }
    }

    /**
     * Applies Blindness effect to players in line of sight of the lantern light.
     */
    private void applyBlindnessToNearbyPlayers() {
        double range = isNearMirror() ? AMPLIFIED_BLINDNESS_RANGE : BLINDNESS_RANGE;
        int duration = isNearMirror() ? AMPLIFIED_BLINDNESS_DURATION : BLINDNESS_DURATION;

        AABB searchBox = this.getBoundingBox().inflate(range);
        List<Player> players = this.level().getEntitiesOfClass(
            Player.class, searchBox,
            player -> !player.isSpectator() && this.getSensing().hasLineOfSight(player)
        );

        for (Player player : players) {
            // Shield blocks the blindness
            if (player.isBlocking()) continue;

            double dist = this.distanceTo(player);
            if (dist <= range) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                blindCooldown = BLIND_COOLDOWN;
            }
        }
    }

    /**
     * Extinguishes the lantern permanently (water bucket hit).
     */
    public void extinguishLantern(boolean permanent) {
        setLanternLit(false);
        if (!permanent) {
            relightTimer = LANTERN_RELIGHT_TIME;
        }
        // Reduce damage when lantern is out
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Projectile hits can extinguish the lantern
        if (source.isIndirect() && isLanternLit()) {
            // Snowball temporarily extinguishes
            extinguishLantern(false);
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && isLanternLit()) {
            // Melee attacks also apply brief blindness
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
        }
        return hit;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (!isLanternLit()) {
                return state.setAndContinue(EXTINGUISHED);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));

        // Flash controller for blindness application
        controllers.add(new AnimationController<>(this, "flash", 0, state -> {
            if (isLanternLit() && blindCooldown == BLIND_COOLDOWN - 1) {
                return state.setAndContinue(FLASH);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
