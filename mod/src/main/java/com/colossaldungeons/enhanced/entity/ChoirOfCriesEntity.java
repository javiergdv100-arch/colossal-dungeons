package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Choir of Cries - An elite entity from the Descent into Madness dungeon.
 *
 * A mass of mouths emitting layered cries. Attracts mobs from other rooms.
 * Sonic AoE disorientation attack. Note blocks with counter-frequency stun it.
 * Water silences it for 5 seconds.
 *
 * Stats: 100 HP, 2 armor, 0.2 speed, 9 sonic damage.
 */
public class ChoirOfCriesEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CRYING =
        SynchedEntityData.defineId(ChoirOfCriesEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_SILENCED =
        SynchedEntityData.defineId(ChoirOfCriesEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_STUNNED =
        SynchedEntityData.defineId(ChoirOfCriesEntity.class, EntityDataSerializers.BOOLEAN);

    // Combat parameters
    private static final int SONIC_AOE_COOLDOWN = 80; // 4 seconds
    private static final int SONIC_AOE_RADIUS = 8;
    private static final float SONIC_DAMAGE = 9.0f;
    private static final int DISORIENTATION_DURATION = 80; // 4 seconds
    private static final int MOB_ATTRACT_COOLDOWN = 200; // 10 seconds
    private static final int MOB_ATTRACT_RANGE = 30;
    private static final int SILENCE_DURATION = 100; // 5 seconds
    private static final int STUN_DURATION = 60; // 3 seconds

    private int sonicCooldown = 0;
    private int attractCooldown = 0;
    private int silenceTimer = 0;
    private int stunTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.choir_of_cries.idle");
    private static final RawAnimation CRY = RawAnimation.begin().thenLoop("animation.choir_of_cries.cry");
    private static final RawAnimation SONIC_ATTACK = RawAnimation.begin().thenPlay("animation.choir_of_cries.sonic_attack");
    private static final RawAnimation SILENCED_IDLE = RawAnimation.begin().thenLoop("animation.choir_of_cries.silenced");
    private static final RawAnimation STUNNED_ANIM = RawAnimation.begin().thenLoop("animation.choir_of_cries.stunned");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.choir_of_cries.walk");

    public ChoirOfCriesEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Choir of Cries.
     * 100 HP, 2 armor, 0.2 speed, 9 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_CRYING, true);
        builder.define(IS_SILENCED, false);
        builder.define(IS_STUNNED, false);
    }

    public boolean isCrying() {
        return this.entityData.get(IS_CRYING);
    }

    public boolean isSilenced() {
        return this.entityData.get(IS_SILENCED);
    }

    public boolean isStunned() {
        return this.entityData.get(IS_STUNNED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (sonicCooldown > 0) sonicCooldown--;
            if (attractCooldown > 0) attractCooldown--;

            // Handle silence timer
            if (isSilenced()) {
                silenceTimer--;
                if (silenceTimer <= 0) {
                    this.entityData.set(IS_SILENCED, false);
                    this.entityData.set(IS_CRYING, true);
                }
            }

            // Handle stun timer
            if (isStunned()) {
                stunTimer--;
                this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if (stunTimer <= 0) {
                    this.entityData.set(IS_STUNNED, false);
                }
                return; // No actions while stunned
            }

            // Check if in water - silence
            if (this.isInWaterOrRain() && !isSilenced()) {
                this.entityData.set(IS_SILENCED, true);
                this.entityData.set(IS_CRYING, false);
                silenceTimer = SILENCE_DURATION;
            }

            // Active abilities only when not silenced
            if (!isSilenced()) {
                // Sonic AoE attack
                if (sonicCooldown <= 0 && this.getTarget() != null) {
                    performSonicAttack();
                    sonicCooldown = SONIC_AOE_COOLDOWN;
                }

                // Attract mobs from other rooms
                if (attractCooldown <= 0) {
                    attractNearbyMobs();
                    attractCooldown = MOB_ATTRACT_COOLDOWN;
                }

                // Constant crying sound
                if (this.tickCount % 40 == 0) {
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.WARDEN_AMBIENT, SoundSource.HOSTILE, 2.0f, 1.5f);
                }
            }
        }
    }

    /**
     * Performs sonic AoE attack dealing damage and applying disorientation.
     */
    private void performSonicAttack() {
        AABB area = this.getBoundingBox().inflate(SONIC_AOE_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            player.hurt(this.damageSources().sonicBoom(this), SONIC_DAMAGE);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DISORIENTATION_DURATION, 0));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DISORIENTATION_DURATION / 2, 0));
        }

        // Sonic wave effect
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 0.8f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Attracts mobs from nearby rooms by giving them the target.
     */
    private void attractNearbyMobs() {
        AABB area = this.getBoundingBox().inflate(MOB_ATTRACT_RANGE);
        List<PathfinderMob> mobs = this.level().getEntitiesOfClass(PathfinderMob.class, area,
            mob -> mob != this && mob.getTarget() == null);

        Player target = this.level().getNearestPlayer(this, MOB_ATTRACT_RANGE);
        if (target != null) {
            for (PathfinderMob mob : mobs) {
                mob.setTarget(target);
            }
        }
    }

    /**
     * Called when a note block plays near this entity.
     * If the frequency is a counter-frequency, stuns the Choir.
     */
    public void onNoteBlockPlayed(BlockPos notePos) {
        if (this.distanceToSqr(notePos.getX(), notePos.getY(), notePos.getZ()) < 64.0) {
            // Counter-frequency stuns
            this.entityData.set(IS_STUNNED, true);
            stunTimer = STUN_DURATION;
            this.entityData.set(IS_CRYING, false);

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.HOSTILE, 1.5f, 0.5f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NOTE,
                    this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Extra damage while stunned
        if (isStunned()) {
            amount *= 1.5f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isNoAi() {
        return isStunned() || super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isStunned()) {
                return state.setAndContinue(STUNNED_ANIM);
            }
            if (isSilenced()) {
                return state.setAndContinue(SILENCED_IDLE);
            }
            if (isCrying()) {
                return state.setAndContinue(CRY);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging && !isStunned() && !isSilenced()) {
                return state.setAndContinue(SONIC_ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
