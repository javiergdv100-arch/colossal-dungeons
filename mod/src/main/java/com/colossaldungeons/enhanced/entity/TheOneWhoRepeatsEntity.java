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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * The One Who Repeats - An elite entity from the Descent into Madness dungeon.
 *
 * Forces room resets (infinite corridor mechanic). Each repetition changes
 * one detail (the clue to breaking the loop). Killing this entity breaks the loop.
 * Recovery compass always points to its real position.
 *
 * Stats: 140 HP, 3 armor, 0.24 speed, 10 attack damage.
 */
public class TheOneWhoRepeatsEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> REPETITION_COUNT =
        SynchedEntityData.defineId(TheOneWhoRepeatsEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_RESETTING =
        SynchedEntityData.defineId(TheOneWhoRepeatsEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> LOOP_ACTIVE =
        SynchedEntityData.defineId(TheOneWhoRepeatsEntity.class, EntityDataSerializers.BOOLEAN);

    // Loop mechanics
    private static final int RESET_COOLDOWN = 400; // 20 seconds between resets
    private static final int RESET_ANIMATION_DURATION = 60; // 3 second reset animation
    private static final int MAX_REPETITIONS = 7; // Loop can repeat up to 7 times
    private static final double LOOP_BOUNDARY_RADIUS = 20.0;

    // Combat
    private static final float BASE_DAMAGE = 10.0f;
    private static final float DAMAGE_PER_REPETITION = 1.0f; // Gets stronger each loop
    private static final int TELEPORT_COOLDOWN = 120; // 6 seconds

    private int resetCooldown = 0;
    private int resetTimer = 0;
    private int teleportCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_one_who_repeats.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.the_one_who_repeats.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.the_one_who_repeats.attack");
    private static final RawAnimation RESET_ANIM = RawAnimation.begin().thenPlay("animation.the_one_who_repeats.reset");
    private static final RawAnimation TELEPORT = RawAnimation.begin().thenPlay("animation.the_one_who_repeats.teleport");
    private static final RawAnimation LOOP_BREAK = RawAnimation.begin().thenPlay("animation.the_one_who_repeats.loop_break");

    public TheOneWhoRepeatsEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for The One Who Repeats.
     * 140 HP, 3 armor, 0.24 speed, 10 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 140.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REPETITION_COUNT, 0);
        builder.define(IS_RESETTING, false);
        builder.define(LOOP_ACTIVE, true);
    }

    public int getRepetitionCount() {
        return this.entityData.get(REPETITION_COUNT);
    }

    public boolean isResetting() {
        return this.entityData.get(IS_RESETTING);
    }

    public boolean isLoopActive() {
        return this.entityData.get(LOOP_ACTIVE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (resetCooldown > 0) resetCooldown--;
            if (teleportCooldown > 0) teleportCooldown--;

            if (isResetting()) {
                resetTimer--;
                this.setDeltaMovement(Vec3.ZERO);

                if (resetTimer <= 0) {
                    executeRoomReset();
                    this.entityData.set(IS_RESETTING, false);
                }
                return;
            }

            // Trigger room reset if loop is active
            if (isLoopActive() && resetCooldown <= 0 && getRepetitionCount() < MAX_REPETITIONS) {
                Player target = this.level().getNearestPlayer(this, LOOP_BOUNDARY_RADIUS);
                if (target != null) {
                    initiateRoomReset();
                }
            }

            // Teleport away from players who get too close
            if (teleportCooldown <= 0) {
                Player nearest = this.level().getNearestPlayer(this, 3.0);
                if (nearest != null) {
                    teleportAway();
                    teleportCooldown = TELEPORT_COOLDOWN;
                }
            }

            // Reality distortion particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.5, 0.5, 0.5, 0.02);
            }
        }
    }

    /**
     * Initiates a room reset sequence.
     */
    private void initiateRoomReset() {
        this.entityData.set(IS_RESETTING, true);
        resetTimer = RESET_ANIMATION_DURATION;
        resetCooldown = RESET_COOLDOWN;

        // Warning sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5f, 0.3f);
    }

    /**
     * Executes the room reset, teleporting players back and incrementing repetition.
     */
    private void executeRoomReset() {
        int repetition = getRepetitionCount() + 1;
        this.entityData.set(REPETITION_COUNT, repetition);

        // Teleport all nearby players back to room start (simulated by pushing them back)
        AABB area = this.getBoundingBox().inflate(LOOP_BOUNDARY_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            // Apply disorientation
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
        }

        // The entity gets slightly stronger each repetition
        float newDamage = BASE_DAMAGE + (repetition * DAMAGE_PER_REPETITION);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);

        // Reset sound and particles
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.5f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 1.0, this.getZ(), 30, 3.0, 2.0, 3.0, 0.1);
        }
    }

    /**
     * Teleports the entity away from the player to a random nearby position.
     */
    private void teleportAway() {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 8.0 + this.random.nextDouble() * 6.0;
            double newX = this.getX() + Math.cos(angle) * dist;
            double newZ = this.getZ() + Math.sin(angle) * dist;
            double newY = this.getY();

            if (this.level().getBlockState(
                    new net.minecraft.core.BlockPos((int) newX, (int) newY, (int) newZ)).isAir()) {
                // Particles at old position
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                }

                this.teleportTo(newX, newY, newZ);

                // Particles at new position
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        newX, newY + 1.0, newZ, 10, 0.3, 0.5, 0.3, 0.05);
                }

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.5f);
                break;
            }
        }
    }

    @Override
    protected void tickDeath() {
        // Breaking the loop on death
        this.entityData.set(LOOP_ACTIVE, false);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.0, this.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0f, 0.5f);

        ++this.deathTime;
        if (this.deathTime >= 60 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isResetting()) {
                return state.setAndContinue(RESET_ANIM);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && !isResetting()) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
