package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Gilded Guard - A golden-armored guard from the Solar Palace.
 *
 * Frontal attacks under sunlight blind the attacker (Blindness 2s).
 * High frontal armor, WEAK from behind. Gold ingot bribes (ignores player 30s).
 * Water bucket dulls armor (no blind, reduced armor 10s).
 *
 * Stats: 40 HP, 8 armor, 0.22 speed, 8 damage.
 */
public class GildedGuardEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_DULLED =
        SynchedEntityData.defineId(GildedGuardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_BRIBED =
        SynchedEntityData.defineId(GildedGuardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int DULL_DURATION = 200; // 10 seconds
    private static final int BRIBE_DURATION = 600; // 30 seconds
    private static final int BLINDNESS_DURATION = 40; // 2 seconds
    private static final int LIGHT_THRESHOLD = 12;
    private static final float BACK_ATTACK_MULTIPLIER = 2.5f;

    private int dullTimer = 0;
    private int bribeTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gilded_guard.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.gilded_guard.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.gilded_guard.attack");
    private static final RawAnimation DULLED = RawAnimation.begin().thenLoop("animation.gilded_guard.dulled");

    public GildedGuardEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Gilded Guard.
     * 40 HP, 8 armor, 0.22 speed, 8 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_DULLED, false);
        builder.define(IS_BRIBED, false);
    }

    public boolean isDulled() {
        return this.entityData.get(IS_DULLED);
    }

    public boolean isBribed() {
        return this.entityData.get(IS_BRIBED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Dull timer
            if (dullTimer > 0) {
                dullTimer--;
                if (dullTimer <= 0) {
                    this.entityData.set(IS_DULLED, false);
                    this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
                }
            }

            // Bribe timer
            if (bribeTimer > 0) {
                bribeTimer--;
                if (bribeTimer <= 0) {
                    this.entityData.set(IS_BRIBED, false);
                }
            }

            // If bribed, don't target players
            if (isBribed()) {
                this.setTarget(null);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        // Gold ingot bribe
        if (heldItem.is(Items.GOLD_INGOT) && !isBribed()) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }
            this.entityData.set(IS_BRIBED, true);
            bribeTimer = BRIBE_DURATION;
            this.setTarget(null);

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.HOSTILE, 1.5f, 1.2f);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            // Check if attack is from behind
            Vec3 attackDir = attacker.position().subtract(this.position()).normalize();
            Vec3 lookDir = this.getLookAngle();
            double dot = attackDir.dot(lookDir);

            if (dot < -0.3) {
                // Attack from behind - weak spot
                amount *= BACK_ATTACK_MULTIPLIER;
            } else {
                // Frontal attack - check for blinding in sunlight
                int lightLevel = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
                boolean inSunlight = lightLevel >= LIGHT_THRESHOLD;

                if (inSunlight && !isDulled() && attacker instanceof Player player) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, 0, false, true));
                }
            }

            // Water bucket dulls armor
            if (attacker instanceof Player player) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.is(Items.WATER_BUCKET) && !isDulled()) {
                    this.entityData.set(IS_DULLED, true);
                    dullTimer = DULL_DURATION;
                    this.getAttribute(Attributes.ARMOR).setBaseValue(3.0);

                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2f, 1.0f);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isDulled()) {
                return state.setAndContinue(DULLED);
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
