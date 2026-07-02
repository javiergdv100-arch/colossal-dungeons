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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Brazier Igneous - A fire creature born from lit braziers in the Solar Palace.
 *
 * Ignites oil and oiled players. Explodes in sparks on death (AoE fire 3 dmg).
 * Water = instant kill. 2 snowballs kill.
 * Powder snow freezes without death explosion.
 *
 * Stats: 18 HP, 0.3 speed, fire 7 damage.
 * Fire immune (set in EntityType.Builder).
 */
public class BrazierIgneousEntity extends CDEGeoEntity implements GeoEntity {

    private static final int SNOWBALL_HITS_TO_KILL = 2;
    private static final float DEATH_EXPLOSION_DAMAGE = 3.0f;
    private static final double DEATH_EXPLOSION_RANGE = 4.0;
    private int snowballHits = 0;
    private boolean frozenDeath = false; // If true, no death explosion

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.brazier_igneous.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.brazier_igneous.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.brazier_igneous.attack");
    private static final RawAnimation EXPLODE = RawAnimation.begin().thenPlay("animation.brazier_igneous.explode");

    public BrazierIgneousEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Brazier Igneous.
     * 18 HP, 0.3 speed, 7 fire damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Fire particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.4, this.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Ignite on contact
            living.setRemainingFireTicks(80); // 4 seconds

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.0f, 1.5f);
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Water bucket = instant kill
        if (source.getDirectEntity() instanceof Player player) {
            if (player.getMainHandItem().is(Items.WATER_BUCKET)) {
                frozenDeath = false; // Still explodes
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 1.0f);
                this.kill();
                return true;
            }
        }

        // Water/drown damage = instant kill
        if (source.type().msgId().contains("drown")) {
            this.kill();
            return true;
        }

        // Snowball hits - 2 kills
        if (source.getDirectEntity() instanceof Snowball) {
            snowballHits++;
            if (snowballHits >= SNOWBALL_HITS_TO_KILL) {
                this.kill();
                return true;
            }
            return super.hurt(source, 1.0f);
        }

        // Powder snow/freeze = kill without explosion
        if (source.type().msgId().contains("freeze")) {
            frozenDeath = true;
            this.kill();
            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;

        // Spark explosion on death (unless frozen)
        if (this.deathTime == 1 && !this.level().isClientSide() && !frozenDeath) {
            // AoE fire damage to nearby entities
            AABB area = this.getBoundingBox().inflate(DEATH_EXPLOSION_RANGE);
            List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this);

            for (LivingEntity entity : nearbyEntities) {
                if (this.distanceTo(entity) <= DEATH_EXPLOSION_RANGE) {
                    entity.hurt(this.damageSources().onFire(), DEATH_EXPLOSION_DAMAGE);
                    entity.setRemainingFireTicks(40);
                }
            }

            // Explosion particles and sound
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.5, this.getZ(), 20, 1.5, 0.5, 1.5, 0.1);
                serverLevel.sendParticles(ParticleTypes.LAVA,
                    this.getX(), this.getY() + 0.5, this.getZ(), 8, 1.0, 0.3, 1.0, 0.05);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.5f);
        }

        if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
