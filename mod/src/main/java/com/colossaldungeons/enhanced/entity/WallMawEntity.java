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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Wall Maw - Hollow Leviathan dungeon creature.
 *
 * A hidden mouth embedded in the Leviathan's walls that bites players when they pass by.
 * Can grab and swallow players, teleporting them to another area of the dungeon.
 * Using flint and steel to cauterize the maw permanently kills it (cannot regenerate).
 *
 * Stats: 36 HP, 4 armor, 8 damage + grab mechanic.
 * Behavior: Hidden -> Bite -> (chance to) Swallow -> Teleport player -> Dormant.
 * Counter: Flint and steel cauterizes permanently.
 */
public class WallMawEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Maw behavioral states.
     */
    public enum MawState {
        HIDDEN,         // Invisible, waiting for prey
        OPENING,        // Briefly visible before bite
        BITING,         // Active bite attack
        SWALLOWING,     // Grabbed a player, preparing teleport
        CAUTERIZED,     // Permanently disabled
        COOLDOWN        // Post-attack rest period
    }

    private static final EntityDataAccessor<Integer> MAW_STATE =
        SynchedEntityData.defineId(WallMawEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_CAUTERIZED =
        SynchedEntityData.defineId(WallMawEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double BITE_TRIGGER_RANGE = 1.5;
    private static final float BITE_DAMAGE = 8.0f;
    private static final float GRAB_CHANCE = 0.35f; // 35% chance to grab on bite
    private static final int SWALLOW_DURATION = 40; // 2 seconds to teleport
    private static final int COOLDOWN_DURATION = 100; // 5 seconds between attacks
    private static final int OPENING_DURATION = 10; // 0.5 second warning
    private static final double TELEPORT_RANGE = 30.0; // Max teleport distance

    private int stateTimer = 0;
    private LivingEntity grabbedTarget = null;
    private BlockPos teleportDestination = null;

    private static final RawAnimation HIDDEN_IDLE = RawAnimation.begin().thenLoop("animation.wall_maw.hidden");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("animation.wall_maw.open");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.wall_maw.bite");
    private static final RawAnimation SWALLOW = RawAnimation.begin().thenPlay("animation.wall_maw.swallow");
    private static final RawAnimation CAUTERIZED_ANIM = RawAnimation.begin().thenLoop("animation.wall_maw.cauterized");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wall_maw.idle");

    public WallMawEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Embedded in wall
    }

    /**
     * Creates the attribute supplier for Wall Maw.
     * 36 HP, 0.0 speed (stationary), 8 damage, 4 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 36.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // Cannot be knocked back
            .add(Attributes.FOLLOW_RANGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAW_STATE, MawState.HIDDEN.ordinal());
        builder.define(IS_CAUTERIZED, false);
    }

    public MawState getMawState() {
        return MawState.values()[this.entityData.get(MAW_STATE)];
    }

    private void setMawState(MawState state) {
        this.entityData.set(MAW_STATE, state.ordinal());
    }

    public boolean isCauterized() {
        return this.entityData.get(IS_CAUTERIZED);
    }

    private void setCauterized(boolean cauterized) {
        this.entityData.set(IS_CAUTERIZED, cauterized);
    }

    @Override
    protected void registerGoals() {
        // Minimal goals - mostly tick-driven behavior
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (isCauterized()) {
                setMawState(MawState.CAUTERIZED);
                return;
            }

            MawState state = getMawState();
            stateTimer--;

            switch (state) {
                case HIDDEN -> {
                    // Wait for player to pass within bite range
                    Player nearest = this.level().getNearestPlayer(this, BITE_TRIGGER_RANGE);
                    if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
                        setMawState(MawState.OPENING);
                        stateTimer = OPENING_DURATION;
                    }
                }
                case OPENING -> {
                    if (stateTimer <= 0) {
                        performBite();
                    }
                }
                case BITING -> {
                    if (stateTimer <= 0) {
                        setMawState(MawState.COOLDOWN);
                        stateTimer = COOLDOWN_DURATION;
                    }
                }
                case SWALLOWING -> {
                    if (stateTimer <= 0) {
                        performTeleport();
                        setMawState(MawState.COOLDOWN);
                        stateTimer = COOLDOWN_DURATION;
                    } else if (grabbedTarget != null) {
                        // Pull target toward maw during swallow
                        grabbedTarget.teleportTo(this.getX(), this.getY(), this.getZ());
                    }
                }
                case COOLDOWN -> {
                    if (stateTimer <= 0) {
                        setMawState(MawState.HIDDEN);
                    }
                }
                case CAUTERIZED -> {
                    // Permanently disabled - do nothing
                }
            }
        }
    }

    /**
     * Performs the bite attack on the nearest player.
     */
    private void performBite() {
        Player nearest = this.level().getNearestPlayer(this, BITE_TRIGGER_RANGE + 0.5);
        if (nearest != null && !nearest.isSpectator()) {
            nearest.hurt(this.damageSources().mobAttack(this), BITE_DAMAGE);
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.5f, 0.5f);

            // Chance to grab and swallow
            if (this.random.nextFloat() < GRAB_CHANCE) {
                grabbedTarget = nearest;
                setMawState(MawState.SWALLOWING);
                stateTimer = SWALLOW_DURATION;
                // Find teleport destination
                teleportDestination = findTeleportDestination();
            } else {
                setMawState(MawState.BITING);
                stateTimer = 20;
            }
        } else {
            setMawState(MawState.COOLDOWN);
            stateTimer = COOLDOWN_DURATION / 2;
        }
    }

    /**
     * Teleports the grabbed player to another area.
     */
    private void performTeleport() {
        if (grabbedTarget != null && teleportDestination != null) {
            grabbedTarget.teleportTo(
                teleportDestination.getX() + 0.5,
                teleportDestination.getY(),
                teleportDestination.getZ() + 0.5);

            this.level().playSound(null, teleportDestination,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.8f);
        }
        grabbedTarget = null;
        teleportDestination = null;
    }

    /**
     * Finds a valid teleport destination within the dungeon.
     */
    private BlockPos findTeleportDestination() {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = this.random.nextInt((int)(TELEPORT_RANGE * 2)) - (int)TELEPORT_RANGE;
            int dz = this.random.nextInt((int)(TELEPORT_RANGE * 2)) - (int)TELEPORT_RANGE;
            BlockPos candidate = origin.offset(dx, 0, dz);

            // Find ground level
            for (int dy = 5; dy >= -5; dy--) {
                BlockPos check = candidate.offset(0, dy, 0);
                if (!this.level().getBlockState(check).isAir() &&
                    this.level().getBlockState(check.above()).isAir() &&
                    this.level().getBlockState(check.above(2)).isAir()) {
                    return check.above();
                }
            }
        }
        // Fallback: teleport nearby
        return origin.offset(5, 0, 5);
    }

    /**
     * Cauterize with flint and steel interaction (permanent kill).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isCauterized() && source.getEntity() instanceof Player player) {
            // Check if player is using flint and steel
            if (player.getMainHandItem().is(Items.FLINT_AND_STEEL) ||
                player.getOffhandItem().is(Items.FLINT_AND_STEEL)) {
                cauterize();
                return true;
            }
        }
        return super.hurt(source, amount);
    }

    /**
     * Permanently cauterizes the maw, killing it.
     */
    private void cauterize() {
        setCauterized(true);
        setMawState(MawState.CAUTERIZED);
        grabbedTarget = null;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 0.5f);

        // Kill after short delay for animation
        this.hurt(this.damageSources().onFire(), this.getMaxHealth());
    }

    @Override
    public boolean isNoAi() {
        return true; // Always stationary
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            MawState mawState = getMawState();
            return switch (mawState) {
                case HIDDEN -> state.setAndContinue(HIDDEN_IDLE);
                case OPENING -> state.setAndContinue(OPEN);
                case BITING -> state.setAndContinue(BITE);
                case SWALLOWING -> state.setAndContinue(SWALLOW);
                case CAUTERIZED -> state.setAndContinue(CAUTERIZED_ANIM);
                case COOLDOWN -> state.setAndContinue(IDLE);
            };
        }));
    }
}
