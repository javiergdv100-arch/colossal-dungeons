package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * Semblance - A miniboss from the Descent into Madness dungeon.
 *
 * A living mirror of the player's decisions. Learns and repeats player attack
 * patterns. Shape-shifts based on player's combat style. Vulnerable ONLY when
 * player breaks their own routine (uses unconventional items/tactics).
 *
 * Implements IBossPhase with 2 phases:
 * Phase 1: Learning mode - copies and reflects attacks
 * Phase 2: Mimicry complete - uses all learned patterns, faster and stronger
 *
 * Stats: 280 HP, 4 armor, 0.28 speed, 12 attack damage.
 */
public class SemblanceEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum SemblancePhase {
        DORMANT,
        PHASE_1_LEARNING,
        TRANSITION,
        PHASE_2_MIMICRY,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(SemblanceEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_VULNERABLE =
        SynchedEntityData.defineId(SemblanceEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> LEARNED_PATTERNS =
        SynchedEntityData.defineId(SemblanceEntity.class, EntityDataSerializers.INT);

    // Phase threshold
    private static final float PHASE_2_THRESHOLD = 0.50f;

    // Learning system
    private static final int MAX_PATTERNS = 5;
    private static final int PATTERN_MEMORY_DURATION = 600; // 30 seconds memory
    private static final int VULNERABILITY_WINDOW = 60; // 3 seconds of vulnerability
    private static final int REFLECT_COOLDOWN = 40;

    // Combat
    private static final float PHASE_1_DAMAGE = 12.0f;
    private static final float PHASE_2_DAMAGE = 15.0f;
    private static final float COUNTER_DAMAGE_MULTIPLIER = 1.5f;

    private final BossPhaseManager<SemblanceEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 60;

    // Pattern tracking
    private final List<String> learnedAttackPatterns = new ArrayList<>();
    private final List<Long> patternTimestamps = new ArrayList<>();
    private int lastDamageSourceHash = 0;
    private int consecutiveSameAttacks = 0;
    private int vulnerabilityTimer = 0;
    private int reflectCooldown = 0;
    private int attackCooldown = 0;

    // Animations
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.semblance.dormant");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.semblance.phase1_idle");
    private static final RawAnimation P1_WALK = RawAnimation.begin().thenLoop("animation.semblance.phase1_walk");
    private static final RawAnimation P1_MIRROR = RawAnimation.begin().thenPlay("animation.semblance.phase1_mirror");
    private static final RawAnimation P1_COUNTER = RawAnimation.begin().thenPlay("animation.semblance.phase1_counter");
    private static final RawAnimation TRANSITION_ANIM = RawAnimation.begin().thenPlay("animation.semblance.transition");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.semblance.phase2_idle");
    private static final RawAnimation P2_WALK = RawAnimation.begin().thenLoop("animation.semblance.phase2_walk");
    private static final RawAnimation P2_ATTACK = RawAnimation.begin().thenPlay("animation.semblance.phase2_attack");
    private static final RawAnimation P2_SHAPESHIFT = RawAnimation.begin().thenPlay("animation.semblance.phase2_shapeshift");
    private static final RawAnimation VULNERABLE_ANIM = RawAnimation.begin().thenLoop("animation.semblance.vulnerable");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.semblance.death");

    public SemblanceEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(SemblancePhase.TRANSITION.ordinal(), PHASE_2_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Semblance.
     * 280 HP, 4 armor, 0.28 speed, 12 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 280.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, SemblancePhase.DORMANT.ordinal());
        builder.define(IS_VULNERABLE, false);
        builder.define(LEARNED_PATTERNS, 0);
    }

