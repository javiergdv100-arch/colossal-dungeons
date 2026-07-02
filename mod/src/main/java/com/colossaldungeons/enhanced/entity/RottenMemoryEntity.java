package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Rotten Memory - A distorted entity from the Descent into Madness dungeon.
 *
 * A distorted recreation of an ally that mutates on approach.
 * Applies Nausea + warped vision effects. Dissipates if ignored for 15 seconds.
 *
 * Stats: 28 HP, 0.26 speed, 7 attack damage + nausea.
 */
public class RottenMemoryEntity extends CDEGeoEntity implements GeoEntity {

    public enum MemoryState {
        DORMANT,      // Appears as ally, waiting
        MUTATING,     // Player approached, form distorting
        AGGRESSIVE,   // Fully mutated, attacking
        DISSIPATING   // Being ignored, fading away
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(RottenMemoryEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MUTATION_LEVEL =
        SynchedEntityData.defineId(RottenMemoryEntity.class, EntityDataSerializers.INT);

    private static final double TRIGGER_RANGE = 8.0;
    private static final int DISSIPATE_TIME = 300; // 15 seconds
    private static final int MUTATION_DURATION = 40; // 2 seconds to mutate
    private static final int NAUSEA_DURATION = 100; // 5 seconds
    private static final int CONFUSION_DURATION = 60; // 3 seconds

    private int ignoreTimer = 0;
    private int mutationTimer = 0;
    private boolean playerApproached = false;

    private static final RawAnimation IDLE_ALLY = RawAnimation.begin().thenLoop("animation.rotten_memory.idle_ally");
    private static final RawAnimation MUTATE = RawAnimation.begin().thenPlay("animation.rotten_memory.mutate");
    private static final RawAnimation IDLE_MUTATED = RawAnimation.begin().thenLoop("animation.rotten_memory.idle_mutated");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rotten_memory.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.rotten_memory.attack");
    private static final RawAnimation DISSIPATE = RawAnimation.begin().thenPlay("animation.rotten_memory.dissipate");

    public RottenMemoryEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Rotten Memory.
     * 28 HP, 0.26 speed, 7 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, MemoryState.DORMANT.ordinal());
        builder.define(MUTATION_LEVEL, 0);
    }

    public MemoryState getMemoryState() {
        return MemoryState.values()[this.entityData.get(STATE)];
    }

    private void setMemoryState(MemoryState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            MemoryState currentState = getMemoryState();

            switch (currentState) {
                case DORMANT -> {
                    // Check if player approaches
                    Player nearest = this.level().getNearestPlayer(this, TRIGGER_RANGE);
                    if (nearest != null && this.distanceTo(nearest) < TRIGGER_RANGE / 2) {
                        // Player is approaching - start mutating
                        playerApproached = true;
                        setMemoryState(MemoryState.MUTATING);
                        mutationTimer = MUTATION_DURATION;
                    } else if (nearest == null) {
                        // Track ignore time
                        ignoreTimer++;
                        if (ignoreTimer >= DISSIPATE_TIME) {
                            setMemoryState(MemoryState.DISSIPATING);
                        }
                    }
                }
                case MUTATING -> {
                    mutationTimer--;
                    // Increase mutation level for visual effect
                    int level = Math.min(3, (MUTATION_DURATION - mutationTimer) / (MUTATION_DURATION / 3));
                    this.entityData.set(MUTATION_LEVEL, level);

                    if (mutationTimer <= 0) {
                        setMemoryState(MemoryState.AGGRESSIVE);
                    }

                    // Mutation particles
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.WARPED_SPORE,
                            this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
                    }
                }
                case AGGRESSIVE -> {
                    // Apply nausea to nearby players
                    if (this.tickCount % 60 == 0) {
                        Player target = this.level().getNearestPlayer(this, 6.0);
                        if (target != null) {
                            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION, 0));
                        }
                    }
                }
                case DISSIPATING -> {
                    // Fade away
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.WARPED_SPORE,
                            this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 1.0, 0.5, 0.03);
                    }
                    this.discard();
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Apply nausea and confusion on hit
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION, 0));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CONFUSION_DURATION, 0));
        }

        return hit;
    }

    @Override
    public boolean isNoAi() {
        MemoryState state = getMemoryState();
        return state == MemoryState.DORMANT || state == MemoryState.DISSIPATING || super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            MemoryState memoryState = getMemoryState();
            return switch (memoryState) {
                case DORMANT -> state.setAndContinue(IDLE_ALLY);
                case MUTATING -> state.setAndContinue(MUTATE);
                case AGGRESSIVE -> {
                    if (state.isMoving()) yield state.setAndContinue(WALK);
                    yield state.setAndContinue(IDLE_MUTATED);
                }
                case DISSIPATING -> state.setAndContinue(DISSIPATE);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && getMemoryState() == MemoryState.AGGRESSIVE) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE_MUTATED);
        }));
    }
}
