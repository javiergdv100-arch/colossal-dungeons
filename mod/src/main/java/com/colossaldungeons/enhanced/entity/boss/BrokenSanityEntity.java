package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * Broken Sanity - The final boss of the Descent into Madness dungeon.
 *
 * Personification of madness with 3 distinct phases:
 * Phase 1: False clones (only one is real, shadow detail reveals).
 * Phase 2: Room distortion + voice traps (recovery compass shows real exit).
 * Phase 3: Inverted controls + hostile memory spawns (candle of sanity marks real).
 *
 * Milk is ESSENTIAL between phases for removing distortion debuffs.
 *
 * Stats: 1400 HP, 5 armor, 0.22 speed, 15-22 damage.
 * Implements IBossPhase with BossPhaseManager and 3 health thresholds.
 */
public class BrokenSanityEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum SanityPhase {
        DORMANT,
        AWAKENING,
        PHASE_1_CLONES,
        TRANSITION_1_2,
        PHASE_2_DISTORTION,
        TRANSITION_2_3,
        PHASE_3_INVERSION,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(BrokenSanityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLONE_COUNT =
        SynchedEntityData.defineId(BrokenSanityEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_REAL_FORM =
        SynchedEntityData.defineId(BrokenSanityEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROOM_DISTORTED =
        SynchedEntityData.defineId(BrokenSanityEntity.class, EntityDataSerializers.BOOLEAN);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.65f;
    private static final float PHASE_3_THRESHOLD = 0.30f;

    // Phase 1: Clone mechanics
    private static final int MAX_CLONES = 4;
    private static final float CLONE_DECOY_DAMAGE = 3.0f;
    private static final int CLONE_RESPAWN_COOLDOWN = 120;

    // Phase 2: Distortion mechanics
    private static final int VOICE_TRAP_COOLDOWN = 100;
    private static final int DISTORTION_DEBUFF_DURATION = 200;
    private static final float PHASE_2_DAMAGE = 18.0f;

    // Phase 3: Inversion mechanics
    private static final int MEMORY_SPAWN_COOLDOWN = 150;
    private static final int INVERSION_DEBUFF_DURATION = 300;
    private static final float PHASE_3_DAMAGE = 22.0f;

    private final BossPhaseManager<BrokenSanityEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 80;
    private static final int AWAKENING_DURATION = 100;

    // Phase state
    private int attackCooldown = 0;
    private int specialCooldown = 0;
    private int cloneRespawnTimer = 0;
    private int voiceTrapCooldown = 0;
    private int memorySpawnCooldown = 0;
    private final List<Vec3> clonePositions = new ArrayList<>();

    // Animations
    private static final RawAnimation DORMANT_ANIM =
        RawAnimation.begin().thenLoop("animation.broken_sanity.dormant");
    private static final RawAnimation AWAKEN_ANIM =
        RawAnimation.begin().thenPlay("animation.broken_sanity.awaken");
    private static final RawAnimation P1_IDLE =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase1_idle");
    private static final RawAnimation P1_WALK =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase1_walk");
    private static final RawAnimation P1_CLONE_ATTACK =
        RawAnimation.begin().thenPlay("animation.broken_sanity.phase1_clone_attack");
    private static final RawAnimation TRANSITION_12_ANIM =
        RawAnimation.begin().thenPlay("animation.broken_sanity.transition_12");
    private static final RawAnimation P2_IDLE =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase2_idle");
    private static final RawAnimation P2_WALK =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase2_walk");
    private static final RawAnimation P2_DISTORT =
        RawAnimation.begin().thenPlay("animation.broken_sanity.phase2_distort");
    private static final RawAnimation TRANSITION_23_ANIM =
        RawAnimation.begin().thenPlay("animation.broken_sanity.transition_23");
    private static final RawAnimation P3_IDLE =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase3_idle");
    private static final RawAnimation P3_WALK =
        RawAnimation.begin().thenLoop("animation.broken_sanity.phase3_walk");
    private static final RawAnimation P3_INVERT =
        RawAnimation.begin().thenPlay("animation.broken_sanity.phase3_invert");
    private static final RawAnimation P3_SPAWN_MEMORY =
        RawAnimation.begin().thenPlay("animation.broken_sanity.phase3_memory");
    private static final RawAnimation DEATH_ANIM =
        RawAnimation.begin().thenPlay("animation.broken_sanity.death");

    public BrokenSanityEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(SanityPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(SanityPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for Broken Sanity.
     * 1400 HP, 5 armor, 0.22 speed, 15 base damage, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1400.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, SanityPhase.DORMANT.ordinal());
        builder.define(CLONE_COUNT, 0);
        builder.define(IS_REAL_FORM, true);
        builder.define(ROOM_DISTORTED, false);
    }

    public SanityPhase getSanityPhase() {
        return SanityPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setSanityPhase(SanityPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick() due to complex phase behavior
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (specialCooldown > 0) specialCooldown--;
            if (cloneRespawnTimer > 0) cloneRespawnTimer--;
            if (voiceTrapCooldown > 0) voiceTrapCooldown--;
            if (memorySpawnCooldown > 0) memorySpawnCooldown--;

            SanityPhase currentPhase = getSanityPhase();
            switch (currentPhase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 16.0);
                    if (nearest != null) {
                        transitionToPhase(SanityPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SanityPhase.PHASE_1_CLONES.ordinal());
                    }
                }
                case PHASE_1_CLONES -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SanityPhase.PHASE_2_DISTORTION.ordinal());
                    }
                }
                case PHASE_2_DISTORTION -> {
                    phaseManager.tick();
                    tickPhase2();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SanityPhase.PHASE_3_INVERSION.ordinal());
                    }
                }
                case PHASE_3_INVERSION -> {
                    tickPhase3();
                }
                case DEATH -> { }
            }
        }
    }

    // ========== Phase 1: False Clones ==========
    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Maintain clones
        if (clonePositions.size() < MAX_CLONES && cloneRespawnTimer <= 0) {
            spawnClone();
            cloneRespawnTimer = CLONE_RESPAWN_COOLDOWN;
        }

        // Attack from clone positions (decoy hits)
        if (specialCooldown <= 0 && !clonePositions.isEmpty()) {
            performCloneAttack(target);
            specialCooldown = 60;
        }

        // Real form melee
        double distance = this.distanceTo(target);
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            attackCooldown = 35;
        }

        if (distance > 5.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.14).add(0, this.getDeltaMovement().y, 0));
        }
    }

    private void spawnClone() {
        double angle = this.random.nextDouble() * Math.PI * 2;
        double dist = 5.0 + this.random.nextDouble() * 8.0;
        Vec3 pos = this.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        clonePositions.add(pos);
        this.entityData.set(CLONE_COUNT, clonePositions.size());

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pos.x, pos.y + 1.0, pos.z, 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private void performCloneAttack(Player target) {
        // Random clone attacks
        if (!clonePositions.isEmpty()) {
            Vec3 clonePos = clonePositions.get(this.random.nextInt(clonePositions.size()));
            double dist = target.position().distanceTo(clonePos);
            if (dist < 5.0) {
                target.hurt(this.damageSources().mobAttack(this), CLONE_DECOY_DAMAGE);
            }
        }
    }

    /**
     * Called when a player hits a clone position. Removes the clone.
     */
    public void onCloneHit(int cloneIndex) {
        if (cloneIndex >= 0 && cloneIndex < clonePositions.size()) {
            clonePositions.remove(cloneIndex);
            this.entityData.set(CLONE_COUNT, clonePositions.size());
        }
    }

    // ========== Phase 2: Room Distortion ==========
    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        this.entityData.set(ROOM_DISTORTED, true);

        // Voice trap mechanic
        if (voiceTrapCooldown <= 0) {
            performVoiceTrap(target);
            voiceTrapCooldown = VOICE_TRAP_COOLDOWN;
        }

        // Apply distortion debuffs periodically
        if (this.tickCount % 100 == 0) {
            AABB area = this.getBoundingBox().inflate(20.0);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, DISTORTION_DEBUFF_DURATION, 0));
            }
        }

        // Melee with increased damage
        double distance = this.distanceTo(target);
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_2_DAMAGE);
            attackCooldown = 30;
        }

        if (distance > 4.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.16).add(0, this.getDeltaMovement().y, 0));
        }
    }

    private void performVoiceTrap(Player target) {
        // Play misleading sounds from false positions
        Vec3 falseDir = new Vec3(
            this.random.nextDouble() - 0.5,
            0,
            this.random.nextDouble() - 0.5
        ).normalize().scale(10.0);
        Vec3 falsePos = target.position().add(falseDir);
        BlockPos soundPos = BlockPos.containing(falsePos);

        this.level().playSound(null, soundPos,
            SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    // ========== Phase 3: Inverted Controls + Hostile Memories ==========
    private void tickPhase3() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Apply inversion debuff (Slowness + Blindness + Confusion simulates inverted controls)
        if (this.tickCount % 80 == 0) {
            AABB area = this.getBoundingBox().inflate(25.0);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, INVERSION_DEBUFF_DURATION, 1));
                player.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS, INVERSION_DEBUFF_DURATION, 0));
            }
        }

        // Spawn hostile memories
        if (memorySpawnCooldown <= 0) {
            spawnHostileMemory(target);
            memorySpawnCooldown = MEMORY_SPAWN_COOLDOWN;
        }

        // Aggressive melee with max damage
        double distance = this.distanceTo(target);
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_3_DAMAGE);
            attackCooldown = 25;
        }

        if (distance > 3.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.2).add(0, this.getDeltaMovement().y, 0));
        }

        // Madness particles
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.WARPED_SPORE,
                this.getX(), this.getY() + 2.0, this.getZ(), 5, 3.0, 1.0, 3.0, 0.02);
        }
    }

    private void spawnHostileMemory(Player target) {
        // Visual representation of hostile memory spawn
        if (this.level() instanceof ServerLevel serverLevel) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 4.0 + this.random.nextDouble() * 6.0;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;

            serverLevel.sendParticles(ParticleTypes.SOUL,
                x, target.getY() + 1.0, z, 15, 0.3, 0.5, 0.3, 0.05);
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.3f);
    }

    // ========== Damage Handling ==========
    @Override
    public boolean hurt(DamageSource source, float amount) {
        SanityPhase phase = getSanityPhase();

        // Phase 1: Only take full damage when hitting the real form
        if (phase == SanityPhase.PHASE_1_CLONES && clonePositions.size() > 0) {
            // Hitting clones does reduced damage to the real entity
            amount *= 0.4f;
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========
    @Override
    public int getCurrentPhase() {
        return getSanityPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        SanityPhase newPhase = SanityPhase.values()[phase];
        SanityPhase oldPhase = getSanityPhase();
        if (newPhase == oldPhase) return;

        setSanityPhase(newPhase);

        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_1_CLONES -> {
                clonePositions.clear();
                this.entityData.set(CLONE_COUNT, 0);
            }
            case PHASE_2_DISTORTION -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
                this.entityData.set(ROOM_DISTORTED, true);
            }
            case PHASE_3_INVERSION -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_3_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
            }
            default -> transitionTimer = 0;
        }

        // Phase change sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.3f);

        // Network sync
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
        return switch (SanityPhase.values()[phase]) {
            case PHASE_2_DISTORTION, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_INVERSION, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (SanityPhase.values()[phase]) {
            case PHASE_1_CLONES -> List.of("clone_attack", "real_strike", "shadow_dash");
            case PHASE_2_DISTORTION -> List.of("voice_trap", "distort", "melee", "confusion_wave");
            case PHASE_3_INVERSION -> List.of("invert", "memory_spawn", "melee", "madness_burst");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getSanityPhase() != SanityPhase.DEATH) {
            transitionToPhase(SanityPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 120 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ========== GeckoLib Animation ==========
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            SanityPhase phase = getSanityPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case AWAKENING -> state.setAndContinue(AWAKEN_ANIM);
                case PHASE_1_CLONES -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_WALK);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION_1_2 -> state.setAndContinue(TRANSITION_12_ANIM);
                case PHASE_2_DISTORTION -> {
                    if (state.isMoving()) yield state.setAndContinue(P2_WALK);
                    yield state.setAndContinue(P2_IDLE);
                }
                case TRANSITION_2_3 -> state.setAndContinue(TRANSITION_23_ANIM);
                case PHASE_3_INVERSION -> {
                    if (state.isMoving()) yield state.setAndContinue(P3_WALK);
                    yield state.setAndContinue(P3_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            SanityPhase phase = getSanityPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_CLONES -> state.setAndContinue(P1_CLONE_ATTACK);
                    case PHASE_2_DISTORTION -> state.setAndContinue(P2_DISTORT);
                    case PHASE_3_INVERSION -> state.setAndContinue(P3_INVERT);
                    default -> state.setAndContinue(P1_CLONE_ATTACK);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