    public SemblancePhase getSemblancePhase() {
        return SemblancePhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setSemblancePhase(SemblancePhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isVulnerable() {
        return this.entityData.get(IS_VULNERABLE);
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick() due to complex mirror behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (reflectCooldown > 0) reflectCooldown--;
            if (attackCooldown > 0) attackCooldown--;

            // Handle vulnerability window
            if (isVulnerable()) {
                vulnerabilityTimer--;
                if (vulnerabilityTimer <= 0) {
                    this.entityData.set(IS_VULNERABLE, false);
                }
            }

            // Clean old patterns
            long currentTime = this.level().getGameTime();
            while (!patternTimestamps.isEmpty() &&
                   currentTime - patternTimestamps.get(0) > PATTERN_MEMORY_DURATION) {
                patternTimestamps.remove(0);
                if (!learnedAttackPatterns.isEmpty()) {
                    learnedAttackPatterns.remove(0);
                }
            }

            SemblancePhase currentPhase = getSemblancePhase();

            switch (currentPhase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 12.0);
                    if (nearest != null) {
                        transitionToPhase(SemblancePhase.PHASE_1_LEARNING.ordinal());
                    }
                }
                case PHASE_1_LEARNING -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SemblancePhase.PHASE_2_MIMICRY.ordinal());
                    }
                }
                case PHASE_2_MIMICRY -> {
                    tickPhase2();
                }
                case DEATH -> {
                    // Death animation
                }
            }

            // Mirror particles
            if (currentPhase != SemblancePhase.DORMANT && currentPhase != SemblancePhase.DEATH) {
                if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 8 == 0) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.3, 0.5, 0.3, 0.01);
                }
            }
        }
    }

    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Mirror the player's position (stay at similar distance)
        if (distance > 6.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.15).add(0, this.getDeltaMovement().y, 0));
        }

        // Counter-attack using learned patterns
        if (attackCooldown <= 0 && distance < 4.0 && !learnedAttackPatterns.isEmpty()) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_1_DAMAGE);
            attackCooldown = 40;
        }
    }

    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Aggressive pursuit
        if (distance > 3.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.2).add(0, this.getDeltaMovement().y, 0));
        }

        // Attack with enhanced damage
        if (attackCooldown <= 0 && distance < 4.0) {
            float damage = PHASE_2_DAMAGE;
            // Extra damage using counter if player repeats patterns
            if (consecutiveSameAttacks >= 3) {
                damage *= COUNTER_DAMAGE_MULTIPLIER;
            }
            target.hurt(this.damageSources().mobAttack(this), damage);
            attackCooldown = 30;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        SemblancePhase phase = getSemblancePhase();

        // Learn the attack pattern
        if (source.getEntity() instanceof Player player) {
            String patternKey = source.type().msgId();
            int sourceHash = patternKey.hashCode();

            // Track consecutive same attacks
            if (sourceHash == lastDamageSourceHash) {
                consecutiveSameAttacks++;
            } else {
                consecutiveSameAttacks = 0;
                lastDamageSourceHash = sourceHash;
            }

            // Learn new pattern
            if (learnedAttackPatterns.size() < MAX_PATTERNS) {
                learnedAttackPatterns.add(patternKey);
                patternTimestamps.add(this.level().getGameTime());
                this.entityData.set(LEARNED_PATTERNS, learnedAttackPatterns.size());
            }

            // Check if player broke their routine (unconventional attack)
            // If the attack source is different from all learned patterns, become vulnerable
            if (!learnedAttackPatterns.contains(patternKey) && learnedAttackPatterns.size() >= 3) {
                this.entityData.set(IS_VULNERABLE, true);
                vulnerabilityTimer = VULNERABILITY_WINDOW;

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.getX(), this.getY() + 1.5, this.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                }

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.5f);
            }
        }

        // Reduced damage when not vulnerable
        if (!isVulnerable() && phase != SemblancePhase.DORMANT) {
            amount *= 0.25f;
        }

        // Reflect damage when not vulnerable (in phase 1)
        if (!isVulnerable() && phase == SemblancePhase.PHASE_1_LEARNING
            && reflectCooldown <= 0 && source.getEntity() instanceof LivingEntity attacker) {
            attacker.hurt(this.damageSources().magic(), amount * 0.5f);
            reflectCooldown = REFLECT_COOLDOWN;
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getSemblancePhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        SemblancePhase newPhase = SemblancePhase.values()[phase];
        SemblancePhase oldPhase = getSemblancePhase();
        if (newPhase == oldPhase) return;

        setSemblancePhase(newPhase);

        switch (newPhase) {
            case TRANSITION -> transitionTimer = TRANSITION_DURATION;
            case PHASE_2_MIMICRY -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
            }
            default -> transitionTimer = 0;
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 1.0f);

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
        return switch (SemblancePhase.values()[phase]) {
            case PHASE_2_MIMICRY, TRANSITION -> PHASE_2_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (SemblancePhase.values()[phase]) {
            case PHASE_1_LEARNING -> List.of("mirror", "counter", "reflect");
            case PHASE_2_MIMICRY -> List.of("mirror", "counter", "shapeshift", "burst", "predict");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getSemblancePhase() != SemblancePhase.DEATH) {
            transitionToPhase(SemblancePhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 80 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            SemblancePhase phase = getSemblancePhase();

            if (isVulnerable()) {
                return state.setAndContinue(VULNERABLE_ANIM);
            }

            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case PHASE_1_LEARNING -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_WALK);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION -> state.setAndContinue(TRANSITION_ANIM);
                case PHASE_2_MIMICRY -> {
                    if (state.isMoving()) yield state.setAndContinue(P2_WALK);
                    yield state.setAndContinue(P2_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            SemblancePhase phase = getSemblancePhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_LEARNING -> state.setAndContinue(P1_COUNTER);
                    case PHASE_2_MIMICRY -> state.setAndContinue(P2_ATTACK);
                    default -> state.setAndContinue(P1_COUNTER);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
