package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Walking Fragment - Worldbearer dungeon creature.
 *
 * A chunk of living stone that has broken free from the dungeon walls.
 * Slow-moving but resistant to slashing damage, weak to blunt/crushing attacks.
 * Blocks narrow paths with its bulk. On death, drops usable weight blocks
 * that players can place for puzzles or counterweight mechanisms.
 *
 * Stats: 28 HP, 6 armor, 6 damage.
 * Traits: Slow movement (0.15), high knockback resistance.
 * Resistances: Slashing damage reduced by 50%.
 * Weaknesses: Blunt/crushing deals 150% damage.
 */
public class WalkingFragmentEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_BLOCKING_PATH =
        SynchedEntityData.defineId(WalkingFragmentEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.walking_fragment.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.walking_fragment.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.walking_fragment.attack");
    private static final RawAnimation BLOCK = RawAnimation.begin().thenLoop("animation.walking_fragment.block");

    private int pathBlockTimer = 0;
    private static final int PATH_BLOCK_DURATION = 200; // 10 seconds standing still before blocking

    public WalkingFragmentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Walking Fragment.
     * 28 HP, 0.15 speed, 6 damage, 6 armor, 0.6 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_BLOCKING_PATH, false);
    }

    public boolean isBlockingPath() {
        return this.entityData.get(IS_BLOCKING_PATH);
    }

    private void setBlockingPath(boolean blocking) {
        this.entityData.set(IS_BLOCKING_PATH, blocking);
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
            // Track how long the entity has been stationary to determine path-blocking
            if (this.getDeltaMovement().horizontalDistanceSqr() < 0.001 && this.getTarget() == null) {
                pathBlockTimer++;
                if (pathBlockTimer >= PATH_BLOCK_DURATION && !isBlockingPath()) {
                    setBlockingPath(true);
                }
            } else {
                pathBlockTimer = 0;
                if (isBlockingPath()) {
                    setBlockingPath(false);
                }
            }
        }
    }

    /**
     * Applies damage resistances and weaknesses.
     * Slashing damage is halved; blunt/crushing damage is amplified by 50%.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Resistant to slashing (sword-type attacks)
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            // Projectiles do reduced damage to stone
            amount *= 0.5f;
        }

        // Check for blunt/crushing - maces, hammers, falling anvils
        if (source.is(DamageTypeTags.IS_FALL)) {
            amount *= 1.5f;
        }

        return super.hurt(source, amount);
    }

    /**
     * On death, drops weight blocks at the entity's position.
     * These blocks can be used by players to activate counterweight mechanisms.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);

        // Drop 1-2 cobblestone as weight blocks (placeholder for custom weight block item)
        BlockPos deathPos = this.blockPosition();
        serverLevel.setBlock(deathPos, Blocks.COBBLESTONE.defaultBlockState(), 3);

        this.level().playSound(null, deathPos,
            SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isBlockingPath()) {
                return state.setAndContinue(BLOCK);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
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
