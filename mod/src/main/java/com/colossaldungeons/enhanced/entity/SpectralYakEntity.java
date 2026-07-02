package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Spectral Yak - A ghostly mountain beast from the Veiled Peak dungeon.
 *
 * Performs telegraphed line charges with huge knockback.
 * Self-stuns if it misses and hits a wall. Shield blocks charge and stuns it.
 * Travels in small herds (spawns in groups of 2-3).
 *
 * Stats: 44 HP, 3 armor, 0.3 speed, 9 charge damage.
 */
public class SpectralYakEntity extends CDEGeoEntity implements GeoEntity {

    public enum YakState {
        IDLE,
        TELEGRAPHING,
        CHARGING,
        STUNNED
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(SpectralYakEntity.class, EntityDataSerializers.INT);

    private static final int TELEGRAPH_DURATION = 30; // 1.5 seconds telegraph
    private static final int CHARGE_MAX_DURATION = 40; // 2 seconds max charge
    private static final int STUN_DURATION = 60; // 3 seconds stunned
    private static final double CHARGE_SPEED = 1.2;
    private static final double CHARGE_KNOCKBACK = 3.0;
    private static final int CHARGE_COOLDOWN = 120; // 6 seconds between charges

    private int stateTimer = 0;
    private int chargeCooldown = 0;
    private Vec3 chargeDirection = Vec3.ZERO;
    private boolean chargeHitTarget = false;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.spectral_yak.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.spectral_yak.walk");
    private static final RawAnimation TELEGRAPH = RawAnimation.begin().thenPlay("animation.spectral_yak.telegraph");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.spectral_yak.charge");
    private static final RawAnimation STUNNED_ANIM = RawAnimation.begin().thenLoop("animation.spectral_yak.stunned");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.spectral_yak.attack");

    public SpectralYakEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Spectral Yak.
     * 44 HP, 3 armor, 0.3 speed, 9 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 44.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, YakState.IDLE.ordinal());
    }

    public YakState getYakState() {
        return YakState.values()[this.entityData.get(STATE)];
    }

    private void setYakState(YakState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (chargeCooldown > 0) chargeCooldown--;

            YakState currentState = getYakState();

            switch (currentState) {
                case IDLE -> {
                    // Initiate charge if target in range
                    LivingEntity target = this.getTarget();
                    if (target != null && chargeCooldown <= 0 && this.distanceTo(target) > 4.0
                        && this.distanceTo(target) < 20.0) {
                        setYakState(YakState.TELEGRAPHING);
                        stateTimer = TELEGRAPH_DURATION;
                        chargeDirection = target.position().subtract(this.position()).normalize();
                    }
                }
                case TELEGRAPHING -> {
                    stateTimer--;
                    // Face the charge direction
                    if (stateTimer <= 0) {
                        setYakState(YakState.CHARGING);
                        stateTimer = CHARGE_MAX_DURATION;
                        chargeHitTarget = false;

                        this.level().playSound(null, this.blockPosition(),
                            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.8f);
                    }
                }
                case CHARGING -> {
                    stateTimer--;

                    // Move in charge direction
                    this.setDeltaMovement(chargeDirection.scale(CHARGE_SPEED)
                        .add(0, this.getDeltaMovement().y, 0));

                    // Check for wall collision
                    if (this.horizontalCollision) {
                        // Hit wall - stun self
                        setYakState(YakState.STUNNED);
                        stateTimer = STUN_DURATION;
                        this.setDeltaMovement(Vec3.ZERO);
                        this.level().playSound(null, this.blockPosition(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0f, 0.5f);
                        break;
                    }

                    // Check for target collision during charge
                    LivingEntity target = this.getTarget();
                    if (target != null && this.distanceTo(target) < 2.0 && !chargeHitTarget) {
                        chargeHitTarget = true;

                        // Check if target is blocking with shield
                        if (target instanceof Player player && player.isBlocking()) {
                            // Shield blocks charge and stuns the yak
                            setYakState(YakState.STUNNED);
                            stateTimer = STUN_DURATION;
                            this.setDeltaMovement(Vec3.ZERO);
                            this.level().playSound(null, this.blockPosition(),
                                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0f, 1.0f);
                        } else {
                            // Hit the target with charge damage and knockback
                            target.hurt(this.damageSources().mobAttack(this),
                                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                            Vec3 knockback = chargeDirection.scale(CHARGE_KNOCKBACK);
                            target.push(knockback.x, 0.5, knockback.z);
                        }
                    }

                    // Charge expired
                    if (stateTimer <= 0) {
                        setYakState(YakState.IDLE);
                        chargeCooldown = CHARGE_COOLDOWN;
                    }
                }
                case STUNNED -> {
                    stateTimer--;
                    this.setDeltaMovement(Vec3.ZERO);
                    if (stateTimer <= 0) {
                        setYakState(YakState.IDLE);
                        chargeCooldown = CHARGE_COOLDOWN;
                    }
                }
            }
        }
    }

    @Override
    public boolean isNoAi() {
        YakState state = getYakState();
        if (state == YakState.TELEGRAPHING || state == YakState.CHARGING || state == YakState.STUNNED) {
            return true;
        }
        return super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            YakState yakState = getYakState();
            return switch (yakState) {
                case IDLE -> {
                    if (state.isMoving()) yield state.setAndContinue(WALK);
                    yield state.setAndContinue(IDLE);
                }
                case TELEGRAPHING -> state.setAndContinue(TELEGRAPH);
                case CHARGING -> state.setAndContinue(CHARGE);
                case STUNNED -> state.setAndContinue(STUNNED_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && getYakState() == YakState.IDLE) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
