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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Primal Ember - A living fire spark from the Primordial Tower.
 *
 * Living spark that ignites on contact, ignites oil and oiled players.
 * Dies INSTANTLY to water bucket (1 hit kill). 2 snowballs also kill.
 * Faster on fire floor (magma blocks).
 *
 * Stats: 16 HP, 0.35 speed, fire 6 damage.
 * Fire immune (set in EntityType.Builder).
 */
public class PrimalEmberEntity extends CDEGeoEntity implements GeoEntity {

    private static final int SNOWBALL_HITS_TO_KILL = 2;
    private int snowballHits = 0;
    private boolean onFireFloor = false;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.primal_ember.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.primal_ember.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.primal_ember.attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.primal_ember.death");

    public PrimalEmberEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Primal Ember.
     * 16 HP, 0.35 speed, 6 fire damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Check if on fire floor (magma blocks) for speed boost
            boolean wasOnFireFloor = onFireFloor;
            onFireFloor = this.level().getBlockState(this.blockPosition().below()).is(Blocks.MAGMA_BLOCK);

            if (onFireFloor && !wasOnFireFloor) {
                // Boost speed on fire floor
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.5);
            } else if (!onFireFloor && wasOnFireFloor) {
                // Reset speed
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
            }

            // Fire particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.15, 0.15, 0.15, 0.02);
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Ignite target on contact
            living.setRemainingFireTicks(80); // 4 seconds of fire

            // Sound effect
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.0f, 1.5f);
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Water bucket = instant kill
        if (source.getDirectEntity() instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.is(Items.WATER_BUCKET)) {
                // Instant kill by water
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 1.0f);
                this.discard();
                return true;
            }
        }

        // Water damage source = instant kill
        if (source.type().msgId().contains("drown")) {
            this.discard();
            return true;
        }

        // Snowball hits - 2 kills
        if (source.getDirectEntity() instanceof Snowball) {
            snowballHits++;
            if (snowballHits >= SNOWBALL_HITS_TO_KILL) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
                }
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 0.8f);
                this.discard();
                return true;
            }
            return super.hurt(source, 1.0f);
        }

        return super.hurt(source, amount);
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
