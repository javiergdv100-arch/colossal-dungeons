package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Antibody - Hollow Leviathan dungeon creature.
 *
 * A white amorphous blob that is part of the Leviathan's immune system. Spawns in waves
 * proportional to the noise and damage players cause in the area. Uses swarm AI to
 * overwhelm targets with numbers. Players holding or having consumed honey bottles
 * become invisible to antibodies (the honey coats their scent).
 *
 * Stats: 12 HP, 5 damage.
 * Behavior: Swarm targeting, noise-reactive spawning, honey bottle invisibility.
 */
public class AntibodyEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_AGGRO =
        SynchedEntityData.defineId(AntibodyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double SWARM_RADIUS = 3.0; // Stays near other antibodies
    private static final double DETECTION_RANGE = 16.0;
    private static final int AGGRO_DURATION = 200; // 10 seconds of aggro before calming

    private int aggroTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.antibody.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.antibody.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.antibody.attack");
    private static final RawAnimation SWARM = RawAnimation.begin().thenLoop("animation.antibody.swarm");

    public AntibodyEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Antibody.
     * 12 HP, 0.28 speed, 5 damage, 0 armor, 0 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_AGGRO, false);
    }

    public boolean isAggro() {
        return this.entityData.get(IS_AGGRO);
    }

    private void setAggro(boolean aggro) {
        this.entityData.set(IS_AGGRO, aggro);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                // Honey bottle makes player invisible to antibodies
                LivingEntity target = this.mob.getTarget();
                if (target instanceof Player player) {
                    return !isPlayerHoneyCoated(player);
                }
                return true;
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Aggro timer management
            if (isAggro()) {
                aggroTimer--;
                if (aggroTimer <= 0) {
                    setAggro(false);
                    this.setTarget(null);
                }
            }

            // Swarm behavior - move toward other antibodies
            if (this.getTarget() != null) {
                moveTowardSwarm();
            }

            // Check if current target became honey-coated
            if (this.getTarget() instanceof Player player && isPlayerHoneyCoated(player)) {
                this.setTarget(null);
                setAggro(false);
            }
        }
    }

    /**
     * Checks if a player is coated in honey (holding honey bottle in either hand).
     * Honey-coated players are invisible to antibodies.
     */
    private static boolean isPlayerHoneyCoated(Player player) {
        return player.getMainHandItem().is(Items.HONEY_BOTTLE) ||
               player.getOffhandItem().is(Items.HONEY_BOTTLE);
    }

    /**
     * Swarm behavior - antibodies cluster together when pursuing a target.
     */
    private void moveTowardSwarm() {
        AABB swarmBox = this.getBoundingBox().inflate(SWARM_RADIUS * 2);
        List<AntibodyEntity> neighbors = this.level().getEntitiesOfClass(
            AntibodyEntity.class, swarmBox, e -> e != this);

        if (!neighbors.isEmpty()) {
            // Calculate center of nearby swarm
            double avgX = 0, avgZ = 0;
            for (AntibodyEntity neighbor : neighbors) {
                avgX += neighbor.getX();
                avgZ += neighbor.getZ();
            }
            avgX /= neighbors.size();
            avgZ /= neighbors.size();

            // Gently steer toward swarm center while pursuing target
            double dx = avgX - this.getX();
            double dz = avgZ - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > SWARM_RADIUS) {
                this.push(dx * 0.01, 0, dz * 0.01);
            }
        }
    }

    /**
     * Activates aggro in response to noise/damage events in the area.
     * Called by the dungeon event system when players cause disturbances.
     */
    public void onNoiseDetected(Player source) {
        if (!isPlayerHoneyCoated(source)) {
            setAggro(true);
            aggroTimer = AGGRO_DURATION;
            this.setTarget(source);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (this.getTarget() != null && isAggro()) {
                if (state.isMoving()) {
                    return state.setAndContinue(SWARM);
                }
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
