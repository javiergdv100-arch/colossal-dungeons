package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Bronze Falcon - A bronze flying automaton from the Solar Palace.
 *
 * Dive-bombs from height with blinding flash pre-impact.
 * Fragile once grounded after missed dive (stunned 3s).
 * Crossbow hit in air (one critical shot derriba).
 * Shield blocks flash.
 *
 * Stats: 16 HP, 4 armor, 0.4 speed (flying), 7 damage dive.
 */
public class BronzeFalconEntity extends CDEGeoEntity implements GeoEntity {

    public enum FalconState {
        FLYING,
        DIVING,
        GROUNDED_STUNNED,
        RECOVERING
    }

    private static final EntityDataAccessor<Integer> FALCON_STATE =
        SynchedEntityData.defineId(BronzeFalconEntity.class, EntityDataSerializers.INT);

    private static final int STUN_DURATION = 60; // 3 seconds
    private static final int DIVE_COOLDOWN = 100; // 5 seconds
    private static final int RECOVERY_DURATION = 40; // 2 seconds
    private static final int FLASH_RANGE = 8;

    private int stateTimer = 0;
    private int diveCooldown = 0;
    private Vec3 diveTarget = null;

    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.bronze_falcon.fly_idle");
    private static final RawAnimation FLY_MOVE = RawAnimation.begin().thenLoop("animation.bronze_falcon.fly_move");
    private static final RawAnimation DIVE = RawAnimation.begin().thenPlay("animation.bronze_falcon.dive");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.bronze_falcon.stunned");
    private static final RawAnimation RECOVER = RawAnimation.begin().thenPlay("animation.bronze_falcon.recover");

    public BronzeFalconEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Bronze Falcon.
     * 16 HP, 4 armor, 0.4 speed, 7 dive damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FALCON_STATE, FalconState.FLYING.ordinal());
    }

    public FalconState getFalconState() {
        return FalconState.values()[this.entityData.get(FALCON_STATE)];
    }

    private void setFalconState(FalconState state) {
        this.entityData.set(FALCON_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (diveCooldown > 0) diveCooldown--;

            FalconState state = getFalconState();

            switch (state) {
                case FLYING -> {
                    // Float in air
                    if (this.onGround()) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.15, 0));
                    }
                    // Maintain altitude
                    double targetY = this.getTarget() != null ?
                        this.getTarget().position().y + 5 : this.position().y + 4;
                    if (this.position().y < targetY) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
                    }

                    // Initiate dive
                    if (diveCooldown <= 0 && this.getTarget() instanceof Player target) {
                        double distance = this.distanceTo(target);
                        if (distance < 16.0 && distance > 3.0) {
                            initiateDive(target);
                        }
                    }
                }
                case DIVING -> {
                    // Dive toward target
                    if (diveTarget != null) {
                        Vec3 direction = diveTarget.subtract(this.position()).normalize();
                        this.setDeltaMovement(direction.scale(0.8));

                        // Check if reached target or ground
                        if (this.onGround() || this.position().distanceTo(diveTarget) < 1.5) {
                            completeDive();
                        }
                    }
                }
                case GROUNDED_STUNNED -> {
                    stateTimer--;
                    this.getNavigation().stop();
                    if (stateTimer <= 0) {
                        setFalconState(FalconState.RECOVERING);
                        stateTimer = RECOVERY_DURATION;
                    }
                }
                case RECOVERING -> {
                    stateTimer--;
                    if (stateTimer <= 0) {
                        setFalconState(FalconState.FLYING);
                        diveCooldown = DIVE_COOLDOWN;
                    }
                }
            }
        }
    }

    /**
     * Initiates a dive-bomb attack with pre-impact blinding flash.
     */
    private void initiateDive(Player target) {
        setFalconState(FalconState.DIVING);
        diveTarget = target.position();

        // Pre-impact flash - blinds nearby players (unless blocking with shield)
        AABB flashArea = this.getBoundingBox().inflate(FLASH_RANGE);
        for (Player player : this.level().getEntitiesOfClass(Player.class, flashArea)) {
            if (!player.isBlocking()) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, true));
            }
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2.0f, 2.0f);
    }

    /**
     * Completes the dive attack. If missed, becomes stunned.
     */
    private void completeDive() {
        Player target = this.level().getNearestPlayer(this, 3.0);

        if (target != null) {
            // Hit the target
            target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            setFalconState(FalconState.FLYING);
            diveCooldown = DIVE_COOLDOWN;
        } else {
            // Missed - stunned on ground
            setFalconState(FalconState.GROUNDED_STUNNED);
            stateTimer = STUN_DURATION;
        }

        diveTarget = null;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.5f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Crossbow hit while flying = critical knockdown
        if (getFalconState() == FalconState.FLYING && source.getDirectEntity() instanceof AbstractArrow arrow) {
            if (arrow.isCritArrow()) {
                setFalconState(FalconState.GROUNDED_STUNNED);
                stateTimer = STUN_DURATION;
                this.setDeltaMovement(0, -0.5, 0);

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1.5f, 1.2f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.getX(), this.getY(), this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }

        // Takes extra damage while stunned
        if (getFalconState() == FalconState.GROUNDED_STUNNED) {
            amount *= 2.0f;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            FalconState falconState = getFalconState();
            return switch (falconState) {
                case FLYING -> {
                    if (state.isMoving()) yield state.setAndContinue(FLY_MOVE);
                    yield state.setAndContinue(FLY_IDLE);
                }
                case DIVING -> state.setAndContinue(DIVE);
                case GROUNDED_STUNNED -> state.setAndContinue(STUNNED);
                case RECOVERING -> state.setAndContinue(RECOVER);
            };
        }));
    }
}
