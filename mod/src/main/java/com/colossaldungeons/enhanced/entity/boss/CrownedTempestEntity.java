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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
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
 * Crowned Tempest - The final boss of the Veiled Peak dungeon.
 *
 * A living storm spirit with 3 distinct phases:
 * Phase 1: Wind gusts pushing toward arena edges + ice shard projectiles.
 * Phase 2: Fog invisibility, attacks guided by thunder sound direction.
 * Phase 3: Lightning strikes in marked zones + total blizzard conditions.
 *
 * Lighting beacons in the arena exposes the boss each turn.
 *
 * Stats: 1250 HP, 5 armor, 0.24 speed, 14-20 damage + lightning.
 *
 * Implements IBossPhase with BossPhaseManager and 3 health thresholds.
 */
public class CrownedTempestEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum TempestPhase {
        DORMANT,
        AWAKENING,
        PHASE_1_WIND,
        TRANSITION_1_2,
        PHASE_2_FOG,
        TRANSITION_2_3,
        PHASE_3_LIGHTNING,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(CrownedTempestEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_EXPOSED =
        SynchedEntityData.defineId(CrownedTempestEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> BEACONS_LIT =
        SynchedEntityData.defineId(CrownedTempestEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.65f;
    private static final float PHASE_3_THRESHOLD = 0.30f;

    // Combat parameters - Phase 1
    private static final float PHASE_1_DAMAGE = 14.0f;
    private static final double WIND_GUST_STRENGTH = 0.8;
    private static final int WIND_GUST_COOLDOWN = 80;
    private static final int ICE_SHARD_COOLDOWN = 60;
    private static final float ICE_SHARD_DAMAGE = 6.0f;

    // Combat parameters - Phase 2
    private static final float PHASE_2_DAMAGE = 17.0f;
    private static final int THUNDER_SOUND_INTERVAL = 100;

    // Combat parameters - Phase 3
    private static final float PHASE_3_DAMAGE = 20.0f;
    private static final int LIGHTNING_STRIKE_COOLDOWN = 60;
    private static final int LIGHTNING_MARK_DURATION = 40;
    private static final float LIGHTNING_DAMAGE = 12.0f;
    private static final int BLIZZARD_SLOW_DURATION = 40;

    // State management
    private final BossPhaseManager<CrownedTempestEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 80;
    private static final int AWAKENING_DURATION = 100;

    private int windGustCooldown = 0;
    private int iceShardCooldown = 0;
    private int lightningCooldown = 0;
    private int thunderSoundTimer = 0;
    private int attackCooldown = 0;

    // Lightning strike marking system
    private final List<BlockPos> markedPositions = new ArrayList<>();
    private int markTimer = 0;

    // Exposure from beacons
    private int exposureTimer = 0;
    private static final int EXPOSURE_DURATION = 100; // 5 seconds of vulnerability

    // ========== Animations ==========
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.crowned_tempest.dormant");
    private static final RawAnimation AWAKEN_ANIM = RawAnimation.begin().thenPlay("animation.crowned_tempest.awaken");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.crowned_tempest.phase1_idle");
    private static final RawAnimation P1_FLY = RawAnimation.begin().thenLoop("animation.crowned_tempest.phase1_fly");
    private static final RawAnimation P1_WIND_GUST = RawAnimation.begin().thenPlay("animation.crowned_tempest.phase1_wind");
    private static final RawAnimation P1_ICE_SHARD = RawAnimation.begin().thenPlay("animation.crowned_tempest.phase1_ice");
    private static final RawAnimation TRANSITION_12_ANIM = RawAnimation.begin().thenPlay("animation.crowned_tempest.transition_12");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.crowned_tempest.phase2_idle");
    private static final RawAnimation P2_FOG_ATTACK = RawAnimation.begin().thenPlay("animation.crowned_tempest.phase2_attack");
    private static final RawAnimation TRANSITION_23_ANIM = RawAnimation.begin().thenPlay("animation.crowned_tempest.transition_23");
    private static final RawAnimation P3_IDLE = RawAnimation.begin().thenLoop("animation.crowned_tempest.phase3_idle");
    private static final RawAnimation P3_FLY = RawAnimation.begin().thenLoop("animation.crowned_tempest.phase3_fly");
    private static final RawAnimation P3_LIGHTNING = RawAnimation.begin().thenPlay("animation.crowned_tempest.phase3_lightning");
    private static final RawAnimation P3_BLIZZARD = RawAnimation.begin().thenPlay("animation.crowned_tempest.phase3_blizzard");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.crowned_tempest.death");

    public CrownedTempestEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(TempestPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(TempestPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Crowned Tempest.
     * 1250 HP, 5 armor, 0.24 speed, 14 base damage, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1250.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, TempestPhase.DORMANT.ordinal());
        builder.define(IS_EXPOSED, false);
        builder.define(BEACONS_LIT, 0);
    }

    public TempestPhase getTempestPhase() {
        return TempestPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setTempestPhase(TempestPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isExposed() {
        return this.entityData.get(IS_EXPOSED);
    }

    @Override
    protected void registerGoals() {
        // AI entirely managed in tick() due to complex phase-based behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Decrement cooldowns
            if (windGustCooldown > 0) windGustCooldown--;
            if (iceShardCooldown > 0) iceShardCooldown--;
            if (lightningCooldown > 0) lightningCooldown--;
            if (attackCooldown > 0) attackCooldown--;

            // Handle exposure timer from beacons
            if (isExposed()) {
                exposureTimer--;
                if (exposureTimer <= 0) {
                    this.entityData.set(IS_EXPOSED, false);
                }
            }

            // Check for beacon light exposure
            checkBeaconExposure();

            TempestPhase currentPhase = getTempestPhase();

            switch (currentPhase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 18.0);
                    if (nearest != null) {
                        transitionToPhase(TempestPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(TempestPhase.PHASE_1_WIND.ordinal());
                    }
                }
                case PHASE_1_WIND -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(TempestPhase.PHASE_2_FOG.ordinal());
                    }
                }
                case PHASE_2_FOG -> {
                    phaseManager.tick();
                    tickPhase2();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(TempestPhase.PHASE_3_LIGHTNING.ordinal());
                    }
                }
                case PHASE_3_LIGHTNING -> {
                    tickPhase3();
                }
                case DEATH -> {
                    // Death animation
                }
            }

            // Float above ground
            if (currentPhase != TempestPhase.DORMANT && currentPhase != TempestPhase.DEATH) {
                if (this.position().y < this.blockPosition().getY() + 3.0) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
                }
                this.fallDistance = 0.0f;
            }
        }
    }

    // ========== Phase 1: Wind Gusts + Ice Shards ==========

    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Wind gust pushing toward edges
        if (windGustCooldown <= 0) {
            performWindGust(target);
            windGustCooldown = WIND_GUST_COOLDOWN;
        }

        // Ice shard ranged attack
        if (iceShardCooldown <= 0 && this.distanceTo(target) > 5.0) {
            performIceShard(target);
            iceShardCooldown = ICE_SHARD_COOLDOWN;
        }

        // Melee when close
        if (this.distanceTo(target) < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_1_DAMAGE);
            attackCooldown = 30;
        }

        // Move toward target
        moveTowardTarget(target, 0.15);
    }

    private void performWindGust(Player target) {
        // Push all players toward arena edges
        AABB area = this.getBoundingBox().inflate(15.0);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            Vec3 pushDir = player.position().subtract(this.position()).normalize();
            player.push(pushDir.x * WIND_GUST_STRENGTH, 0.3, pushDir.z * WIND_GUST_STRENGTH);
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 2.0f, 0.8f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY(), this.getZ(), 30, 5.0, 1.0, 5.0, 0.1);
        }
    }

    private void performIceShard(Player target) {
        // Deal ice shard damage
        target.hurt(this.damageSources().mobAttack(this), ICE_SHARD_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ========== Phase 2: Fog Invisibility + Thunder Sound ==========

    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Invisible unless exposed by beacon
        thunderSoundTimer++;
        if (thunderSoundTimer >= THUNDER_SOUND_INTERVAL) {
            thunderSoundTimer = 0;
            // Thunder sound from the boss's real position (directional hint)
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 0.5f);
        }

        // Attacks from fog
        if (attackCooldown <= 0 && this.distanceTo(target) < 5.0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_2_DAMAGE);
            attackCooldown = 40;

            // Brief reveal on attack
            this.entityData.set(IS_EXPOSED, true);
            exposureTimer = 20; // Brief flash only
        }

        // Erratic movement in fog
        if (this.tickCount % 40 == 0) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 5.0 + this.random.nextDouble() * 8.0;
            Vec3 newTarget = target.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            Vec3 dir = newTarget.subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.2).add(0, this.getDeltaMovement().y, 0));
        }

        // Fog particles
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                this.getX(), this.getY(), this.getZ(), 5, 3.0, 1.0, 3.0, 0.0);
        }
    }

    // ========== Phase 3: Lightning Strikes + Total Blizzard ==========

    private void tickPhase3() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Lightning strike marking and execution
        if (lightningCooldown <= 0) {
            markLightningZone(target);
            lightningCooldown = LIGHTNING_STRIKE_COOLDOWN;
        }

        // Execute marked lightning strikes
        if (!markedPositions.isEmpty()) {
            markTimer++;
            if (markTimer >= LIGHTNING_MARK_DURATION) {
                executeLightningStrikes();
                markTimer = 0;
            }
        }

        // Total blizzard: constant slowness to all players
        if (this.tickCount % 40 == 0) {
            AABB area = this.getBoundingBox().inflate(25.0);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLIZZARD_SLOW_DURATION, 0));
            }
        }

        // Melee with highest damage
        if (this.distanceTo(target) < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_3_DAMAGE);
            attackCooldown = 25;
        }

        // More aggressive movement
        moveTowardTarget(target, 0.2);

        // Blizzard particles
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                this.getX(), this.getY() + 5.0, this.getZ(), 20, 12.0, 3.0, 12.0, 0.05);
        }
    }

    /**
     * Marks positions where lightning will strike after a delay.
     */
    private void markLightningZone(Player target) {
        markedPositions.clear();

        // Mark 3-5 positions around the target
        int count = 3 + this.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 10.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 10.0;
            BlockPos markPos = target.blockPosition().offset((int) offsetX, 0, (int) offsetZ);
            markedPositions.add(markPos);

            // Visual marker (particles)
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    markPos.getX() + 0.5, markPos.getY() + 0.5, markPos.getZ() + 0.5,
                    10, 0.5, 0.0, 0.5, 0.01);
            }
        }
    }

    /**
     * Executes lightning strikes at all marked positions.
     */
    private void executeLightningStrikes() {
        for (BlockPos pos : markedPositions) {
            // Spawn lightning bolt
            if (this.level() instanceof ServerLevel serverLevel) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (bolt != null) {
                    bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    bolt.setVisualOnly(false);
                    serverLevel.addFreshEntity(bolt);
                }
            }

            // Additional damage to nearby players
            AABB strikeArea = new AABB(pos).inflate(2.0);
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, strikeArea);
            for (Player player : nearbyPlayers) {
                player.hurt(this.damageSources().lightningBolt(), LIGHTNING_DAMAGE);
            }
        }
        markedPositions.clear();
    }

    /**
     * Checks if beacon light exposes the tempest.
     */
    private void checkBeaconExposure() {
        int lightLevel = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());
        if (lightLevel >= 12 && !isExposed()) {
            this.entityData.set(IS_EXPOSED, true);
            exposureTimer = EXPOSURE_DURATION;
        }
    }

    private void moveTowardTarget(Player target, double speed) {
        double distance = this.distanceTo(target);
        if (distance > 5.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(speed).add(0, this.getDeltaMovement().y, 0));
        }
    }

    // ========== Damage Handling ==========

    @Override
    public boolean hurt(DamageSource source, float amount) {
        TempestPhase phase = getTempestPhase();

        // In phase 2, reduced damage unless exposed
        if (phase == TempestPhase.PHASE_2_FOG && !isExposed()) {
            amount *= 0.3f;
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvisible() {
        TempestPhase phase = getTempestPhase();
        if (phase == TempestPhase.PHASE_2_FOG && !isExposed()) {
            return true;
        }
        return super.isInvisible();
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getTempestPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        TempestPhase newPhase = TempestPhase.values()[phase];
        TempestPhase oldPhase = getTempestPhase();
        if (newPhase == oldPhase) return;

        setTempestPhase(newPhase);

        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_1_WIND -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_1_DAMAGE);
            }
            case PHASE_2_FOG -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
            }
            case PHASE_3_LIGHTNING -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_3_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30);
            }
            default -> transitionTimer = 0;
        }

        // Phase transition sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 0.3f);

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
        return switch (TempestPhase.values()[phase]) {
            case PHASE_2_FOG, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_LIGHTNING, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (TempestPhase.values()[phase]) {
            case PHASE_1_WIND -> List.of("wind_gust", "ice_shard", "melee");
            case PHASE_2_FOG -> List.of("fog_attack", "thunder_strike", "ambush");
            case PHASE_3_LIGHTNING -> List.of("lightning_zone", "blizzard", "melee", "ice_storm");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getTempestPhase() != TempestPhase.DEATH) {
            transitionToPhase(TempestPhase.DEATH.ordinal());
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
            TempestPhase phase = getTempestPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case AWAKENING -> state.setAndContinue(AWAKEN_ANIM);
                case PHASE_1_WIND -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_FLY);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION_1_2 -> state.setAndContinue(TRANSITION_12_ANIM);
                case PHASE_2_FOG -> state.setAndContinue(P2_IDLE);
                case TRANSITION_2_3 -> state.setAndContinue(TRANSITION_23_ANIM);
                case PHASE_3_LIGHTNING -> {
                    if (state.isMoving()) yield state.setAndContinue(P3_FLY);
                    yield state.setAndContinue(P3_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            TempestPhase phase = getTempestPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_WIND -> state.setAndContinue(P1_WIND_GUST);
                    case PHASE_2_FOG -> state.setAndContinue(P2_FOG_ATTACK);
                    case PHASE_3_LIGHTNING -> state.setAndContinue(P3_LIGHTNING);
                    default -> state.setAndContinue(P1_WIND_GUST);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
