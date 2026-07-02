package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Veil Warden - An elite creature from the Veiled Peak dungeon.
 *
 * Invisible in fog, only visible under beacon light. Ambush AI attacks
 * from blind spots. Can extinguish nearby beacons to regain invisibility.
 *
 * Stats: 120 HP, 5 armor, 0.28 speed, 11 attack damage.
 */
public class VeilWardenEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_VISIBLE =
        SynchedEntityData.defineId(VeilWardenEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_AMBUSHING =
        SynchedEntityData.defineId(VeilWardenEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int BEACON_LIGHT_THRESHOLD = 12;
    private static final int BEACON_EXTINGUISH_RANGE = 5;
    private static final int EXTINGUISH_COOLDOWN = 300; // 15 seconds
    private static final int AMBUSH_DAMAGE_MULTIPLIER = 2;
    private static final double AMBUSH_ANGLE_THRESHOLD = -0.3; // Behind target

    private int extinguishCooldown = 0;
    private int invisibilityRecoverTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.veil_warden.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.veil_warden.walk");
    private static final RawAnimation AMBUSH_ATTACK = RawAnimation.begin().thenPlay("animation.veil_warden.ambush");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.veil_warden.attack");
    private static final RawAnimation EXTINGUISH = RawAnimation.begin().thenPlay("animation.veil_warden.extinguish");
    private static final RawAnimation STEALTH = RawAnimation.begin().thenLoop("animation.veil_warden.stealth");

    public VeilWardenEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Veil Warden.
     * 120 HP, 5 armor, 0.28 speed, 11 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_VISIBLE, false);
        builder.define(IS_AMBUSHING, false);
    }

    public boolean isVisible() {
        return this.entityData.get(IS_VISIBLE);
    }

    public boolean isAmbushing() {
        return this.entityData.get(IS_AMBUSHING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (extinguishCooldown > 0) extinguishCooldown--;

            // Check visibility based on light level
            int lightLevel = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());
            boolean underLight = lightLevel >= BEACON_LIGHT_THRESHOLD;
            this.entityData.set(IS_VISIBLE, underLight);

            // If visible and beacons are nearby, attempt to extinguish them
            if (underLight && extinguishCooldown <= 0) {
                attemptExtinguishBeacons();
            }

            // Ambush AI: position behind target
            LivingEntity target = this.getTarget();
            if (target != null && !isVisible()) {
                // Try to get behind the target
                Vec3 targetLook = target.getLookAngle().normalize();
                Vec3 toWarden = this.position().subtract(target.position()).normalize();
                double dot = targetLook.dot(toWarden);

                // If behind the target, mark as ambushing
                this.entityData.set(IS_AMBUSHING, dot < AMBUSH_ANGLE_THRESHOLD);
            } else {
                this.entityData.set(IS_AMBUSHING, false);
            }
        }
    }

    /**
     * Attempts to extinguish nearby beacon/torch light sources.
     */
    private void attemptExtinguishBeacons() {
        BlockPos center = this.blockPosition();

        for (int x = -BEACON_EXTINGUISH_RANGE; x <= BEACON_EXTINGUISH_RANGE; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -BEACON_EXTINGUISH_RANGE; z <= BEACON_EXTINGUISH_RANGE; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (this.level().getBlockState(checkPos).is(Blocks.TORCH) ||
                        this.level().getBlockState(checkPos).is(Blocks.WALL_TORCH) ||
                        this.level().getBlockState(checkPos).is(Blocks.SOUL_TORCH)) {
                        // Extinguish the torch
                        this.level().setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        extinguishCooldown = EXTINGUISH_COOLDOWN;

                        this.level().playSound(null, checkPos,
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0f, 0.5f);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SMOKE,
                                checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                                5, 0.2, 0.2, 0.2, 0.01);
                        }
                        return; // One torch per attempt
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Ambush attack deals double damage
        if (isAmbushing() && !isVisible()) {
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * AMBUSH_DAMAGE_MULTIPLIER;
            if (target instanceof LivingEntity living) {
                living.hurt(this.damageSources().mobAttack(this), damage);
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5f, 0.8f);
                return true;
            }
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean isInvisible() {
        if (!isVisible()) {
            return true;
        }
        return super.isInvisible();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (!isVisible()) {
                return state.setAndContinue(STEALTH);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                if (isAmbushing()) {
                    return state.setAndContinue(AMBUSH_ATTACK);
                }
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
