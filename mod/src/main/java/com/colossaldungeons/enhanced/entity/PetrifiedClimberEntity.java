package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
 * Petrified Climber - A frozen reanimated adventurer from the Veiled Peak dungeon.
 *
 * Resistant to cold damage, extremely weak to fire (fire halves max HP).
 * Climbs walls to flank players. Can push targets toward precipices.
 *
 * Stats: 30 HP, 5 armor, 0.28 speed, 6 attack damage.
 */
public class PetrifiedClimberEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CLIMBING =
        SynchedEntityData.defineId(PetrifiedClimberEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_FIRE_WEAKENED =
        SynchedEntityData.defineId(PetrifiedClimberEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float FIRE_DAMAGE_MULTIPLIER = 3.0f;
    private static final float FIRE_MAX_HP_REDUCTION = 0.5f;
    private static final float PUSH_STRENGTH = 1.2f;
    private boolean hasBeenBurned = false;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.petrified_climber.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.petrified_climber.walk");
    private static final RawAnimation CLIMB = RawAnimation.begin().thenLoop("animation.petrified_climber.climb");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.petrified_climber.attack");
    private static final RawAnimation PUSH = RawAnimation.begin().thenPlay("animation.petrified_climber.push");

    public PetrifiedClimberEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Petrified Climber.
     * 30 HP, 5 armor, 0.28 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_CLIMBING, false);
        builder.define(IS_FIRE_WEAKENED, false);
    }

    public boolean isClimbing() {
        return this.entityData.get(IS_CLIMBING);
    }

    public boolean isFireWeakened() {
        return this.entityData.get(IS_FIRE_WEAKENED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Check if on a wall (horizontalCollision as climbing indicator)
            boolean climbing = this.horizontalCollision && !this.onGround();
            this.entityData.set(IS_CLIMBING, climbing);

            // Allow climbing by negating gravity when on wall
            if (climbing) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.1, 0));
                this.fallDistance = 0.0f;
            }

            // Push attack logic: push target toward edges
            if (this.getTarget() instanceof Player target && this.distanceTo(target) < 2.5) {
                if (this.tickCount % 60 == 0) { // Push every 3 seconds
                    Vec3 pushDir = target.position().subtract(this.position()).normalize();
                    target.push(pushDir.x * PUSH_STRENGTH, 0.2, pushDir.z * PUSH_STRENGTH);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Resistant to cold/freezing
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }

        // Extremely weak to fire - takes triple damage and permanently loses max HP
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_DAMAGE_MULTIPLIER;

            if (!hasBeenBurned) {
                hasBeenBurned = true;
                this.entityData.set(IS_FIRE_WEAKENED, true);
                // Halve max HP permanently on first fire hit
                float newMax = this.getMaxHealth() * FIRE_MAX_HP_REDUCTION;
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                if (this.getHealth() > newMax) {
                    this.setHealth(newMax);
                }

                // Fire melting particles
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean onClimbable() {
        // Always treat as on ladder when climbing
        return isClimbing() || super.onClimbable();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isClimbing()) {
                return state.setAndContinue(CLIMB);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
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
    }
}
