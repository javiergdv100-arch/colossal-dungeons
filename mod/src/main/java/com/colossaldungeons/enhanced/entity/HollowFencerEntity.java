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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hollow Fencer - A rare acrobatic swordsman from the Descent into Madness dungeon.
 *
 * A headless corpse controlled by a chest parasite that mimics human voices.
 * Fast rapier attacks: Punzada rapida (single thrust), Rafaga de estocadas
 * (burst that disables shields). Parry stance (100% block during animation).
 * 10s offense then 3s fatigue cycle. Low HP triggers interruptible healing
 * potion (snowball interrupts).
 *
 * Stats: 80 HP, 3 armor, 0.32 speed, 9 attack damage.
 */
public class HollowFencerEntity extends CDEGeoEntity implements GeoEntity {

    public enum FencerState {
        IDLE,
        OFFENSIVE,
        PUNZADA_RAPIDA,
        RAFAGA_ESTOCADAS,
        PARRY,
        FATIGUED,
        HEALING
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(HollowFencerEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_PARRYING =
        SynchedEntityData.defineId(HollowFencerEntity.class, EntityDataSerializers.BOOLEAN);

    // Combat timing
    private static final int OFFENSE_DURATION = 200; // 10 seconds
    private static final int FATIGUE_DURATION = 60; // 3 seconds
    private static final int PARRY_DURATION = 20; // 1 second perfect block
    private static final int PUNZADA_COOLDOWN = 30; // 1.5 seconds
    private static final int RAFAGA_COOLDOWN = 100; // 5 seconds
    private static final int RAFAGA_HITS = 4; // Burst of 4 rapid hits
    private static final int HEAL_DURATION = 60; // 3 seconds to drink potion
    private static final float HEAL_AMOUNT = 30.0f;
    private static final float HEAL_THRESHOLD = 0.25f; // Start healing at 25% HP

    // Damage values
    private static final float PUNZADA_DAMAGE = 9.0f;
    private static final float RAFAGA_SINGLE_DAMAGE = 5.0f;

    private int combatCycleTimer = 0;
    private int attackCooldown = 0;
    private int rafagaCooldown = 0;
    private int parryTimer = 0;
    private int healTimer = 0;
    private int rafagaHitsRemaining = 0;
    private int rafagaHitTimer = 0;
    private boolean hasHealed = false;

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hollow_fencer.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hollow_fencer.walk");
    private static final RawAnimation PUNZADA = RawAnimation.begin().thenPlay("animation.hollow_fencer.punzada_rapida");
    private static final RawAnimation RAFAGA = RawAnimation.begin().thenPlay("animation.hollow_fencer.rafaga_estocadas");
    private static final RawAnimation PARRY_ANIM = RawAnimation.begin().thenPlay("animation.hollow_fencer.parry");
    private static final RawAnimation FATIGUE_ANIM = RawAnimation.begin().thenLoop("animation.hollow_fencer.fatigued");
    private static final RawAnimation HEAL_ANIM = RawAnimation.begin().thenPlay("animation.hollow_fencer.heal");
    private static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.hollow_fencer.dodge");

    public HollowFencerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Hollow Fencer.
     * 80 HP, 3 armor, 0.32 speed, 9 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 80.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, FencerState.IDLE.ordinal());
        builder.define(IS_PARRYING, false);
    }

    public FencerState getFencerState() {
        return FencerState.values()[this.entityData.get(STATE)];
    }

