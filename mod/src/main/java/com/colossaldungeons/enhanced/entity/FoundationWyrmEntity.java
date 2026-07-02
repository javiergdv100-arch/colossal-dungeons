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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Foundation Wyrm - Worldbearer dungeon elite creature.
 *
 * A massive rock worm that burrows through the dungeon foundation, emerging to ambush
 * players. When it burrows, it creates holes that players can use as alternative paths.
 * After emerging, it is weakened for several seconds before it can attack at full strength.
 *
 * Stats: 120 HP, 5 armor, 11 damage.
 * Traits: Burrow/emerge cycle, creates usable holes, post-emergence vulnerability.
 * Weakness: Takes 50% more damage for 4 seconds after emerging.
 */
public class FoundationWyrmEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Behavior states for the burrow/ambush cycle.
     */
    public enum WyrmState {
        BURROWED,
        EMERGING,
        SURFACE_WEAK,
        SURFACE_ACTIVE,
        BURROWING
    }

    private static final EntityDataAccessor<Integer> WYRM_STATE =
        SynchedEntityData.defineId(FoundationWyrmEntity.class, EntityDataSerializers.INT);

    private static final int EMERGE_DURATION = 30; // 1.5 seconds to emerge
    private static final int WEAKNESS_DURATION = 80; // 4 seconds of post-emerge weakness
    private static final int SURFACE_ACTIVE_DURATION = 200; // 10 seconds active before re-burrowing
    private static final int BURROW_DURATION = 20; // 1 second to burrow
    private static final int BURROWED_DURATION_MIN = 60; // Min 3 seconds underground
    private static final int BURROWED_DURATION_MAX = 140; // Max 7 seconds underground
    private static final double DETECTION_RANGE = 12.0;
    private static final float EMERGENCE_DAMAGE = 6.0f;
    private static final double EMERGENCE_RADIUS = 2.5;

    private int stateTimer = 0;
    private int burrowDuration = 0;
    private BlockPos lastBurrowPos = null;

    private static final RawAnimation BURROWED_IDLE = RawAnimation.begin().thenLoop("animation.foundation_wyrm.burrowed");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("animation.foundation_wyrm.emerge");
    private static final RawAnimation IDLE_WEAK = RawAnimation.begin().thenLoop("animation.foundation_wyrm.idle_weak");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.foundation_wyrm.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.foundation_wyrm.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.foundation_wyrm.attack");
    private static final RawAnimation BURROW = RawAnimation.begin().thenPlay("animation.foundation_wyrm.burrow");

    public FoundationWyrmEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Foundation Wyrm.
     * 120 HP, 0.22 speed, 11 damage, 5 armor, 0.6 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WYRM_STATE, WyrmState.BURROWED.ordinal());
    }

    public WyrmState getWyrmState() {
        return WyrmState.values()[this.entityData.get(WYRM_STATE)];
    }

    private void setWyrmState(WyrmState state) {
        this.entityData.set(WYRM_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            WyrmState state = getWyrmState();
            stateTimer--;

            switch (state) {
                case BURROWED -> {
                    // Wait underground, detect players approaching
                    if (stateTimer <= 0) {
                        Player nearest = this.level().getNearestPlayer(this, DETECTION_RANGE);
                        if (nearest != null) {
                            // Teleport near the player before emerging
                            Vec3 targetPos = nearest.position().add(
                                (this.random.nextDouble() - 0.5) * 4.0,
                                0,
                                (this.random.nextDouble() - 0.5) * 4.0);
                            this.teleportTo(targetPos.x, targetPos.y, targetPos.z);
                            startEmerging();
                        }
                    }
                }
                case EMERGING -> {
                    if (stateTimer <= 0) {
                        // Deal emergence damage to nearby entities
                        performEmergenceAttack();
                        // Create hole at last burrow position
                        createBurrowHole();
                        setWyrmState(WyrmState.SURFACE_WEAK);
                        stateTimer = WEAKNESS_DURATION;
                    }
                }
                case SURFACE_WEAK -> {
                    if (stateTimer <= 0) {
                        setWyrmState(WyrmState.SURFACE_ACTIVE);
                        stateTimer = SURFACE_ACTIVE_DURATION;
                    }
                }
                case SURFACE_ACTIVE -> {
                    if (stateTimer <= 0 || this.getTarget() == null) {
                        startBurrowing();
                    }
                }
                case BURROWING -> {
                    if (stateTimer <= 0) {
                        lastBurrowPos = this.blockPosition();
                        setWyrmState(WyrmState.BURROWED);
                        burrowDuration = BURROWED_DURATION_MIN +
                            this.random.nextInt(BURROWED_DURATION_MAX - BURROWED_DURATION_MIN);
                        stateTimer = burrowDuration;
                        // Make invisible/invulnerable while burrowed
                        this.setInvisible(true);
                    }
                }
            }
        }
    }

    private void startEmerging() {
        setWyrmState(WyrmState.EMERGING);
        stateTimer = EMERGE_DURATION;
        this.setInvisible(false);
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    private void startBurrowing() {
        setWyrmState(WyrmState.BURROWING);
        stateTimer = BURROW_DURATION;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    /**
     * Deals damage to entities near the emergence point.
     */
    private void performEmergenceAttack() {
        AABB emergeBox = this.getBoundingBox().inflate(EMERGENCE_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, emergeBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            target.hurt(source, EMERGENCE_DAMAGE);
            Vec3 knockDir = target.position().subtract(this.position()).normalize();
            target.knockback(1.5f, -knockDir.x, -knockDir.z);
        }
    }

    /**
     * Creates a passable hole at the burrow position that players can use as a path.
     */
    private void createBurrowHole() {
        if (lastBurrowPos != null && this.level() instanceof ServerLevel serverLevel) {
            // Replace blocks around the burrow point with air to create a passage
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos holePos = lastBurrowPos.offset(dx, dy, dz);
                        if (serverLevel.getBlockState(holePos).is(Blocks.STONE) ||
                            serverLevel.getBlockState(holePos).is(Blocks.DEEPSLATE)) {
                            serverLevel.setBlock(holePos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Takes extra damage while in SURFACE_WEAK state (post-emergence vulnerability).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getWyrmState() == WyrmState.SURFACE_WEAK) {
            amount *= 1.5f; // 50% more damage during weakness
        }
        // Invulnerable while burrowed
        if (getWyrmState() == WyrmState.BURROWED) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isNoAi() {
        WyrmState state = getWyrmState();
        if (state == WyrmState.BURROWED || state == WyrmState.EMERGING || state == WyrmState.BURROWING) {
            return true;
        }
        return super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            WyrmState wyrmState = getWyrmState();
            return switch (wyrmState) {
                case BURROWED -> state.setAndContinue(BURROWED_IDLE);
                case EMERGING -> state.setAndContinue(EMERGE);
                case SURFACE_WEAK -> state.setAndContinue(IDLE_WEAK);
                case SURFACE_ACTIVE -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
                case BURROWING -> state.setAndContinue(BURROW);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            WyrmState wyrmState = getWyrmState();
            if (wyrmState == WyrmState.SURFACE_ACTIVE && this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
