package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Spitting Polyp - Hollow Leviathan dungeon creature.
 *
 * A fixed organic turret attached to walls or ceilings that spits acid projectiles
 * at passing players. Creates acid puddles where projectiles land. Completely immobile
 * and easy to kill at melee range, but dangerous from a distance. Shields can block
 * the acid projectiles.
 *
 * Stats: 22 HP, ranged acid 7 damage.
 * Behavior: Fixed position, ranged acid spit, creates puddles.
 * Weakness: Immobile, easy to kill at melee range.
 */
public class SpittingPolypEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CHARGING =
        SynchedEntityData.defineId(SpittingPolypEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_FIRING =
        SynchedEntityData.defineId(SpittingPolypEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float ACID_DAMAGE = 7.0f;
    private static final double ATTACK_RANGE = 16.0;
    private static final int CHARGE_DURATION = 30; // 1.5 seconds to charge
    private static final int FIRE_COOLDOWN = 60; // 3 seconds between shots
    private static final float ACID_PUDDLE_DAMAGE = 2.0f;
    private static final int PUDDLE_DURATION = 100; // 5 seconds

    private int chargeTimer = 0;
    private int fireCooldown = 0;
    private LivingEntity aimTarget = null;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.spitting_polyp.idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlay("animation.spitting_polyp.charge");
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("animation.spitting_polyp.fire");
    private static final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlay("animation.spitting_polyp.hurt");

    public SpittingPolypEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Fixed to walls/ceilings
    }

    /**
     * Creates the attribute supplier for Spitting Polyp.
     * 22 HP, 0.0 speed (immobile), 7 damage (ranged), 0 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 22.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // Fixed to surface
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_CHARGING, false);
        builder.define(IS_FIRING, false);
    }

    public boolean isCharging() {
        return this.entityData.get(IS_CHARGING);
    }

    private void setCharging(boolean charging) {
        this.entityData.set(IS_CHARGING, charging);
    }

    public boolean isFiring() {
        return this.entityData.get(IS_FIRING);
    }

    private void setFiring(boolean firing) {
        this.entityData.set(IS_FIRING, firing);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            fireCooldown--;

            if (isCharging()) {
                chargeTimer--;
                if (chargeTimer <= 0) {
                    fireAcid();
                }
            } else if (fireCooldown <= 0) {
                // Look for targets
                LivingEntity target = this.getTarget();
                if (target != null && this.distanceTo(target) <= ATTACK_RANGE) {
                    // Check line of sight
                    if (this.hasLineOfSight(target)) {
                        startCharging(target);
                    }
                }
            }

            // Reset firing state after short delay
            if (isFiring()) {
                setFiring(false);
            }
        }
    }

    /**
     * Starts charging for an acid shot.
     */
    private void startCharging(LivingEntity target) {
        setCharging(true);
        chargeTimer = CHARGE_DURATION;
        aimTarget = target;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.0f, 2.0f);
    }

    /**
     * Fires an acid projectile at the target.
     */
    private void fireAcid() {
        setCharging(false);
        setFiring(true);
        fireCooldown = FIRE_COOLDOWN;

        if (aimTarget != null && !aimTarget.isDeadOrDying()) {
            // Calculate trajectory toward target
            Vec3 direction = aimTarget.position().add(0, aimTarget.getBbHeight() * 0.5, 0)
                .subtract(this.position()).normalize();

            // Spawn acid projectile (using snowball-like entity as placeholder)
            if (this.level() instanceof ServerLevel serverLevel) {
                // Create acid projectile entity
                net.minecraft.world.entity.projectile.Snowball acidBall =
                    new net.minecraft.world.entity.projectile.Snowball(this.level(), this);
                acidBall.setPos(this.getX(), this.getY() + 0.5, this.getZ());
                acidBall.shoot(direction.x, direction.y, direction.z, 1.2f, 2.0f);
                serverLevel.addFreshEntity(acidBall);

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1.5f, 0.7f);
            }
        }

        aimTarget = null;
    }

    /**
     * Creates an acid puddle at the specified position.
     * Called when the acid projectile lands.
     */
    public static void createAcidPuddle(Level level, BlockPos pos) {
        // Place a temporary hazard block (using water cauldron as visual placeholder)
        // In production this would be a custom acid puddle block
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState(), 3);
        }
    }

    @Override
    public boolean isNoAi() {
        return true; // Completely stationary
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isFiring()) {
                return state.setAndContinue(FIRE);
            }
            if (isCharging()) {
                return state.setAndContinue(CHARGE);
            }
            return state.setAndContinue(IDLE);
        }));
    }
}
