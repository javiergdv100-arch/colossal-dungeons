package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Counterweight Guardian - Worldbearer dungeon creature.
 *
 * A massive stone automaton wielding a charged mace. The longer it winds up its attack,
 * the more damage and knockback it deals. After a fully charged attack, it enters
 * a lengthy recovery animation during which it is vulnerable to counterattack.
 *
 * Stats: 46 HP, 6 armor, 10 damage base (up to 20 with full charge), knockback.
 * Traits: Medium speed (0.2), high knockback resistance.
 * Weakness: Long recovery period (3 seconds) after charged attack.
 */
public class CounterweightGuardianEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Attack phase for the charged mace mechanic.
     */
    public enum ChargeState {
        IDLE,
        CHARGING,
        RELEASING,
        RECOVERING
    }

    private static final EntityDataAccessor<Integer> CHARGE_STATE =
        SynchedEntityData.defineId(CounterweightGuardianEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CHARGE_LEVEL =
        SynchedEntityData.defineId(CounterweightGuardianEntity.class, EntityDataSerializers.INT);

    private static final int MAX_CHARGE_TICKS = 60; // 3 seconds max charge
    private static final int RECOVERY_DURATION = 60; // 3 seconds recovery (vulnerability window)
    private static final float BASE_DAMAGE = 10.0f;
    private static final float MAX_CHARGED_DAMAGE = 20.0f;
    private static final float BASE_KNOCKBACK = 1.0f;
    private static final float MAX_KNOCKBACK = 4.0f;
    private static final double MACE_REACH = 3.5;

    private int chargeTicks = 0;
    private int recoveryTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.counterweight_guardian.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.counterweight_guardian.move");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.counterweight_guardian.charge");
    private static final RawAnimation RELEASE = RawAnimation.begin().thenPlay("animation.counterweight_guardian.release");
    private static final RawAnimation RECOVER = RawAnimation.begin().thenLoop("animation.counterweight_guardian.recover");

    public CounterweightGuardianEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Counterweight Guardian.
     * 46 HP, 0.2 speed, 10 damage, 6 armor, 0.7 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 46.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE_STATE, ChargeState.IDLE.ordinal());
        builder.define(CHARGE_LEVEL, 0);
    }

    public ChargeState getChargeState() {
        return ChargeState.values()[this.entityData.get(CHARGE_STATE)];
    }

    private void setChargeState(ChargeState state) {
        this.entityData.set(CHARGE_STATE, state.ordinal());
    }

    public int getChargeLevel() {
        return this.entityData.get(CHARGE_LEVEL);
    }

    private void setChargeLevel(int level) {
        this.entityData.set(CHARGE_LEVEL, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            ChargeState currentState = getChargeState();

            switch (currentState) {
                case IDLE -> {
                    // Start charging when target is in range
                    LivingEntity target = this.getTarget();
                    if (target != null && this.distanceTo(target) <= MACE_REACH + 2.0) {
                        setChargeState(ChargeState.CHARGING);
                        chargeTicks = 0;
                    }
                }
                case CHARGING -> {
                    chargeTicks++;
                    setChargeLevel(chargeTicks);

                    LivingEntity target = this.getTarget();
                    // Release if max charge reached or target moves out of range
                    if (chargeTicks >= MAX_CHARGE_TICKS ||
                        (target != null && this.distanceTo(target) > MACE_REACH + 4.0)) {
                        releaseCharge();
                    }
                    // Also release if target is close and charge is at least half
                    else if (target != null && this.distanceTo(target) <= MACE_REACH && chargeTicks >= 20) {
                        releaseCharge();
                    }
                }
                case RELEASING -> {
                    // Short release animation then go to recovery
                    chargeTicks--;
                    if (chargeTicks <= 0) {
                        setChargeState(ChargeState.RECOVERING);
                        recoveryTimer = RECOVERY_DURATION;
                    }
                }
                case RECOVERING -> {
                    recoveryTimer--;
                    if (recoveryTimer <= 0) {
                        setChargeState(ChargeState.IDLE);
                        setChargeLevel(0);
                    }
                }
            }
        }
    }

    /**
     * Releases the charged mace attack, dealing damage based on charge duration.
     */
    private void releaseCharge() {
        setChargeState(ChargeState.RELEASING);
        int charge = chargeTicks;
        chargeTicks = 10; // Short release window

        // Calculate damage and knockback based on charge
        float chargeFraction = Math.min(1.0f, (float) charge / MAX_CHARGE_TICKS);
        float damage = BASE_DAMAGE + (MAX_CHARGED_DAMAGE - BASE_DAMAGE) * chargeFraction;
        float knockback = BASE_KNOCKBACK + (MAX_KNOCKBACK - BASE_KNOCKBACK) * chargeFraction;

        // Hit all entities in front within reach
        AABB hitBox = this.getBoundingBox().inflate(MACE_REACH);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, hitBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            if (this.distanceTo(target) <= MACE_REACH) {
                target.hurt(source, damage);
                Vec3 knockDir = target.position().subtract(this.position()).normalize();
                target.knockback(knockback, -knockDir.x, -knockDir.z);
                target.hurtMarked = true;
            }
        }

        // Impact sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f + chargeFraction * 0.5f);
    }

    /**
     * Takes extra damage while recovering (vulnerability window).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getChargeState() == ChargeState.RECOVERING) {
            amount *= 1.5f; // 50% extra damage during recovery
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isNoAi() {
        // Disable movement AI during charge and recovery
        ChargeState state = getChargeState();
        if (state == ChargeState.CHARGING || state == ChargeState.RECOVERING) {
            return true;
        }
        return super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            ChargeState chargeState = getChargeState();
            return switch (chargeState) {
                case CHARGING -> state.setAndContinue(CHARGE);
                case RELEASING -> state.setAndContinue(RELEASE);
                case RECOVERING -> state.setAndContinue(RECOVER);
                case IDLE -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
            };
        }));
    }
}
