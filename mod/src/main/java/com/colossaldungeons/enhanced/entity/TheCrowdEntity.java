package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Crowd - A swarm entity from the Descent into Madness dungeon.
 *
 * A mass of grabbing hands emerging from dark surfaces. Slows on contact.
 * Torches eliminate dark surfaces (prevents spawning). Fire eliminates them in mass.
 * Swarm mob with low individual HP but spawns in groups.
 *
 * Stats: 6 HP, 0.25 speed, 3 attack damage.
 */
public class TheCrowdEntity extends CDEGeoEntity implements GeoEntity {

    private static final int SLOWNESS_DURATION = 40; // 2 seconds
    private static final int SLOWNESS_AMPLIFIER = 2;
    private static final int LIGHT_LEVEL_DESTROY = 10;
    private static final float FIRE_DAMAGE_MULTIPLIER = 5.0f;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_crowd.idle");
    private static final RawAnimation GRAB = RawAnimation.begin().thenLoop("animation.the_crowd.grab");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.the_crowd.attack");
    private static final RawAnimation DISSOLVE = RawAnimation.begin().thenPlay("animation.the_crowd.dissolve");

    public TheCrowdEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for The Crowd.
     * 6 HP, 0.25 speed, 3 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Self-destruct in bright light
            int lightLevel = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());
            if (lightLevel >= LIGHT_LEVEL_DESTROY) {
                // Dissolve in light
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.3, this.getZ(), 5, 0.3, 0.2, 0.3, 0.02);
                }
                this.discard();
                return;
            }

            // Apply slowness on contact with target
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceTo(target) < 1.5) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
            }

            // Dark surface particles
            if (this.tickCount % 15 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Apply slowness on hit (grabbing)
            living.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fire kills them extremely efficiently
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_DAMAGE_MULTIPLIER;

            // Spread fire death to nearby crowd members (chain elimination)
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.0) {
                return state.setAndContinue(GRAB);
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
