package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Mimic entity - disguises itself as a chest and ambushes players.
 *
 * Has three states:
 * - DISGUISED: looks like a chest, immobile, invulnerable appearance
 * - AWAKENING: transitional animation when player gets within 3 blocks
 * - ACTIVE: fully mobile, attacks the player
 *
 * Stats: 60 HP, 0.3 speed (when active), 10 damage, 0.4 knockback resistance.
 */
public class MimicEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Mimic behavioral states.
     */
    public enum MimicState {
        DISGUISED,
        AWAKENING,
        ACTIVE
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.INT);

    private static final double ACTIVATION_RANGE = 3.0;
    private int awakeningTimer = 0;
    private static final int AWAKENING_DURATION = 20; // 1 second

    private static final RawAnimation DISGUISE = RawAnimation.begin().thenLoop("animation.mimic.disguise");
    private static final RawAnimation AWAKEN = RawAnimation.begin().thenPlay("animation.mimic.awaken");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mimic.idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.mimic.attack");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.mimic.move");

    public MimicEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Mimic.
     * 60 HP, 0.3 speed, 10 damage, 0.4 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, MimicState.DISGUISED.ordinal());
    }

    public MimicState getMimicState() {
        return MimicState.values()[this.entityData.get(STATE)];
    }

    public void setMimicState(MimicState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            MimicState currentState = getMimicState();

            switch (currentState) {
                case DISGUISED -> {
                    // Check for nearby players
                    Player nearest = this.level().getNearestPlayer(this, ACTIVATION_RANGE);
                    if (nearest != null) {
                        setMimicState(MimicState.AWAKENING);
                        awakeningTimer = AWAKENING_DURATION;
                    }
                }
                case AWAKENING -> {
                    awakeningTimer--;
                    if (awakeningTimer <= 0) {
                        setMimicState(MimicState.ACTIVE);
                    }
                }
                case ACTIVE -> {
                    // Normal AI takes over - goals handle attack behavior
                }
            }
        }
    }

    @Override
    public boolean isNoAi() {
        // Disable AI while disguised
        if (getMimicState() == MimicState.DISGUISED || getMimicState() == MimicState.AWAKENING) {
            return true;
        }
        return super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main controller: handles state-based animations
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            MimicState mimicState = getMimicState();
            return switch (mimicState) {
                case DISGUISED -> state.setAndContinue(DISGUISE);
                case AWAKENING -> state.setAndContinue(AWAKEN);
                case ACTIVE -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
            };
        }));

        // Attack controller
        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (getMimicState() == MimicState.ACTIVE && this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
