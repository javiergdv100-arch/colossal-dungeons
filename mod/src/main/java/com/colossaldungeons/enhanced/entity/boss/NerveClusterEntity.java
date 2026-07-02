package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.core.registry.CDESounds;
import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.core.BlockPos;
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
 * Nerve Cluster - Hollow Leviathan dungeon miniboss (multipart).
 *
 * A network of nerve ganglia spread across multiple connected nodes. Each ganglion
 * controls different room defenses (spawners, acid sprayers, muscle contractions).
 * Electric discharges pulse through the connections between nodes. Players must
 * cut the connecting branches to disable defenses before attacking the core node.
 *
 * Stats: 320 HP total (distributed), 4 armor, 12 damage (electric discharge).
 * Phases: NETWORKED (all ganglion active) -> SEVERED (branches cut) -> CORE_EXPOSED
 * Mechanic: Destroy branch connections to disable room defenses, then kill core.
 */
public class NerveClusterEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phases based on remaining active branches.
     */
    public enum BossPhase {
        NETWORKED,      // Full network - all defenses active
        PARTIALLY_CUT,  // Some branches severed - some defenses disabled
        CORE_EXPOSED,   // All branches cut - core vulnerable
        DEATH
    }

    /**
     * Represents a branch/ganglion connection.
     */
    public enum BranchType {
        SPAWNER_CONTROL,    // Controls mob spawners in the room
        ACID_CONTROL,       // Controls acid spray mechanisms
        MUSCLE_CONTROL,     // Controls room contraction/crushing walls
        SHIELD_GENERATOR    // Provides damage shield to the core
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE_DATA =
        SynchedEntityData.defineId(NerveClusterEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ACTIVE_BRANCHES =
        SynchedEntityData.defineId(NerveClusterEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_DISCHARGING =
        SynchedEntityData.defineId(NerveClusterEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int TOTAL_BRANCHES = 4;
    private static final float BRANCH_HP = 40.0f;
    private static final float DISCHARGE_DAMAGE = 12.0f;
    private static final double DISCHARGE_RANGE = 6.0;
    private static final int DISCHARGE_INTERVAL = 80; // 4 seconds between discharges
    private static final int DISCHARGE_DURATION = 20; // 1 second discharge active
    private static final float SHIELD_DAMAGE_REDUCTION = 0.5f; // 50% reduction with shield active

    private final float[] branchHealth;
    private final boolean[] branchAlive;
    private int dischargeTimer = 0;
    private int dischargeActiveTimer = 0;

    private final BossPhaseManager<NerveClusterEntity> phaseManager;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nerve_cluster.idle");
    private static final RawAnimation PULSE = RawAnimation.begin().thenLoop("animation.nerve_cluster.pulse");
    private static final RawAnimation DISCHARGE = RawAnimation.begin().thenPlay("animation.nerve_cluster.discharge");
    private static final RawAnimation SEVERED = RawAnimation.begin().thenPlay("animation.nerve_cluster.severed");
    private static final RawAnimation CORE_EXPOSED_ANIM = RawAnimation.begin().thenLoop("animation.nerve_cluster.core_exposed");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.nerve_cluster.death");

    public NerveClusterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Fixed organic structure

        this.branchHealth = new float[TOTAL_BRANCHES];
        this.branchAlive = new boolean[TOTAL_BRANCHES];
        for (int i = 0; i < TOTAL_BRANCHES; i++) {
            this.branchHealth[i] = BRANCH_HP;
            this.branchAlive[i] = true;
        }

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(BossPhase.PARTIALLY_CUT.ordinal(), 0.70f)
            .addThreshold(BossPhase.CORE_EXPOSED.ordinal(), 0.30f);
    }

    /**
     * Creates the attribute supplier for Nerve Cluster.
     * 320 HP, 0.0 speed (stationary), 12 damage (electric), 4 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 320.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE_DATA, BossPhase.NETWORKED.ordinal());
        builder.define(ACTIVE_BRANCHES, TOTAL_BRANCHES);
        builder.define(IS_DISCHARGING, false);
    }

    public BossPhase getBossPhaseEnum() {
        return BossPhase.values()[this.entityData.get(BOSS_PHASE_DATA)];
    }

    private void setBossPhaseEnum(BossPhase phase) {
        this.entityData.set(BOSS_PHASE_DATA, phase.ordinal());
    }

    public int getActiveBranches() {
        return this.entityData.get(ACTIVE_BRANCHES);
    }

    private void setActiveBranches(int count) {
        this.entityData.set(ACTIVE_BRANCHES, count);
    }

    public boolean isDischarging() {
        return this.entityData.get(IS_DISCHARGING);
    }

    private void setDischarging(boolean discharging) {
        this.entityData.set(IS_DISCHARGING, discharging);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            phaseManager.tick();

            BossPhase phase = getBossPhaseEnum();
            if (phase == BossPhase.DEATH) return;

            // Electric discharge cycle
            dischargeTimer--;
            if (dischargeTimer <= 0 && phase != BossPhase.CORE_EXPOSED) {
                startDischarge();
                dischargeTimer = DISCHARGE_INTERVAL;
            }

            // Handle active discharge
            if (isDischarging()) {
                dischargeActiveTimer--;
                performDischarge();
                if (dischargeActiveTimer <= 0) {
                    setDischarging(false);
                }
            }

            // Update branch count and phase
            int alive = countAliveBranches();
            setActiveBranches(alive);
            updatePhaseFromBranches(alive);

            // Activate room defenses based on alive branches
            activateDefenses();
        }
    }

    /**
     * Starts an electric discharge along the network connections.
     */
    private void startDischarge() {
        setDischarging(true);
        dischargeActiveTimer = DISCHARGE_DURATION;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5f, 1.5f);
    }

    /**
     * Performs the electric discharge, damaging entities near active branches.
     */
    private void performDischarge() {
        AABB dischargeBox = this.getBoundingBox().inflate(DISCHARGE_RANGE);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, dischargeBox, e -> e != this && !e.isSpectator());

        // Damage scales with number of active branches
        float damageMultiplier = (float) getActiveBranches() / TOTAL_BRANCHES;
        float damage = DISCHARGE_DAMAGE * damageMultiplier;

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            if (this.distanceTo(target) <= DISCHARGE_RANGE) {
                target.hurt(source, damage);
                // Electric stun effect - brief slowdown
                target.setDeltaMovement(target.getDeltaMovement().scale(0.3));
            }
        }
    }

    /**
     * Activates room defenses based on which branches are still alive.
     */
    private void activateDefenses() {
        // Each branch controls a different defense mechanism
        // When the branch is severed, that defense is permanently disabled
        // This is handled by the dungeon system checking getActiveBranches()
    }

    /**
     * Counts remaining alive branches.
     */
    private int countAliveBranches() {
        int count = 0;
        for (boolean alive : branchAlive) {
            if (alive) count++;
        }
        return count;
    }

    /**
     * Updates the boss phase based on remaining active branches.
     */
    private void updatePhaseFromBranches(int aliveBranches) {
        BossPhase current = getBossPhaseEnum();
        if (aliveBranches == 0 && current != BossPhase.CORE_EXPOSED && current != BossPhase.DEATH) {
            transitionToPhase(BossPhase.CORE_EXPOSED.ordinal());
        } else if (aliveBranches <= 2 && current == BossPhase.NETWORKED) {
            transitionToPhase(BossPhase.PARTIALLY_CUT.ordinal());
        }
    }

    /**
     * Damage is distributed to branches first, then to core when exposed.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide()) {
            BossPhase phase = getBossPhaseEnum();

            // Shield generator branch reduces core damage
            if (branchAlive[BranchType.SHIELD_GENERATOR.ordinal()] && phase != BossPhase.CORE_EXPOSED) {
                amount *= SHIELD_DAMAGE_REDUCTION;
            }

            // Try to damage a branch first
            if (phase != BossPhase.CORE_EXPOSED) {
                int targetBranch = findRandomAliveBranch();
                if (targetBranch >= 0) {
                    branchHealth[targetBranch] -= amount;
                    if (branchHealth[targetBranch] <= 0) {
                        branchHealth[targetBranch] = 0;
                        branchAlive[targetBranch] = false;
                        onBranchSevered(targetBranch);
                    }
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 1.0f, 0.8f);
                    return true; // Damage absorbed by branch
                }
            }
        }
        // Core damage
        return super.hurt(source, amount);
    }

    private int findRandomAliveBranch() {
        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < TOTAL_BRANCHES; i++) {
            if (branchAlive[i]) {
                alive.add(i);
            }
        }
        if (alive.isEmpty()) return -1;
        return alive.get(this.random.nextInt(alive.size()));
    }

    private void onBranchSevered(int branch) {
        BranchType type = BranchType.values()[branch];
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 2.0f, 0.5f);

        // Notify the dungeon system that a defense has been disabled
        // Actual effect handled by dungeon state machine at runtime
    }

    @Override
    public boolean isNoAi() {
        return true; // Stationary
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getBossPhaseEnum().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        BossPhase newPhase = BossPhase.values()[phase];
        BossPhase oldPhase = getBossPhaseEnum();
        if (newPhase == oldPhase) return;

        setBossPhaseEnum(newPhase);

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
            case PARTIALLY_CUT -> 0.70f;
            case CORE_EXPOSED -> 0.30f;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (BossPhase.values()[phase]) {
            case NETWORKED -> List.of("discharge", "spawn_defense", "acid_spray", "room_crush");
            case PARTIALLY_CUT -> List.of("discharge", "reduced_defenses");
            case CORE_EXPOSED -> List.of("desperate_discharge");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getBossPhaseEnum() != BossPhase.DEATH) {
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
            BossPhase phase = getBossPhaseEnum();
            if (phase == BossPhase.DEATH) {
                return state.setAndContinue(DEATH_ANIM);
            }
            if (isDischarging()) {
                return state.setAndContinue(DISCHARGE);
            }
            if (phase == BossPhase.CORE_EXPOSED) {
                return state.setAndContinue(CORE_EXPOSED_ANIM);
            }
            return state.setAndContinue(PULSE);
        }));
    }
}
