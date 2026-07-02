package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.core.registry.CDESounds;
import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * Living Vertebra - Worldbearer dungeon miniboss.
 *
 * A multi-segment stone serpent composed of linked vertebra segments. Each segment
 * has its own HP pool. Destroying segments shortens the serpent and reduces its reach.
 * The core vertebra (located in the center of the body) is the actual kill point.
 * Sweeps the room with its body, dealing damage to everything in its path.
 *
 * Stats: 260 HP total (distributed across segments), 7 armor, 13 damage.
 * Phases: FULL (all segments) -> DAMAGED (half segments) -> EXPOSED (core vulnerable)
 * Mechanic: Destroy outer segments to expose and reach the core vertebra.
 */
public class LivingVertebraEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phases based on remaining segment count.
     */
    public enum BossPhase {
        FULL,       // All segments intact - sweeps entire room
        DAMAGED,    // Half segments destroyed - faster, shorter sweeps
        EXPOSED,    // Core vertebra exposed - vulnerable but desperate attacks
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(LivingVertebraEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> SEGMENT_COUNT =
        SynchedEntityData.defineId(LivingVertebraEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_SWEEPING =
        SynchedEntityData.defineId(LivingVertebraEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_SEGMENTS = 8;
    private static final int DAMAGED_THRESHOLD = 4; // Phase changes at half segments
    private static final int EXPOSED_THRESHOLD = 2; // Core exposed with 2 or fewer segments
    private static final float SEGMENT_HP = 30.0f; // Each segment has 30 HP
    private static final float SWEEP_DAMAGE = 13.0f;
    private static final double SWEEP_RADIUS = 6.0;
    private static final int SWEEP_DURATION = 40; // 2 seconds per sweep
    private static final int SWEEP_COOLDOWN = 80; // 4 seconds between sweeps

    private final float[] segmentHealth;
    private int sweepTimer = 0;
    private int sweepCooldown = 0;
    private float sweepAngle = 0.0f;

    private final BossPhaseManager<LivingVertebraEntity> phaseManager;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.living_vertebra.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.living_vertebra.move");
    private static final RawAnimation SWEEP = RawAnimation.begin().thenPlay("animation.living_vertebra.sweep");
    private static final RawAnimation COIL = RawAnimation.begin().thenPlay("animation.living_vertebra.coil");
    private static final RawAnimation EXPOSED_IDLE = RawAnimation.begin().thenLoop("animation.living_vertebra.exposed_idle");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.living_vertebra.death");

    public LivingVertebraEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.segmentHealth = new float[MAX_SEGMENTS];
        for (int i = 0; i < MAX_SEGMENTS; i++) {
            this.segmentHealth[i] = SEGMENT_HP;
        }

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(BossPhase.DAMAGED.ordinal(), 0.60f)
            .addThreshold(BossPhase.EXPOSED.ordinal(), 0.25f);
    }

    /**
     * Creates the attribute supplier for Living Vertebra.
     * 260 HP, 0.18 speed, 13 damage, 7 armor, 0.8 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 260.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 13.0)
            .add(Attributes.ARMOR, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, BossPhase.FULL.ordinal());
        builder.define(SEGMENT_COUNT, MAX_SEGMENTS);
        builder.define(IS_SWEEPING, false);
    }

    public BossPhase getBossPhase() {
        return BossPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setBossPhase(BossPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public int getSegmentCount() {
        return this.entityData.get(SEGMENT_COUNT);
    }

    private void setSegmentCount(int count) {
        this.entityData.set(SEGMENT_COUNT, count);
    }

    public boolean isSweeping() {
        return this.entityData.get(IS_SWEEPING);
    }

    private void setSweeping(boolean sweeping) {
        this.entityData.set(IS_SWEEPING, sweeping);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            phaseManager.tick();

            // Handle sweep attack
            if (isSweeping()) {
                sweepTimer--;
                performSweepTick();
                if (sweepTimer <= 0) {
                    setSweeping(false);
                    sweepCooldown = SWEEP_COOLDOWN;
                }
            } else {
                sweepCooldown--;
                if (sweepCooldown <= 0 && this.getTarget() != null) {
                    startSweep();
                }
            }

            // Update segment count and phase
            int alive = countAliveSegments();
            setSegmentCount(alive);
            updatePhaseFromSegments(alive);
        }
    }

    /**
     * Starts a room-sweeping attack where the serpent body rotates through the area.
     */
    private void startSweep() {
        setSweeping(true);
        sweepTimer = SWEEP_DURATION;
        sweepAngle = 0.0f;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /**
     * Performs one tick of the sweep attack, rotating the damage arc.
     */
    private void performSweepTick() {
        sweepAngle += (float)(Math.PI * 2.0 / SWEEP_DURATION);

        // Calculate sweep damage arc based on remaining segments (shorter serpent = shorter reach)
        double reach = SWEEP_RADIUS * ((double) getSegmentCount() / MAX_SEGMENTS);
        reach = Math.max(2.0, reach);

        AABB sweepBox = this.getBoundingBox().inflate(reach);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, sweepBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            double dist = this.distanceTo(target);
            if (dist <= reach) {
                // Check if target is in the current sweep arc
                Vec3 toTarget = target.position().subtract(this.position()).normalize();
                double angle = Math.atan2(toTarget.z, toTarget.x);
                double sweepAngleNorm = sweepAngle % (Math.PI * 2);
                double angleDiff = Math.abs(angle - sweepAngleNorm);

                if (angleDiff < 0.5 || angleDiff > Math.PI * 2 - 0.5) {
                    target.hurt(source, SWEEP_DAMAGE);
                    Vec3 knockDir = toTarget.scale(2.0);
                    target.knockback(2.0f, -knockDir.x, -knockDir.z);
                    target.hurtMarked = true;
                }
            }
        }
    }

    /**
     * Counts how many segments are still alive.
     */
    private int countAliveSegments() {
        int count = 0;
        for (float hp : segmentHealth) {
            if (hp > 0) count++;
        }
        return count;
    }

    /**
     * Updates the boss phase based on remaining segments.
     */
    private void updatePhaseFromSegments(int aliveSegments) {
        BossPhase current = getBossPhase();
        if (aliveSegments <= EXPOSED_THRESHOLD && current != BossPhase.EXPOSED && current != BossPhase.DEATH) {
            transitionToPhase(BossPhase.EXPOSED.ordinal());
        } else if (aliveSegments <= DAMAGED_THRESHOLD && current == BossPhase.FULL) {
            transitionToPhase(BossPhase.DAMAGED.ordinal());
        }
    }

    /**
     * Damage is distributed to individual segments. Only damage to the core kills the entity.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide()) {
            // Distribute damage to a random alive outer segment first
            // Unless in EXPOSED phase, then damage goes to core (actual HP)
            if (getBossPhase() != BossPhase.EXPOSED) {
                int targetSegment = findRandomAliveSegment();
                if (targetSegment >= 0) {
                    segmentHealth[targetSegment] -= amount;
                    if (segmentHealth[targetSegment] <= 0) {
                        segmentHealth[targetSegment] = 0;
                        onSegmentDestroyed(targetSegment);
                    }
                    // Play hit sound but don't damage main HP pool
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);
                    return true; // Absorbed by segment
                }
            }
        }
        // Core damage - passes through to real HP
        return super.hurt(source, amount);
    }

    private int findRandomAliveSegment() {
        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < MAX_SEGMENTS; i++) {
            if (segmentHealth[i] > 0) {
                alive.add(i);
            }
        }
        if (alive.isEmpty()) return -1;
        return alive.get(this.random.nextInt(alive.size()));
    }

    private void onSegmentDestroyed(int segment) {
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getBossPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        BossPhase newPhase = BossPhase.values()[phase];
        BossPhase oldPhase = getBossPhase();
        if (newPhase == oldPhase) return;

        setBossPhase(newPhase);

        this.level().playSound(null, this.blockPosition(),
            CDESounds.BOSS_PHASE_CHANGE.get(), SoundSource.HOSTILE, 2.0f, 1.0f);

        if (this.level() instanceof ServerLevel serverLevel) {
            CDENetworking.BossPhasePayload payload =
                new CDENetworking.BossPhasePayload(this.getId(), phase);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceTo(this) < 64.0) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }

    @Override
    public float getPhaseHealthThreshold(int phase) {
        return switch (BossPhase.values()[phase]) {
            case DAMAGED -> 0.60f;
            case EXPOSED -> 0.25f;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (BossPhase.values()[phase]) {
            case FULL -> List.of("sweep", "coil", "slam");
            case DAMAGED -> List.of("sweep", "rapid_strike", "slam");
            case EXPOSED -> List.of("desperate_lunge", "rapid_strike");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getBossPhase() != BossPhase.DEATH) {
            transitionToPhase(BossPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 60 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 10, state -> {
            BossPhase phase = getBossPhase();
            if (phase == BossPhase.DEATH) {
                return state.setAndContinue(DEATH_ANIM);
            }
            if (isSweeping()) {
                return state.setAndContinue(SWEEP);
            }
            if (phase == BossPhase.EXPOSED) {
                return state.setAndContinue(EXPOSED_IDLE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging && !isSweeping()) {
                return state.setAndContinue(COIL);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
