package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Devouring Larva - Hollow Leviathan dungeon creature.
 *
 * A fast-moving digestive organism that crawls through the Leviathan's internal passages.
 * Moves faster on wet surfaces. Eats dropped items in its vicinity and can grow larger
 * with each item consumed, gaining HP and size. Extremely vulnerable to fire damage.
 * Eggs and food items can be used to lure it away from players.
 *
 * Stats: 20 HP, 1 armor, 6 damage.
 * Traits: Fast on wet surfaces (0.35 vs 0.25 normally), eats items, grows, fire-weak.
 */
public class DevouringLarvaEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> GROWTH_STAGE =
        SynchedEntityData.defineId(DevouringLarvaEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> ON_WET_SURFACE =
        SynchedEntityData.defineId(DevouringLarvaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_GROWTH = 5;
    private static final float HP_PER_GROWTH = 8.0f;
    private static final float SIZE_SCALE_PER_GROWTH = 0.15f;
    private static final double ITEM_DETECTION_RANGE = 5.0;
    private static final double LURE_RANGE = 10.0;
    private static final float FIRE_VULNERABILITY_MULTIPLIER = 3.0f;
    private static final float WET_SPEED_BONUS = 0.35f;
    private static final float DRY_SPEED = 0.25f;

    private int itemSearchCooldown = 0;
    private ItemEntity targetItem = null;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.devouring_larva.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.devouring_larva.move");
    private static final RawAnimation MOVE_FAST = RawAnimation.begin().thenLoop("animation.devouring_larva.move_fast");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.devouring_larva.attack");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.devouring_larva.eat");
    private static final RawAnimation GROW = RawAnimation.begin().thenPlay("animation.devouring_larva.grow");

    public DevouringLarvaEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Devouring Larva.
     * 20 HP, 0.25 speed (dry), 6 damage, 1 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH_STAGE, 0);
        builder.define(ON_WET_SURFACE, false);
    }

    public int getGrowthStage() {
        return this.entityData.get(GROWTH_STAGE);
    }

    private void setGrowthStage(int stage) {
        this.entityData.set(GROWTH_STAGE, Math.min(MAX_GROWTH, stage));
    }

    public boolean isOnWetSurface() {
        return this.entityData.get(ON_WET_SURFACE);
    }

    private void setOnWetSurface(boolean wet) {
        this.entityData.set(ON_WET_SURFACE, wet);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Check wet surface for speed bonus
            boolean wet = this.isInWaterOrBubble() ||
                this.level().isRainingAt(this.blockPosition());
            setOnWetSurface(wet);

            // Update speed based on surface
            if (wet) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WET_SPEED_BONUS);
            } else {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(DRY_SPEED);
            }

            // Search for dropped items to eat
            itemSearchCooldown--;
            if (itemSearchCooldown <= 0) {
                searchForItems();
                itemSearchCooldown = 20; // Check every second
            }

            // Move toward target item if found
            if (targetItem != null && !targetItem.isRemoved()) {
                if (this.distanceTo(targetItem) < 1.0) {
                    eatItem(targetItem);
                } else {
                    // Navigate toward the item
                    this.getNavigation().moveTo(targetItem, 1.2);
                }
            }

            // Check for lure items (eggs, food) within range
            checkForLures();
        }
    }

    /**
     * Searches for dropped items nearby to eat.
     */
    private void searchForItems() {
        AABB searchBox = this.getBoundingBox().inflate(ITEM_DETECTION_RANGE);
        List<ItemEntity> items = this.level().getEntitiesOfClass(
            ItemEntity.class, searchBox, e -> !e.isRemoved());

        if (!items.isEmpty()) {
            // Prioritize food items
            targetItem = items.stream()
                .filter(i -> i.getItem().isEdible() || i.getItem().is(Items.EGG))
                .findFirst()
                .orElse(items.get(0));
        } else {
            targetItem = null;
        }
    }

    /**
     * Checks for lure items (eggs, food) that divert the larva from players.
     */
    private void checkForLures() {
        AABB lureBox = this.getBoundingBox().inflate(LURE_RANGE);
        List<ItemEntity> lures = this.level().getEntitiesOfClass(
            ItemEntity.class, lureBox, e -> {
                ItemStack stack = e.getItem();
                return stack.is(Items.EGG) || stack.isEdible();
            });

        if (!lures.isEmpty()) {
            // Drop current player target in favor of lure
            this.setTarget(null);
            targetItem = lures.get(0);
        }
    }

    /**
     * Eats a dropped item, potentially growing larger.
     */
    private void eatItem(ItemEntity item) {
        item.discard();
        targetItem = null;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.0f, 1.2f);

        // Grow if not at max
        int currentGrowth = getGrowthStage();
        if (currentGrowth < MAX_GROWTH) {
            setGrowthStage(currentGrowth + 1);
            // Increase max HP and heal
            float newMaxHp = 20.0f + HP_PER_GROWTH * (currentGrowth + 1);
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHp);
            this.heal(HP_PER_GROWTH);
            // Scale size
            float newScale = 1.0f + SIZE_SCALE_PER_GROWTH * (currentGrowth + 1);
            this.refreshDimensions();
        }
    }

    /**
     * Extremely vulnerable to fire damage.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= FIRE_VULNERABILITY_MULTIPLIER;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (state.isMoving()) {
                if (isOnWetSurface()) {
                    return state.setAndContinue(MOVE_FAST);
                }
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
