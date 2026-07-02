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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Unstable Golem - An elemental elite from the Primordial Tower.
 *
 * Cycles between fire/water/air/earth elements every few seconds, changing
 * attacks AND weaknesses. Opposite element deals x3 damage (water vs fire,
 * lava vs water, snowball vs air, fire vs earth). Brief vulnerability
 * during element transition. Spyglass predicts next element by core color.
 *
 * Stats: 150 HP, 6 armor, 0.22 speed, 12 damage.
 */
public class UnstableGolemEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Elemental states the golem cycles through.
     */
    public enum Element {
        FIRE,
        WATER,
        AIR,
        EARTH
    }

    private static final EntityDataAccessor<Integer> CURRENT_ELEMENT =
        SynchedEntityData.defineId(UnstableGolemEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_TRANSITIONING =
        SynchedEntityData.defineId(UnstableGolemEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ELEMENT_CYCLE_TICKS = 160; // 8 seconds per element
    private static final int TRANSITION_DURATION = 30; // 1.5 seconds vulnerability
    private static final float OPPOSITE_ELEMENT_MULTIPLIER = 3.0f;

    private int elementCycleTimer = 0;
    private int transitionTimer = 0;

    private static final RawAnimation IDLE_FIRE = RawAnimation.begin().thenLoop("animation.unstable_golem.idle_fire");
    private static final RawAnimation IDLE_WATER = RawAnimation.begin().thenLoop("animation.unstable_golem.idle_water");
    private static final RawAnimation IDLE_AIR = RawAnimation.begin().thenLoop("animation.unstable_golem.idle_air");
    private static final RawAnimation IDLE_EARTH = RawAnimation.begin().thenLoop("animation.unstable_golem.idle_earth");
    private static final RawAnimation TRANSITION = RawAnimation.begin().thenPlay("animation.unstable_golem.transition");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.unstable_golem.attack");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.unstable_golem.move");

    public UnstableGolemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Unstable Golem.
     * 150 HP, 6 armor, 0.22 speed, 12 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 150.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURRENT_ELEMENT, Element.FIRE.ordinal());
        builder.define(IS_TRANSITIONING, false);
    }

    public Element getCurrentElement() {
        return Element.values()[this.entityData.get(CURRENT_ELEMENT)];
    }

    private void setCurrentElement(Element element) {
        this.entityData.set(CURRENT_ELEMENT, element.ordinal());
    }

    public boolean isTransitioning() {
        return this.entityData.get(IS_TRANSITIONING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.7));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Handle element cycling
            if (isTransitioning()) {
                transitionTimer--;
                if (transitionTimer <= 0) {
                    this.entityData.set(IS_TRANSITIONING, false);
                }
            } else {
                elementCycleTimer++;
                if (elementCycleTimer >= ELEMENT_CYCLE_TICKS) {
                    cycleElement();
                    elementCycleTimer = 0;
                }
            }

            // Element-specific particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
                spawnElementParticles(serverLevel);
            }
        }
    }

    /**
     * Cycles to the next element in sequence.
     */
    private void cycleElement() {
        Element current = getCurrentElement();
        Element next = Element.values()[(current.ordinal() + 1) % Element.values().length];
        setCurrentElement(next);

        // Begin transition period (vulnerability window)
        this.entityData.set(IS_TRANSITIONING, true);
        transitionTimer = TRANSITION_DURATION;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    /**
     * Spawns particles based on current element.
     */
    private void spawnElementParticles(ServerLevel level) {
        switch (getCurrentElement()) {
            case FIRE -> level.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
            case WATER -> level.sendParticles(ParticleTypes.DRIPPING_WATER,
                this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
            case AIR -> level.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
            case EARTH -> level.sendParticles(ParticleTypes.CRIT,
                this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (hit && target instanceof LivingEntity living) {
            // Element-specific attack effects
            switch (getCurrentElement()) {
                case FIRE -> living.setRemainingFireTicks(60);
                case WATER -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                case AIR -> {
                    // Knockback
                    var dir = target.position().subtract(this.position()).normalize();
                    living.push(dir.x * 1.5, 0.4, dir.z * 1.5);
                }
                case EARTH -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 127));
            }
        }

        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // During transition, takes extra damage
        if (isTransitioning()) {
            amount *= 1.5f;
        }

        // Check for opposite element bonus damage
        Element current = getCurrentElement();
        float modifiedAmount = amount;

        switch (current) {
            case FIRE -> {
                // Water vs fire = x3
                if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
                    modifiedAmount *= OPPOSITE_ELEMENT_MULTIPLIER;
                }
            }
            case WATER -> {
                // Lava/fire vs water = x3
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    modifiedAmount *= OPPOSITE_ELEMENT_MULTIPLIER;
                }
            }
            case AIR -> {
                // Snowball vs air = x3
                if (source.getDirectEntity() instanceof Snowball) {
                    modifiedAmount = this.getMaxHealth() * 0.1f * OPPOSITE_ELEMENT_MULTIPLIER; // Snowballs deal significant damage
                }
            }
            case EARTH -> {
                // Fire vs earth = x3
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    modifiedAmount *= OPPOSITE_ELEMENT_MULTIPLIER;
                }
            }
        }

        return super.hurt(source, modifiedAmount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isTransitioning()) {
                return state.setAndContinue(TRANSITION);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return switch (getCurrentElement()) {
                case FIRE -> state.setAndContinue(IDLE_FIRE);
                case WATER -> state.setAndContinue(IDLE_WATER);
                case AIR -> state.setAndContinue(IDLE_AIR);
                case EARTH -> state.setAndContinue(IDLE_EARTH);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE_FIRE);
        }));
    }
}
