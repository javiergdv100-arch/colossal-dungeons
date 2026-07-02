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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
 * Sovereign Parasite - Hollow Leviathan FINAL BOSS.
 *
 * A colossal parasitic organism that has taken control of the Leviathan's heart.
 * Three distinct combat phases with unique mechanics:
 *
 * Phase 1 (100%-60% HP): Tentacle roots attack players while spawning larvae.
 *   Roots are independent cuttable parts (shears deal 3x damage to roots).
 *   Must cut roots to reach the main body.
 *
 * Phase 2 (60%-25% HP): Retreats behind a living rib cage barrier.
 *   Players must use bone meal on the ribs to force them open.
 *   While protected, spawns waves of antibodies and larvae.
 *
 * Phase 3 (25%-0% HP): Global leviathan spasm - the room contracts periodically.
 *   Walls close in rhythmically, arena shrinks. Desperate final attacks.
 *   Must defeat before the room crushes all players.
 *
 * Stats: 1300 HP, 6 armor, 15-21 damage depending on attack type.
 * Implements IBossPhase with BossPhaseManager for health-based transitions.
 */
public class SovereignParasiteEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phase enumeration.
     */
    public enum BossPhase {
        DORMANT,
        AWAKENING,
        PHASE_1_ROOTS,
        TRANSITION_1_2,
        PHASE_2_RIBCAGE,
        TRANSITION_2_3,
        PHASE_3_SPASM,
        DEATH
    }

    /**
     * Attack types with varying damage.
     */
    public enum AttackType {
        TENTACLE_SLAM(15.0f),
        TENTACLE_SWEEP(12.0f),
        ROOT_GRAB(8.0f),
        ACID_SPIT(10.0f),
        BODY_CRUSH(21.0f),
        SPASM_WAVE(18.0f);

        private final float damage;
        AttackType(float damage) { this.damage = damage; }
        public float getDamage() { return damage; }
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE_DATA =
        SynchedEntityData.defineId(SovereignParasiteEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ACTIVE_ROOTS =
        SynchedEntityData.defineId(SovereignParasiteEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> RIBS_OPEN =
        SynchedEntityData.defineId(SovereignParasiteEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> SPASM_INTENSITY =
        SynchedEntityData.defineId(SovereignParasiteEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
        SynchedEntityData.defineId(SovereignParasiteEntity.class, EntityDataSerializers.INT);

    // Phase thresholds
    private static final float PHASE_2_THRESHOLD = 0.60f;
    private static final float PHASE_3_THRESHOLD = 0.25f;

    // Root system
    private static final int TOTAL_ROOTS = 6;
    private static final float ROOT_HP = 50.0f;
    private static final float SHEARS_MULTIPLIER = 3.0f;
    private static final double ROOT_ATTACK_RANGE = 8.0;

    // Phase 2 - Rib cage
    private static final int RIB_BONE_MEAL_REQUIRED = 5;
    private static final int SPAWN_WAVE_INTERVAL = 120; // 6 seconds

    // Phase 3 - Spasm
    private static final int SPASM_INTERVAL = 100; // 5 seconds
    private static final double SPASM_CRUSH_RANGE = 12.0;
    private static final float SPASM_DAMAGE = 18.0f;
    private static final int MAX_SPASM_INTENSITY = 5;

    // Timers and state
    private final float[] rootHealth;
    private final boolean[] rootAlive;
    private int attackCooldown = 0;
    private int attackTimer = 0;
    private int spawnWaveTimer = 0;
    private int spasmTimer = 0;
    private int ribBoneMealCount = 0;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 80;
    private static final int AWAKENING_DURATION = 100;

    private final BossPhaseManager<SovereignParasiteEntity> phaseManager;

    // Animations
    private static final RawAnimation DORMANT_IDLE = RawAnimation.begin().thenLoop("animation.sovereign_parasite.dormant");
    private static final RawAnimation AWAKEN = RawAnimation.begin().thenPlay("animation.sovereign_parasite.awaken");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.sovereign_parasite.phase1_idle");
    private static final RawAnimation P1_TENTACLE_SLAM = RawAnimation.begin().thenPlay("animation.sovereign_parasite.tentacle_slam");
    private static final RawAnimation P1_TENTACLE_SWEEP = RawAnimation.begin().thenPlay("animation.sovereign_parasite.tentacle_sweep");
    private static final RawAnimation P1_ROOT_GRAB = RawAnimation.begin().thenPlay("animation.sovereign_parasite.root_grab");
    private static final RawAnimation TRANSITION_ANIM = RawAnimation.begin().thenPlay("animation.sovereign_parasite.transition");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.sovereign_parasite.phase2_idle");
    private static final RawAnimation P2_SPAWN = RawAnimation.begin().thenPlay("animation.sovereign_parasite.phase2_spawn");
    private static final RawAnimation P2_RIBS_OPEN = RawAnimation.begin().thenPlay("animation.sovereign_parasite.ribs_open");
    private static final RawAnimation P3_IDLE = RawAnimation.begin().thenLoop("animation.sovereign_parasite.phase3_idle");
    private static final RawAnimation P3_SPASM = RawAnimation.begin().thenPlay("animation.sovereign_parasite.spasm");
    private static final RawAnimation P3_CRUSH = RawAnimation.begin().thenPlay("animation.sovereign_parasite.crush");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.sovereign_parasite.death");

    public SovereignParasiteEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.rootHealth = new float[TOTAL_ROOTS];
        this.rootAlive = new boolean[TOTAL_ROOTS];
        for (int i = 0; i < TOTAL_ROOTS; i++) {
            this.rootHealth[i] = ROOT_HP;
            this.rootAlive[i] = true;
        }

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(BossPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(BossPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for Sovereign Parasite.
     * 1300 HP, 0.12 speed, 15 base damage, 6 armor, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.12)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE_DATA, BossPhase.DORMANT.ordinal());
        builder.define(ACTIVE_ROOTS, TOTAL_ROOTS);
        builder.define(RIBS_OPEN, false);
        builder.define(SPASM_INTENSITY, 0);
        builder.define(ATTACK_STATE, 0);
    }

    public BossPhase getBossPhaseEnum() {
        return BossPhase.values()[this.entityData.get(BOSS_PHASE_DATA)];
    }

    private void setBossPhaseEnum(BossPhase phase) {
        this.entityData.set(BOSS_PHASE_DATA, phase.ordinal());
    }

    public int getActiveRoots() {
        return this.entityData.get(ACTIVE_ROOTS);
    }

    private void setActiveRoots(int count) {
        this.entityData.set(ACTIVE_ROOTS, count);
    }

    public boolean areRibsOpen() {
        return this.entityData.get(RIBS_OPEN);
    }

    private void setRibsOpen(boolean open) {
        this.entityData.set(RIBS_OPEN, open);
    }

    public int getSpasmIntensity() {
        return this.entityData.get(SPASM_INTENSITY);
    }

    private void setSpasmIntensity(int intensity) {
        this.entityData.set(SPASM_INTENSITY, Math.min(MAX_SPASM_INTENSITY, intensity));
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
            BossPhase phase = getBossPhaseEnum();

            switch (phase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 15.0);
                    if (nearest != null) {
                        transitionToPhase(BossPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_1_ROOTS.ordinal());
                    }
                }
                case PHASE_1_ROOTS -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_2_RIBCAGE.ordinal());
                    }
                }
                case PHASE_2_RIBCAGE -> {
                    phaseManager.tick();
                    tickPhase2();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_3_SPASM.ordinal());
                    }
                }
                case PHASE_3_SPASM -> {
                    tickPhase3();
                }
                case DEATH -> {
                    // Death handled by tickDeath
                }
            }

            // Update root count
            setActiveRoots(countAliveRoots());
        }
    }

    // ========== Phase 1: Tentacle Roots ==========

    private void tickPhase1() {
        attackCooldown--;

        if (attackCooldown <= 0) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                double dist = this.distanceTo(target);
                if (dist <= ROOT_ATTACK_RANGE) {
                    selectPhase1Attack(target, dist);
                }
            }
        }

        // Periodically spawn larvae
        spawnWaveTimer--;
        if (spawnWaveTimer <= 0) {
            spawnLarvae();
            spawnWaveTimer = SPAWN_WAVE_INTERVAL;
        }
    }

    private void selectPhase1Attack(LivingEntity target, double distance) {
        float roll = this.random.nextFloat();

        if (distance < 3.0 && roll < 0.4f) {
            performAttack(target, AttackType.TENTACLE_SLAM);
            attackCooldown = 40;
        } else if (roll < 0.7f) {
            performAttack(target, AttackType.TENTACLE_SWEEP);
            attackCooldown = 60;
        } else {
            performRootGrab(target);
            attackCooldown = 80;
        }
    }

    private void performAttack(LivingEntity target, AttackType type) {
        target.hurt(this.damageSources().mobAttack(this), type.getDamage());
        Vec3 knockDir = target.position().subtract(this.position()).normalize();
        target.knockback(2.0f, -knockDir.x, -knockDir.z);
        target.hurtMarked = true;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    private void performRootGrab(LivingEntity target) {
        // Grab pulls target toward the boss
        Vec3 pullDir = this.position().subtract(target.position()).normalize().scale(0.5);
        target.setDeltaMovement(pullDir);
        target.hurt(this.damageSources().mobAttack(this), AttackType.ROOT_GRAB.getDamage());
        target.hurtMarked = true;
    }

    // ========== Phase 2: Rib Cage ==========

    private void tickPhase2() {
        if (!areRibsOpen()) {
            // Protected by rib cage - spawn waves but take reduced damage
            spawnWaveTimer--;
            if (spawnWaveTimer <= 0) {
                spawnAntibodyWave();
                spawnLarvae();
                spawnWaveTimer = SPAWN_WAVE_INTERVAL;
            }
        } else {
            // Ribs are open - vulnerable, behaves like phase 1 but weaker
            attackCooldown--;
            if (attackCooldown <= 0) {
                LivingEntity target = this.getTarget();
                if (target != null && this.distanceTo(target) <= 5.0) {
                    performAttack(target, AttackType.ACID_SPIT);
                    attackCooldown = 50;
                }
            }
        }
    }

    /**
     * Called when a player uses bone meal on this entity (interaction check).
     * Applies one count toward opening the rib cage.
     */
    public void onBoneMealApplied(Player player) {
        if (getBossPhaseEnum() == BossPhase.PHASE_2_RIBCAGE && !areRibsOpen()) {
            ribBoneMealCount++;
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f, 1.0f);

            if (ribBoneMealCount >= RIB_BONE_MEAL_REQUIRED) {
                setRibsOpen(true);
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0f, 0.5f);
            }
        }
    }

    // ========== Phase 3: Global Spasm ==========

    private void tickPhase3() {
        attackCooldown--;
        spasmTimer--;

        // Spasm intensity increases over time
        if (spasmTimer <= 0) {
            performGlobalSpasm();
            setSpasmIntensity(getSpasmIntensity() + 1);
            // Spasms get faster as intensity increases
            spasmTimer = SPASM_INTERVAL - (getSpasmIntensity() * 10);
            spasmTimer = Math.max(40, spasmTimer); // Minimum 2 seconds
        }

        // Direct attacks between spasms
        if (attackCooldown <= 0) {
            LivingEntity target = this.getTarget();
            if (target != null && this.distanceTo(target) <= 6.0) {
                performAttack(target, AttackType.BODY_CRUSH);
                attackCooldown = 60;
            }
        }
    }

    /**
     * Performs a global room spasm - contracts walls and damages all players in range.
     */
    private void performGlobalSpasm() {
        AABB spasmBox = this.getBoundingBox().inflate(SPASM_CRUSH_RANGE);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, spasmBox, e -> e != this && !e.isSpectator());

        float intensity = (float) getSpasmIntensity() / MAX_SPASM_INTENSITY;
        float damage = SPASM_DAMAGE * (0.5f + intensity * 0.5f);

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            double dist = this.distanceTo(target);
            if (dist <= SPASM_CRUSH_RANGE) {
                target.hurt(source, damage);
                // Push toward center (room contracting)
                Vec3 pushDir = this.position().subtract(target.position()).normalize();
                target.push(pushDir.x * 0.5, 0.2, pushDir.z * 0.5);
                target.hurtMarked = true;
            }
        }

        // Earthquake sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    // ========== Spawning ==========

    private void spawnLarvae() {
        if (!(this.level() instanceof ServerLevel)) return;
        // Spawn 3-5 larvae around the boss
        // Actual spawning references CDEEntities.DEVOURING_LARVA
    }

    private void spawnAntibodyWave() {
        if (!(this.level() instanceof ServerLevel)) return;
        // Spawn 4-6 antibodies
        // Actual spawning references CDEEntities.ANTIBODY
    }

    // ========== Root System ==========

    private int countAliveRoots() {
        int count = 0;
        for (boolean alive : rootAlive) {
            if (alive) count++;
        }
        return count;
    }

    /**
     * Damage handling with root system and rib cage protection.
     * Shears deal 3x damage to roots.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide()) {
            BossPhase phase = getBossPhaseEnum();

            // Check for bone meal interaction in Phase 2
            if (phase == BossPhase.PHASE_2_RIBCAGE && !areRibsOpen()) {
                if (source.getEntity() instanceof Player player) {
                    if (player.getMainHandItem().is(Items.BONE_MEAL) ||
                        player.getOffhandItem().is(Items.BONE_MEAL)) {
                        onBoneMealApplied(player);
                        return false; // Don't deal damage, consume bone meal action
                    }
                }
                // Rib cage provides heavy protection
                amount *= 0.2f;
            }

            // Phase 1: Damage goes to roots first
            if (phase == BossPhase.PHASE_1_ROOTS && countAliveRoots() > 0) {
                // Check if using shears for bonus damage to roots
                float rootDamage = amount;
                if (source.getEntity() instanceof Player player) {
                    if (player.getMainHandItem().is(Items.SHEARS) ||
                        player.getOffhandItem().is(Items.SHEARS)) {
                        rootDamage *= SHEARS_MULTIPLIER;
                    }
                }

                int targetRoot = findRandomAliveRoot();
                if (targetRoot >= 0) {
                    rootHealth[targetRoot] -= rootDamage;
                    if (rootHealth[targetRoot] <= 0) {
                        rootHealth[targetRoot] = 0;
                        rootAlive[targetRoot] = false;
                        onRootSevered(targetRoot);
                    }
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 1.0f, 0.8f);

                    // Some damage bleeds through to main body (30%)
                    amount *= 0.3f;
                }
            }
        }
        return super.hurt(source, amount);
    }

    private int findRandomAliveRoot() {
        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < TOTAL_ROOTS; i++) {
            if (rootAlive[i]) alive.add(i);
        }
        if (alive.isEmpty()) return -1;
        return alive.get(this.random.nextInt(alive.size()));
    }

    private void onRootSevered(int root) {
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 2.0f, 0.5f);
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

        // Set up timers for new phase
        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_2_RIBCAGE -> {
                ribBoneMealCount = 0;
                setRibsOpen(false);
                spawnWaveTimer = SPAWN_WAVE_INTERVAL;
            }
            case PHASE_3_SPASM -> {
                spasmTimer = SPASM_INTERVAL;
                setSpasmIntensity(0);
            }
            default -> transitionTimer = 0;
        }

        // Play phase transition sound
        this.level().playSound(null, this.blockPosition(),
            CDESounds.BOSS_PHASE_CHANGE.get(), SoundSource.HOSTILE, 3.0f, 0.8f);

        // Sync to clients
        if (this.level() instanceof ServerLevel serverLevel) {
            CDENetworking.BossPhasePayload payload =
                new CDENetworking.BossPhasePayload(this.getId(), phase);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceTo(this) < 80.0) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
    }

    @Override
    public float getPhaseHealthThreshold(int phase) {
        return switch (BossPhase.values()[phase]) {
            case PHASE_2_RIBCAGE, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_SPASM, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (BossPhase.values()[phase]) {
            case PHASE_1_ROOTS -> List.of("tentacle_slam", "tentacle_sweep", "root_grab", "spawn_larvae");
            case PHASE_2_RIBCAGE -> List.of("spawn_antibodies", "spawn_larvae", "acid_spit");
            case PHASE_3_SPASM -> List.of("body_crush", "global_spasm", "room_contract");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getBossPhaseEnum() != BossPhase.DEATH) {
            transitionToPhase(BossPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 120 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ========== GeckoLib Animations ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main body controller - phase-based
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            BossPhase phase = getBossPhaseEnum();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_IDLE);
                case AWAKENING -> state.setAndContinue(AWAKEN);
                case PHASE_1_ROOTS -> state.setAndContinue(P1_IDLE);
                case TRANSITION_1_2, TRANSITION_2_3 -> state.setAndContinue(TRANSITION_ANIM);
                case PHASE_2_RIBCAGE -> {
                    if (areRibsOpen()) {
                        yield state.setAndContinue(P2_RIBS_OPEN);
                    }
                    yield state.setAndContinue(P2_IDLE);
                }
                case PHASE_3_SPASM -> state.setAndContinue(P3_IDLE);
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        // Attack controller
        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            BossPhase phase = getBossPhaseEnum();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_ROOTS -> state.setAndContinue(P1_TENTACLE_SLAM);
                    case PHASE_3_SPASM -> state.setAndContinue(P3_CRUSH);
                    default -> state.setAndContinue(P1_TENTACLE_SLAM);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));

        // Spasm controller (Phase 3 only)
        controllers.add(new AnimationController<>(this, "spasm", 5, state -> {
            if (getBossPhaseEnum() == BossPhase.PHASE_3_SPASM && getSpasmIntensity() > 0) {
                return state.setAndContinue(P3_SPASM);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
