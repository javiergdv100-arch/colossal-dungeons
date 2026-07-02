package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Lesser Atlas - Worldbearer dungeon elite creature.
 *
 * A smaller cousin of the Dying Atlas that holds up a section of the dungeon ceiling.
 * Killing it causes a controlled ceiling collapse in the area (spawns falling blocks
 * as a death event). Players face a strategic choice: kill it to open a new path through
 * the rubble, or leave it alive to prevent a dangerous collapse zone.
 *
 * Stats: 140 HP, 8 armor, 12 damage.
 * Traits: Extremely slow, high knockback resistance, does not pursue far.
 * Death Event: Spawns 15-25 falling stone blocks above in a 5-block radius.
 */
public class LesserAtlasEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_HOLDING_CEILING =
        SynchedEntityData.defineId(LesserAtlasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_STRAINED =
        SynchedEntityData.defineId(LesserAtlasEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float STRAIN_THRESHOLD = 0.4f; // Shows strain below 40% HP
    private static final int COLLAPSE_RADIUS = 5;
    private static final int MIN_FALLING_BLOCKS = 15;
    private static final int MAX_FALLING_BLOCKS = 25;
    private static final float COLLAPSE_DAMAGE = 8.0f;

    private static final RawAnimation IDLE_HOLDING = RawAnimation.begin().thenLoop("animation.lesser_atlas.idle_holding");
    private static final RawAnimation IDLE_STRAINED = RawAnimation.begin().thenLoop("animation.lesser_atlas.idle_strained");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.lesser_atlas.attack");
    private static final RawAnimation COLLAPSE = RawAnimation.begin().thenPlay("animation.lesser_atlas.collapse");

    public LesserAtlasEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Lesser Atlas.
     * 140 HP, 0.08 speed, 12 damage, 8 armor, 0.9 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 140.0)
            .add(Attributes.MOVEMENT_SPEED, 0.08)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 8.0); // Doesn't pursue far
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_HOLDING_CEILING, true);
        builder.define(IS_STRAINED, false);
    }

    public boolean isHoldingCeiling() {
        return this.entityData.get(IS_HOLDING_CEILING);
    }

    public boolean isStrained() {
        return this.entityData.get(IS_STRAINED);
    }

    private void setStrained(boolean strained) {
        this.entityData.set(IS_STRAINED, strained);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        // No strolling - stays in place holding ceiling
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Update strained visual when health drops below threshold
            boolean shouldStrain = this.getHealth() / this.getMaxHealth() < STRAIN_THRESHOLD;
            if (shouldStrain != isStrained()) {
                setStrained(shouldStrain);
            }
        }
    }

    /**
     * On death, triggers the ceiling collapse event.
     * Spawns falling blocks above the entity's position to simulate structural failure.
     */
    @Override
    protected void tickDeath() {
        ++this.deathTime;

        // Trigger collapse on first death tick
        if (this.deathTime == 1 && !this.level().isClientSide()) {
            triggerCeilingCollapse();
        }

        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    /**
     * Triggers the ceiling collapse, spawning falling blocks and damaging nearby entities.
     */
    private void triggerCeilingCollapse() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        BlockPos basePos = this.blockPosition();

        // Play collapse sound
        this.level().playSound(null, basePos,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0f, 0.5f);

        // Spawn falling blocks from above
        int blockCount = MIN_FALLING_BLOCKS + this.random.nextInt(MAX_FALLING_BLOCKS - MIN_FALLING_BLOCKS + 1);
        for (int i = 0; i < blockCount; i++) {
            int offsetX = this.random.nextInt(COLLAPSE_RADIUS * 2 + 1) - COLLAPSE_RADIUS;
            int offsetZ = this.random.nextInt(COLLAPSE_RADIUS * 2 + 1) - COLLAPSE_RADIUS;
            int height = 5 + this.random.nextInt(4); // 5-8 blocks above

            BlockPos fallingPos = basePos.offset(offsetX, height, offsetZ);

            // Spawn a falling block entity
            net.minecraft.world.entity.item.FallingBlockEntity fallingBlock =
                net.minecraft.world.entity.item.FallingBlockEntity.fall(
                    serverLevel, fallingPos, Blocks.STONE.defaultBlockState());
            if (fallingBlock != null) {
                fallingBlock.setHurtsEntities(COLLAPSE_DAMAGE, 40);
            }
        }

        // Damage entities in the collapse zone
        AABB collapseBox = new AABB(
            basePos.offset(-COLLAPSE_RADIUS, 0, -COLLAPSE_RADIUS),
            basePos.offset(COLLAPSE_RADIUS, 6, COLLAPSE_RADIUS));

        List<LivingEntity> victims = this.level().getEntitiesOfClass(
            LivingEntity.class, collapseBox, e -> e != this && !e.isSpectator());

        for (LivingEntity victim : victims) {
            victim.hurt(this.damageSources().fallingBlock(this), COLLAPSE_DAMAGE);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(COLLAPSE);
            }
            if (isStrained()) {
                return state.setAndContinue(IDLE_STRAINED);
            }
            return state.setAndContinue(IDLE_HOLDING);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(isStrained() ? IDLE_STRAINED : IDLE_HOLDING);
        }));
    }
}
