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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Ancestral Droplet - A living water mass from the Primordial Tower.
 *
 * Generates currents that push/drag players. Extinguishes player's light sources.
 * Freezes and becomes fragile with cold (powder snow = instant freeze then one-hit shatter).
 *
 * Stats: 18 HP, 0.25 speed, 5 damage + drag effect.
 */
public class AncestralDropletEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_FROZEN =
        SynchedEntityData.defineId(AncestralDropletEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double CURRENT_RANGE = 6.0;
    private static final double CURRENT_STRENGTH = 0.15;
    private static final int FREEZE_DURATION = 100; // 5 seconds frozen before auto-thaw
    private int freezeTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ancestral_droplet.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.ancestral_droplet.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ancestral_droplet.attack");
    private static final RawAnimation FROZEN = RawAnimation.begin().thenLoop("animation.ancestral_droplet.frozen");
    private static final RawAnimation SHATTER = RawAnimation.begin().thenPlay("animation.ancestral_droplet.shatter");

    public AncestralDropletEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Ancestral Droplet.
     * 18 HP, 0.25 speed, 5 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_FROZEN, false);
    }

    public boolean isFrozen() {
        return this.entityData.get(IS_FROZEN);
    }

    private void setFrozen(boolean frozen) {
        this.entityData.set(IS_FROZEN, frozen);
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
            // Handle frozen state
            if (isFrozen()) {
                freezeTimer--;
                if (freezeTimer <= 0) {
                    setFrozen(false);
                }
                // While frozen, no AI runs
                this.getNavigation().stop();
                return;
            }

            // Check for powder snow contact = instant freeze
            if (this.level().getBlockState(this.blockPosition()).is(Blocks.POWDER_SNOW)) {
                setFrozen(true);
                freezeTimer = FREEZE_DURATION;
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.5f, 0.5f);
                return;
            }

            // Generate water currents pushing/dragging nearby players
            if (this.tickCount % 10 == 0) {
                generateCurrents();
            }

            // Water particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    this.getX(), this.getY() + 0.5, this.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    /**
     * Generates water currents that push/drag nearby players toward or away from this entity.
     */
    private void generateCurrents() {
        AABB area = this.getBoundingBox().inflate(CURRENT_RANGE);
        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            double distance = this.distanceTo(player);
            if (distance > 0 && distance <= CURRENT_RANGE) {
                // Drag player toward this entity
                Vec3 direction = this.position().subtract(player.position()).normalize();
                double strength = CURRENT_STRENGTH * (1.0 - (distance / CURRENT_RANGE));
                player.push(direction.x * strength, 0, direction.z * strength);

                // Extinguish player's light sources (apply darkness briefly)
                if (distance < 3.0 && this.tickCount % 40 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Apply slowness to simulate water drag
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));

            // Pull target toward self
            Vec3 pullDir = this.position().subtract(target.position()).normalize();
            living.push(pullDir.x * 0.3, 0, pullDir.z * 0.3);
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // If frozen, takes massive damage (one-hit shatter)
        if (isFrozen()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.2f);
            this.discard();
            return true;
        }

        // Powder snow projectile or freezing damage = instant freeze
        if (source.type().msgId().contains("freeze")) {
            setFrozen(true);
            freezeTimer = FREEZE_DURATION;
            return super.hurt(source, amount);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isFrozen()) {
                return state.setAndContinue(FROZEN);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging && !isFrozen()) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