    private void setFencerState(FencerState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    public boolean isParrying() {
        return this.entityData.get(IS_PARRYING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (rafagaCooldown > 0) rafagaCooldown--;

            // Check if should start healing
            if (!hasHealed && this.getHealth() / this.getMaxHealth() <= HEAL_THRESHOLD
                && getFencerState() != FencerState.HEALING) {
                setFencerState(FencerState.HEALING);
                healTimer = HEAL_DURATION;
            }

            FencerState currentState = getFencerState();

            switch (currentState) {
                case IDLE -> {
                    if (this.getTarget() != null) {
                        setFencerState(FencerState.OFFENSIVE);
                        combatCycleTimer = OFFENSE_DURATION;
                    }
                }
                case OFFENSIVE -> {
                    combatCycleTimer--;
                    tickOffensiveCombat();

                    if (combatCycleTimer <= 0) {
                        setFencerState(FencerState.FATIGUED);
                        combatCycleTimer = FATIGUE_DURATION;
                    }
                }
                case PUNZADA_RAPIDA -> {
                    // Single precise thrust attack (handled in doHurtTarget)
                    setFencerState(FencerState.OFFENSIVE);
                }
                case RAFAGA_ESTOCADAS -> {
                    // Burst of rapid hits
                    tickRafaga();
                }
                case PARRY -> {
                    parryTimer--;
                    this.entityData.set(IS_PARRYING, true);
                    if (parryTimer <= 0) {
                        this.entityData.set(IS_PARRYING, false);
                        setFencerState(FencerState.OFFENSIVE);
                    }
                }
                case FATIGUED -> {
                    combatCycleTimer--;
                    // Cannot attack during fatigue
                    if (combatCycleTimer <= 0) {
                        setFencerState(FencerState.OFFENSIVE);
                        combatCycleTimer = OFFENSE_DURATION;
                    }
                }
                case HEALING -> {
                    healTimer--;
                    // Cannot move while healing
                    this.setDeltaMovement(Vec3.ZERO);

                    if (healTimer <= 0) {
                        // Successfully healed
                        this.heal(HEAL_AMOUNT);
                        hasHealed = true;
                        setFencerState(FencerState.OFFENSIVE);
                        combatCycleTimer = OFFENSE_DURATION;

                        this.level().playSound(null, this.blockPosition(),
                            SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    /**
     * Handles offensive combat AI - choosing between attacks.
     */
    private void tickOffensiveCombat() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Random parry before player attacks
        if (this.random.nextFloat() < 0.05f && distance < 4.0) {
            setFencerState(FencerState.PARRY);
            parryTimer = PARRY_DURATION;
            return;
        }

        if (distance < 3.5) {
            // Choose attack type
            if (rafagaCooldown <= 0 && this.random.nextFloat() < 0.3f) {
                // Rafaga de estocadas - burst attack
                setFencerState(FencerState.RAFAGA_ESTOCADAS);
                rafagaHitsRemaining = RAFAGA_HITS;
                rafagaHitTimer = 0;
                rafagaCooldown = RAFAGA_COOLDOWN;
            } else if (attackCooldown <= 0) {
                // Punzada rapida - single thrust
                if (target instanceof LivingEntity living) {
                    living.hurt(this.damageSources().mobAttack(this), PUNZADA_DAMAGE);
                    attackCooldown = PUNZADA_COOLDOWN;

                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.5f);
                }
            }
        }
    }

    /**
     * Executes the Rafaga de Estocadas burst attack.
     * Rapid hits that disable shields.
     */
    private void tickRafaga() {
        rafagaHitTimer++;

        if (rafagaHitTimer % 5 == 0 && rafagaHitsRemaining > 0) {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceTo(target) < 4.0) {
                // Rafaga bypasses shields
                if (target instanceof Player player) {
                    player.getCooldowns().addCooldown(player.getUseItem().getItem(), 40);
                    player.stopUsingItem(); // Disable shield
                }
                target.hurt(this.damageSources().mobAttack(this), RAFAGA_SINGLE_DAMAGE);
                rafagaHitsRemaining--;

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8f, 1.8f);
            } else {
                rafagaHitsRemaining = 0;
            }
        }

        if (rafagaHitsRemaining <= 0) {
            setFencerState(FencerState.OFFENSIVE);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Parry blocks all damage
        if (isParrying()) {
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.5f, 1.2f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
            }
            return false;
        }

        // Snowball interrupts healing
        if (getFencerState() == FencerState.HEALING && source.getDirectEntity() instanceof Snowball) {
            setFencerState(FencerState.FATIGUED);
            combatCycleTimer = FATIGUE_DURATION;
            healTimer = 0;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0f, 1.5f);

            return super.hurt(source, 1.0f);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            FencerState fencerState = getFencerState();
            return switch (fencerState) {
                case FATIGUED -> state.setAndContinue(FATIGUE_ANIM);
                case PARRY -> state.setAndContinue(PARRY_ANIM);
                case HEALING -> state.setAndContinue(HEAL_ANIM);
                default -> {
                    if (state.isMoving()) yield state.setAndContinue(WALK);
                    yield state.setAndContinue(IDLE);
                }
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            FencerState fencerState = getFencerState();
            if (fencerState == FencerState.RAFAGA_ESTOCADAS) {
                return state.setAndContinue(RAFAGA);
            }
            if (this.swinging && fencerState == FencerState.OFFENSIVE) {
                return state.setAndContinue(PUNZADA);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
