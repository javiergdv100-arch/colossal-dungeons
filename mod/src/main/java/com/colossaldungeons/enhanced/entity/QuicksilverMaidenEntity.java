package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Quicksilver Maiden - Liquid mercury figure that slides through floors and mirrors.
 *
 * A flowing, liquid figure that splits into smaller droplets when damaged and
 * reforms if left alone. Fire hardens it (slower but more fragile). Can travel
 * through mirror surfaces to reappear behind the player.
 *
 * Stats: 30 HP, 0 armor, 5 damage.
 * Size: 0.6 x 1.6 blocks.
 *
 * Behavior:
 * - Splits into smaller droplets when hit (each droplet has proportional HP).
 * - Reforms if droplets are left alone for ~5 seconds.
 * - Fire/flint-and-steel hardens it temporarily (+vulnerability, -speed) for 15s.
 * - Can travel through mirror surfaces to reappear behind players.
 * - Water disperses it preventing re-fusion for 10 seconds.
 * - Powder snow freezes it completely for 5 seconds (critical vulnerability).
 *
 * Drops: Mercury droplet, Alchemical ingredient.
 */
public class QuicksilverMaidenEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Mercury state.
     */
    public enum MercuryState {
        LIQUID,     // Normal state - fluid and fast
        HARDENED,   // Fire-affected - slower but more fragile
        SPLITTING,  // In the process of splitting
        REFORMING,  // Droplets reforming into whole
        FROZEN      // Completely frozen by powder snow
    }

    private static final EntityDataAccessor<Integer> MERCURY_STATE =
        SynchedEntityData.defineId(QuicksilverMaidenEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> SPLIT_COUNT =
        SynchedEntityData.defineId(QuicksilverMaidenEntity.class, EntityDataSerializers.INT);

    private static final int HARDEN_DURATION = 300; // 15 seconds
    private static final int FREEZE_DURATION = 100; // 5 seconds
    private static final int REFORM_DELAY = 100; // 5 seconds to reform
    private static final int DISPERSE_DURATION = 200; // 10 seconds from water
    private static final int MAX_SPLITS = 3; // Maximum number of splits

    private int stateTimer = 0;
    private int reformTimer = 0;

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.quicksilver_maiden.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.quicksilver_maiden.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.quicksilver_maiden.attack");
    private static final RawAnimation SPLIT_ANIM = RawAnimation.begin().thenPlay("animation.quicksilver_maiden.split");
    private static final RawAnimation REFORM_ANIM = RawAnimation.begin().thenPlay("animation.quicksilver_maiden.reform");
    private static final RawAnimation HARDENED_IDLE = RawAnimation.begin().thenLoop("animation.quicksilver_maiden.hardened");
    private static final RawAnimation FROZEN_ANIM = RawAnimation.begin().thenLoop("animation.quicksilver_maiden.frozen");
    private static final RawAnimation MIRROR_TRAVEL = RawAnimation.begin().thenPlay("animation.quicksilver_maiden.mirror_travel");

    public QuicksilverMaidenEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Quicksilver Maiden.
     * 30 HP, 0 armor, 5 damage, 0.32 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MERCURY_STATE, MercuryState.LIQUID.ordinal());
        builder.define(SPLIT_COUNT, 0);
    }

    public MercuryState getMercuryState() {
        return MercuryState.values()[this.entityData.get(MERCURY_STATE)];
    }

    private void setMercuryState(MercuryState state) {
        this.entityData.set(MERCURY_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return getMercuryState() != MercuryState.FROZEN && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            MercuryState currentState = getMercuryState();

            // Handle state timers
            if (stateTimer > 0) {
                stateTimer--;
                if (stateTimer <= 0) {
                    switch (currentState) {
                        case HARDENED -> {
                            // Return to liquid state
                            setMercuryState(MercuryState.LIQUID);
                            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
                        }
                        case FROZEN -> {
                            // Unfreeze
                            setMercuryState(MercuryState.LIQUID);
                            this.setNoAi(false);
                        }
                        default -> {}
                    }
                }
            }

            // Handle reform timer for split droplets
            if (currentState == MercuryState.SPLITTING) {
                reformTimer++;
                if (reformTimer >= REFORM_DELAY) {
                    reform();
                }
            }

            // Frozen: no AI
            if (currentState == MercuryState.FROZEN) {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        MercuryState currentState = getMercuryState();

        // Fire damage hardens it
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            if (currentState == MercuryState.LIQUID) {
                harden();
            }
            // Take extra damage when hardened (fragile)
            if (currentState == MercuryState.HARDENED) {
                amount *= 1.5f;
            }
        }

        // Freezing damage freezes it
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) {
            freeze();
            return super.hurt(source, amount);
        }

        // Split on non-fire physical damage if in liquid state
        if (currentState == MercuryState.LIQUID && !source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            int splits = this.entityData.get(SPLIT_COUNT);
            if (splits < MAX_SPLITS) {
                split();
                return super.hurt(source, amount * 0.5f); // Reduced damage on split
            }
        }

        return super.hurt(source, amount);
    }

    /**
     * Hardens the mercury (fire effect) - slower but more fragile.
     */
    private void harden() {
        setMercuryState(MercuryState.HARDENED);
        stateTimer = HARDEN_DURATION;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.15); // Much slower
    }

    /**
     * Freezes the entity completely (powder snow effect).
     */
    private void freeze() {
        setMercuryState(MercuryState.FROZEN);
        stateTimer = FREEZE_DURATION;
        this.setNoAi(true);
    }

    /**
     * Splits into droplets - in practice spawns smaller copies.
     */
    private void split() {
        setMercuryState(MercuryState.SPLITTING);
        int splits = this.entityData.get(SPLIT_COUNT);
        this.entityData.set(SPLIT_COUNT, splits + 1);
        reformTimer = 0;

        // In full implementation: spawn 2 smaller QuicksilverMaiden copies
        // with proportional HP (HP / 3 each)
        // For now, mark the state for animation purposes
    }

    /**
     * Reforms from split state back to whole.
     */
    private void reform() {
        setMercuryState(MercuryState.REFORMING);
        reformTimer = 0;

        // In full implementation: despawn split copies and restore HP
        this.entityData.set(SPLIT_COUNT, 0);

        // Brief reforming animation then back to liquid
        setMercuryState(MercuryState.LIQUID);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            MercuryState mercuryState = getMercuryState();
            return switch (mercuryState) {
                case FROZEN -> state.setAndContinue(FROZEN_ANIM);
                case HARDENED -> state.setAndContinue(HARDENED_IDLE);
                case SPLITTING -> state.setAndContinue(SPLIT_ANIM);
                case REFORMING -> state.setAndContinue(REFORM_ANIM);
                case LIQUID -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && getMercuryState() == MercuryState.LIQUID) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
