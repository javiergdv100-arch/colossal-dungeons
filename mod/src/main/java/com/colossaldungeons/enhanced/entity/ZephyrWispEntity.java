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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
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
 * Zephyr Wisp - A wind elemental from the Veiled Peak dungeon.
 *
 * Main threat is pushing players into void/precipices via strong knockback.
 * Erratic fast movement pattern. Shield completely nullifies its knockback.
 * 3 snowball hits disperse (kill) it instantly.
 *
 * Stats: 14 HP, 0.4 speed (erratic), 3 attack damage + strong knockback.
 */
public class ZephyrWispEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> SNOWBALL_HITS =
        SynchedEntityData.defineId(ZephyrWispEntity.class, EntityDataSerializers.INT);

    private static final int SNOWBALL_HITS_TO_DISPERSE = 3;
    private static final double KNOCKBACK_STRENGTH = 2.5;
    private static final double ERRATIC_MOVE_AMPLITUDE = 0.4;

    private int erraticMoveTick = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.zephyr_wisp.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.zephyr_wisp.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.zephyr_wisp.attack");
    private static final RawAnimation DISPERSE = RawAnimation.begin().thenPlay("animation.zephyr_wisp.disperse");

    public ZephyrWispEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Zephyr Wisp.
     * 14 HP, 0.4 speed, 3 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.4)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SNOWBALL_HITS, 0);
    }

    public int getSnowballHits() {
        return this.entityData.get(SNOWBALL_HITS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.2));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Erratic movement pattern - add random motion
            erraticMoveTick++;
            if (erraticMoveTick % 10 == 0) {
                double randomX = (this.random.nextDouble() - 0.5) * ERRATIC_MOVE_AMPLITUDE;
                double randomY = (this.random.nextDouble() - 0.5) * 0.2;
                double randomZ = (this.random.nextDouble() - 0.5) * ERRATIC_MOVE_AMPLITUDE;
                this.setDeltaMovement(this.getDeltaMovement().add(randomX, randomY, randomZ));
            }

            // Float slightly above ground
            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.08, 0));
            }

            // Wind particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Check if target is blocking with a shield
            if (living instanceof Player player && player.isBlocking()) {
                // Shield completely nullifies knockback
                return true;
            }

            // Apply strong knockback pushing toward void/edge
            Vec3 knockbackDir = target.position().subtract(this.position()).normalize();
            living.push(
                knockbackDir.x * KNOCKBACK_STRENGTH,
                0.4,
                knockbackDir.z * KNOCKBACK_STRENGTH
            );

            // Wind gust sound
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.5f, 1.2f);
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Check for snowball hits
        if (source.getDirectEntity() instanceof Snowball) {
            int hits = getSnowballHits() + 1;
            this.entityData.set(SNOWBALL_HITS, hits);

            if (hits >= SNOWBALL_HITS_TO_DISPERSE) {
                // Disperse the wisp instantly
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 2.0f, 0.5f);
                this.discard();
                return true;
            }

            // Snowball still damages
            return super.hurt(source, 1.0f);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
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
    }
}
