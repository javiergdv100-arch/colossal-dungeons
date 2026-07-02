package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
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
 * Old Gust - An air floor whirlwind from the Primordial Tower.
 *
 * Pushes players between floating platforms with strong knockback.
 * Can lift and drop players. Shield completely nullifies knockback.
 * 3 snowball hits disperse (kill) it.
 *
 * Stats: 14 HP, 0.4 speed, 3 damage + strong knockback.
 */
public class OldGustEntity extends CDEGeoEntity implements GeoEntity {

    private static final int SNOWBALL_HITS_TO_DISPERSE = 3;
    private static final double KNOCKBACK_STRENGTH = 3.0;
    private static final double LIFT_STRENGTH = 0.8;
    private int snowballHits = 0;
    private int liftCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.old_gust.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.old_gust.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.old_gust.attack");
    private static final RawAnimation LIFT = RawAnimation.begin().thenPlay("animation.old_gust.lift");

    public OldGustEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Old Gust.
     * 14 HP, 0.4 speed, 3 damage + strong knockback.
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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.2));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (liftCooldown > 0) liftCooldown--;

            // Float above ground
            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.1, 0));
            }

            // Erratic wind motion
            if (this.tickCount % 8 == 0) {
                double rx = (this.random.nextDouble() - 0.5) * 0.3;
                double rz = (this.random.nextDouble() - 0.5) * 0.3;
                this.setDeltaMovement(this.getDeltaMovement().add(rx, 0, rz));
            }

            // Wind particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.3, this.getZ(), 1, 0.3, 0.2, 0.3, 0.02);
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
                0.5,
                knockbackDir.z * KNOCKBACK_STRENGTH
            );

            // Chance to lift and drop
            if (liftCooldown <= 0 && this.random.nextFloat() < 0.3f) {
                living.push(0, LIFT_STRENGTH, 0);
                liftCooldown = 80; // 4 seconds cooldown
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.5f, 1.5f);
            }

            // Wind gust sound
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.2f, 1.0f);
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Snowball hits - 3 kills
        if (source.getDirectEntity() instanceof Snowball) {
            snowballHits++;
            if (snowballHits >= SNOWBALL_HITS_TO_DISPERSE) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.5, this.getZ(), 25, 0.5, 0.5, 0.5, 0.15);
                }
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 2.0f, 0.5f);
                this.discard();
                return true;
            }
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
