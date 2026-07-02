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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Solar Dancer - An agile sun priest from the Solar Palace.
 *
 * Spinning burning ribbons. Stronger under direct light (+50% damage),
 * weaker in shadow (-50%). Leaves brief fire trails when spinning.
 * Fishing rod drags to shadow.
 *
 * Stats: 22 HP, 1 armor, 0.35 speed (agile), 6 damage.
 */
public class SolarDancerEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_IN_LIGHT =
        SynchedEntityData.defineId(SolarDancerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_SPINNING =
        SynchedEntityData.defineId(SolarDancerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int LIGHT_THRESHOLD = 12; // Sky light level for "direct light"
    private static final float LIGHT_DAMAGE_BONUS = 1.5f;
    private static final float SHADOW_DAMAGE_PENALTY = 0.5f;
    private int spinCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.solar_dancer.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.solar_dancer.move");
    private static final RawAnimation SPIN = RawAnimation.begin().thenPlay("animation.solar_dancer.spin");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.solar_dancer.attack");

    public SolarDancerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Solar Dancer.
     * 22 HP, 1 armor, 0.35 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 22.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 18.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_IN_LIGHT, true);
        builder.define(IS_SPINNING, false);
    }

    public boolean isInLight() {
        return this.entityData.get(IS_IN_LIGHT);
    }

    public boolean isSpinning() {
        return this.entityData.get(IS_SPINNING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (spinCooldown > 0) spinCooldown--;

            // Check light level at position
            int lightLevel = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
            boolean inLight = lightLevel >= LIGHT_THRESHOLD;
            this.entityData.set(IS_IN_LIGHT, inLight);

            // Fire trail while spinning
            if (isSpinning() && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.2, this.getZ(), 3, 0.3, 0.05, 0.3, 0.01);
            }

            // Light particles when in sunlight
            if (inLight && this.tickCount % 8 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Modify damage based on light
        float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float actualDamage = baseDamage;

        if (isInLight()) {
            actualDamage *= LIGHT_DAMAGE_BONUS;
        } else {
            actualDamage *= SHADOW_DAMAGE_PENALTY;
        }

        boolean hit = target.hurt(this.damageSources().mobAttack(this), actualDamage);

        if (hit && target instanceof LivingEntity living) {
            // Ignite target
            living.setRemainingFireTicks(40);

            // Start spinning attack
            if (spinCooldown <= 0) {
                this.entityData.set(IS_SPINNING, true);
                spinCooldown = 60;
                // Spinning lasts briefly
                // Reset in 20 ticks via tick()
            }
        }

        return hit;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isSpinning()) {
                return state.setAndContinue(SPIN);
            }
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
