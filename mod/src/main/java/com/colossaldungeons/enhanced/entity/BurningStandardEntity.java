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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Burning Standard - A standard-bearer from the Solar Palace.
 *
 * Buffs nearby allies (+30% damage, +20% speed in 8-block radius).
 * Not very aggressive, hides behind allies.
 * Water extinguishes standard (removes buff).
 * Fishing rod yanks standard away.
 *
 * Stats: 30 HP, 3 armor, 0.25 speed, 6 damage.
 */
public class BurningStandardEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_STANDARD_LIT =
        SynchedEntityData.defineId(BurningStandardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double BUFF_RANGE = 8.0;
    private static final int BUFF_DURATION = 60; // Re-applied every 3 seconds
    private static final int EXTINGUISH_DURATION = 200; // 10 seconds before re-igniting
    private int buffCooldown = 0;
    private int relightTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.burning_standard.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.burning_standard.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.burning_standard.attack");
    private static final RawAnimation EXTINGUISHED = RawAnimation.begin().thenLoop("animation.burning_standard.extinguished");
    private static final RawAnimation BUFF_PULSE = RawAnimation.begin().thenPlay("animation.burning_standard.buff");

    public BurningStandardEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Burning Standard.
     * 30 HP, 3 armor, 0.25 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_STANDARD_LIT, true);
    }

    public boolean isStandardLit() {
        return this.entityData.get(IS_STANDARD_LIT);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Avoid player - hides behind allies
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6.0f, 1.2, 1.5));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (buffCooldown > 0) buffCooldown--;

            // Handle extinguished state
            if (!isStandardLit()) {
                relightTimer--;
                if (relightTimer <= 0) {
                    // Re-ignite
                    this.entityData.set(IS_STANDARD_LIT, true);
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.0f, 1.0f);
                }
                return;
            }

            // Buff nearby allies
            if (buffCooldown <= 0) {
                applyBuffToAllies();
                buffCooldown = BUFF_DURATION;
            }

            // Fire particles from standard
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 2.0, this.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    /**
     * Applies damage and speed buffs to all nearby allies.
     * +30% damage, +20% speed in 8-block radius.
     */
    private void applyBuffToAllies() {
        AABB area = this.getBoundingBox().inflate(BUFF_RANGE);
        List<Mob> nearbyMobs = this.level().getEntitiesOfClass(Mob.class, area,
            mob -> mob != this && !(mob instanceof Player));

        for (Mob mob : nearbyMobs) {
            if (this.distanceTo(mob) <= BUFF_RANGE) {
                // +30% damage buff (strength)
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_DURATION + 10, 0, false, true));
                // +20% speed buff
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_DURATION + 10, 0, false, true));
            }
        }

        // Buff pulse particles
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                this.getX(), this.getY() + 1.5, this.getZ(), 8, 2.0, 0.5, 2.0, 0.05);
        }
    }

    /**
     * Extinguishes the standard, removing all buffs from nearby allies.
     */
    public void extinguishStandard() {
        if (isStandardLit()) {
            this.entityData.set(IS_STANDARD_LIT, false);
            relightTimer = EXTINGUISH_DURATION;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 1.0f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Water extinguishes the standard
        if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
            extinguishStandard();
        }

        // Check for water bucket interaction
        if (source.getDirectEntity() instanceof Player player) {
            if (player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET)) {
                extinguishStandard();
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (!isStandardLit()) {
                return state.setAndContinue(EXTINGUISHED);
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
