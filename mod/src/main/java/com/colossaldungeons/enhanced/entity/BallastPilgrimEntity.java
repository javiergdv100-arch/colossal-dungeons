package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Ballast Pilgrim - Worldbearer dungeon creature.
 *
 * A fanatical stone humanoid that carries a massive counterweight block on its back.
 * Moves slowly due to the weight but hits hard. On death, drops a heavy block that
 * can activate pressure plates and counterweight mechanisms in the dungeon.
 *
 * Special mechanic: A fishing rod can hook and remove its ballast, making it faster
 * but weaker and causing it to drop its counterweight early.
 *
 * Stats: 24 HP, 2 armor, 7 damage.
 * Traits: Slow (0.13 speed with ballast, 0.25 without).
 */
public class BallastPilgrimEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> HAS_BALLAST =
        SynchedEntityData.defineId(BallastPilgrimEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_LOADED = RawAnimation.begin().thenLoop("animation.ballast_pilgrim.idle_loaded");
    private static final RawAnimation MOVE_LOADED = RawAnimation.begin().thenLoop("animation.ballast_pilgrim.move_loaded");
    private static final RawAnimation IDLE_UNLOADED = RawAnimation.begin().thenLoop("animation.ballast_pilgrim.idle_unloaded");
    private static final RawAnimation MOVE_UNLOADED = RawAnimation.begin().thenLoop("animation.ballast_pilgrim.move_unloaded");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ballast_pilgrim.attack");
    private static final RawAnimation DROP_BALLAST = RawAnimation.begin().thenPlay("animation.ballast_pilgrim.drop_ballast");

    private boolean justDroppedBallast = false;
    private int dropAnimTimer = 0;

    public BallastPilgrimEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Ballast Pilgrim.
     * 24 HP, 0.13 speed (loaded), 7 damage, 2 armor, 0.4 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.13)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_BALLAST, true);
    }

    public boolean hasBallast() {
        return this.entityData.get(HAS_BALLAST);
    }

    private void setHasBallast(boolean hasBallast) {
        this.entityData.set(HAS_BALLAST, hasBallast);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Handle drop animation timer
            if (justDroppedBallast) {
                dropAnimTimer--;
                if (dropAnimTimer <= 0) {
                    justDroppedBallast = false;
                }
            }

            // Update speed based on ballast state
            if (!hasBallast()) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0); // Weaker without weight
            }
        }
    }

    /**
     * Fishing rod interaction removes the ballast from the pilgrim.
     * This makes it faster but weaker and drops the counterweight block.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide() && hasBallast()) {
            // Check if damage was from fishing rod hook (indicated by indirect mob attack)
            if (source.getEntity() instanceof Player player) {
                if (player.getMainHandItem().is(Items.FISHING_ROD) ||
                    player.getOffhandItem().is(Items.FISHING_ROD)) {
                    removeBallast();
                }
            }
        }
        return result;
    }

    /**
     * Removes the ballast, dropping a heavy block at the entity's position.
     */
    private void removeBallast() {
        if (!hasBallast()) return;

        setHasBallast(false);
        justDroppedBallast = true;
        dropAnimTimer = 20;

        // Drop heavy block at current position
        BlockPos dropPos = this.blockPosition();
        if (this.level() instanceof ServerLevel serverLevel) {
            // Place a heavy block that can activate mechanisms
            serverLevel.setBlock(dropPos, Blocks.ANVIL.defaultBlockState(), 3);
            this.level().playSound(null, dropPos,
                SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
    }

    /**
     * On death, drops the counterweight block if still carrying it.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);

        if (hasBallast()) {
            BlockPos deathPos = this.blockPosition();
            serverLevel.setBlock(deathPos, Blocks.ANVIL.defaultBlockState(), 3);
            this.level().playSound(null, deathPos,
                SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 0.5f);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (justDroppedBallast) {
                return state.setAndContinue(DROP_BALLAST);
            }
            if (hasBallast()) {
                if (state.isMoving()) {
                    return state.setAndContinue(MOVE_LOADED);
                }
                return state.setAndContinue(IDLE_LOADED);
            } else {
                if (state.isMoving()) {
                    return state.setAndContinue(MOVE_UNLOADED);
                }
                return state.setAndContinue(IDLE_UNLOADED);
            }
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(hasBallast() ? IDLE_LOADED : IDLE_UNLOADED);
        }));
    }
}
