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

import java.util.List;

/**
 * Chained Helios - The final boss of the Solar Palace.
 *
 * A chained solar entity (small living sun). 3-phase boss.
 * Phase 1: flares + rolling solar spheres across arena.
 * Phase 2: breaks chains, light beams that must be blocked/reflected with mirrors/shield.
 * Phase 3: Total Zenith igniting all oil on floor; triggering eclipse
 * (water on central altar) opens lethal damage window.
 * Bone meal CRITICAL to clean oil before Phase 3.
 *
 * Implements IBossPhase with BossPhaseManager, 3 thresholds (70%/40%/15%).
 *
 * Stats: 1450 HP, 7 armor, 0.2 speed, 16-22 damage + fire.
 * Fire immune (set in EntityType.Builder).
 */
public class ChainedHeliosEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum HeliosPhase {
        DORMANT,
        AWAKENING,
        PHASE_1_CHAINED,
        TRANSITION_1_2,
        PHASE_2_UNCHAINED,
        TRANSITION_2_3,
        PHASE_3_ZENITH,
        ECLIPSE_VULNERABLE,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(ChainedHeliosEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_ECLIPSED =
        SynchedEntityData.defineId(ChainedHeliosEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> CHAINS_BROKEN =
        SynchedEntityData.defineId(ChainedHeliosEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.70f;
    private static final float PHASE_3_THRESHOLD = 0.40f;
    private static final float ECLIPSE_THRESHOLD = 0.15f;

    // Damage values
    private static final float PHASE_1_DAMAGE = 16.0f;
    private static final float PHASE_2_DAMAGE = 18.0f;
    private static final float PHASE_3_DAMAGE = 22.0f;
    private static final float FLARE_DAMAGE = 8.0f;
    private static final float BEAM_DAMAGE = 14.0f;
    private static final float ECLIPSE_DAMAGE_MULTIPLIER = 3.0f;

    private static final int TRANSITION_DURATION = 100;
    private static final int AWAKENING_DURATION = 120;
    private static final int ECLIPSE_DURATION = 80; // 4 seconds of extreme vulnerability
    private static final int MAX_CHAINS = 4;

    private final BossPhaseManager<ChainedHeliosEntity> phaseManager;
    private int transitionTimer = 0;
    private int attackCooldown = 0;
    private int flareCooldown = 0;
    private int beamCooldown = 0;
    private int eclipseTimer = 0;
    private int solarSphereCooldown = 0;
    private boolean oilIgnited = false;

    // ========== Animations ==========
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.chained_helios.dormant");
    private static final RawAnimation AWAKEN_ANIM = RawAnimation.begin().thenPlay("animation.chained_helios.awaken");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.chained_helios.phase1_idle");
    private static final RawAnimation P1_FLARE = RawAnimation.begin().thenPlay("animation.chained_helios.phase1_flare");
    private static final RawAnimation P1_SPHERE = RawAnimation.begin().thenPlay("animation.chained_helios.phase1_sphere");
    private static final RawAnimation TRANS_12 = RawAnimation.begin().thenPlay("animation.chained_helios.transition_12");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.chained_helios.phase2_idle");
    private static final RawAnimation P2_MOVE = RawAnimation.begin().thenLoop("animation.chained_helios.phase2_move");
    private static final RawAnimation P2_BEAM = RawAnimation.begin().thenPlay("animation.chained_helios.phase2_beam");
    private static final RawAnimation TRANS_23 = RawAnimation.begin().thenPlay("animation.chained_helios.transition_23");
    private static final RawAnimation P3_IDLE = RawAnimation.begin().thenLoop("animation.chained_helios.phase3_idle");
    private static final RawAnimation P3_MOVE = RawAnimation.begin().thenLoop("animation.chained_helios.phase3_move");
    private static final RawAnimation P3_IGNITE = RawAnimation.begin().thenPlay("animation.chained_helios.phase3_ignite");
    private static final RawAnimation P3_ZENITH = RawAnimation.begin().thenPlay("animation.chained_helios.phase3_zenith");
    private static final RawAnimation ECLIPSE_ANIM = RawAnimation.begin().thenLoop("animation.chained_helios.eclipse");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.chained_helios.death");

    public ChainedHeliosEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(HeliosPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(HeliosPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1450.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, HeliosPhase.DORMANT.ordinal());
        builder.define(IS_ECLIPSED, false);
        builder.define(CHAINS_BROKEN, 0);
    }

    public HeliosPhase getHeliosPhase() {
        return HeliosPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setHeliosPhase(HeliosPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isEclipsed() {
        return this.entityData.get(IS_ECLIPSED);
    }

    public int getChainsBroken() {
        return this.entityData.get(CHAINS_BROKEN);
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick()
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (flareCooldown > 0) flareCooldown--;
            if (beamCooldown > 0) beamCooldown--;
            if (solarSphereCooldown > 0) solarSphereCooldown--;

            HeliosPhase phase = getHeliosPhase();

            switch (phase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 16.0);
                    if (nearest != null) {
                        transitionToPhase(HeliosPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(HeliosPhase.PHASE_1_CHAINED.ordinal());
                    }
                }
                case PHASE_1_CHAINED -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(HeliosPhase.PHASE_2_UNCHAINED.ordinal());
                    }
                }
                case PHASE_2_UNCHAINED -> {
                    phaseManager.tick();
                    tickPhase2();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(HeliosPhase.PHASE_3_ZENITH.ordinal());
                    }
                }
                case PHASE_3_ZENITH -> {
                    tickPhase3();
                }
                case ECLIPSE_VULNERABLE -> {
                    eclipseTimer--;
                    if (eclipseTimer <= 0) {
                        this.entityData.set(IS_ECLIPSED, false);
                        setHeliosPhase(HeliosPhase.PHASE_3_ZENITH);
                    }
                }
                case DEATH -> {}
            }
        }
    }

    // ========== Phase 1: Chained - Flares + Solar Spheres ==========

    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Flare attack
        if (flareCooldown <= 0) {
            performFlare(target);
            flareCooldown = 80;
        }

        // Rolling solar spheres
        if (solarSphereCooldown <= 0) {
            launchSolarSphere(target);
            solarSphereCooldown = 120;
        }
    }

    private void performFlare(Player target) {
        double distance = this.distanceTo(target);
        if (distance < 14.0) {
            target.hurt(this.damageSources().onFire(), FLARE_DAMAGE);
            target.setRemainingFireTicks(60);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5f, 0.8f);
        }
    }

    private void launchSolarSphere(Player target) {
        // Conceptual: sphere rolls across arena damaging anything in path
        AABB area = this.getBoundingBox().inflate(10.0);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
        for (Player player : players) {
            if (this.random.nextFloat() < 0.4f) {
                player.hurt(this.damageSources().onFire(), FLARE_DAMAGE * 0.7f);
                player.setRemainingFireTicks(40);
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY() + 1.0, this.getZ(), 20, 3.0, 0.5, 3.0, 0.08);
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 1.5f);
    }

    // ========== Phase 2: Unchained - Light Beams ==========

    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Move toward target (now unchained, can move)
        if (distance > 6.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.18).add(0, this.getDeltaMovement().y, 0));
        }

        // Light beam attacks
        if (beamCooldown <= 0) {
            performBeam(target);
            beamCooldown = 60;
        }

        // Melee fire attack
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_2_DAMAGE);
            target.setRemainingFireTicks(60);
            attackCooldown = 30;
        }
    }

    private void performBeam(Player target) {
        // Shield reflects beam back
        if (target.isBlocking()) {
            this.hurt(this.damageSources().magic(), BEAM_DAMAGE * 2.0f);
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 2.0f, 0.8f);
            return;
        }

        target.hurt(this.damageSources().magic(), BEAM_DAMAGE);
        target.setRemainingFireTicks(60);

        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 start = this.position().add(0, 1.5, 0);
            Vec3 end = target.position().add(0, 1.0, 0);
            Vec3 dir = end.subtract(start).normalize();
            double dist = start.distanceTo(end);
            for (double d = 0; d < dist; d += 0.5) {
                Vec3 pos = start.add(dir.scale(d));
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 2.0f);
    }

    // ========== Phase 3: Total Zenith ==========

    private void tickPhase3() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Ignite all oil on floor
        if (!oilIgnited) {
            igniteArenaOil();
            oilIgnited = true;
        }

        // Constant fire damage in arena
        if (this.tickCount % 20 == 0) {
            AABB area = this.getBoundingBox().inflate(15.0);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : players) {
                player.setRemainingFireTicks(40);
                if (this.tickCount % 40 == 0) {
                    player.hurt(this.damageSources().onFire(), 4.0f);
                }
            }
        }

        // Move and attack
        if (distance > 4.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.2).add(0, this.getDeltaMovement().y, 0));
        }

        if (distance < 5.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_3_DAMAGE);
            target.setRemainingFireTicks(100);
            attackCooldown = 25;
        }

        // Beam attacks continue in phase 3
        if (beamCooldown <= 0) {
            performBeam(target);
            beamCooldown = 50;
        }
    }

    private void igniteArenaOil() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY(), this.getZ(), 50, 8.0, 0.1, 8.0, 0.05);
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0f, 0.3f);
    }

    /**
     * Triggers eclipse state - water on central altar.
     * Opens the lethal damage window.
     */
    public void triggerEclipse() {
        if (getHeliosPhase() == HeliosPhase.PHASE_3_ZENITH) {
            this.entityData.set(IS_ECLIPSED, true);
            setHeliosPhase(HeliosPhase.ECLIPSE_VULNERABLE);
            eclipseTimer = ECLIPSE_DURATION;
            oilIgnited = false;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 3.0f, 0.3f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 30, 2.0, 2.0, 2.0, 0.1);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Eclipse = massive vulnerability
        if (isEclipsed()) {
            amount *= ECLIPSE_DAMAGE_MULTIPLIER;
        }

        // Water triggers eclipse in phase 3
        if (getHeliosPhase() == HeliosPhase.PHASE_3_ZENITH) {
            if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
                triggerEclipse();
            }
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getHeliosPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        HeliosPhase newPhase = HeliosPhase.values()[phase];
        HeliosPhase oldPhase = getHeliosPhase();
        if (newPhase == oldPhase) return;

        setHeliosPhase(newPhase);

        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_1_CHAINED -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_1_DAMAGE);
            }
            case PHASE_2_UNCHAINED -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
                this.entityData.set(CHAINS_BROKEN, MAX_CHAINS);
            }
            case PHASE_3_ZENITH -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_3_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
                oilIgnited = false;
            }
            default -> transitionTimer = 0;
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.5f);

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
        return switch (HeliosPhase.values()[phase]) {
            case PHASE_2_UNCHAINED, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_ZENITH, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (HeliosPhase.values()[phase]) {
            case PHASE_1_CHAINED -> List.of("flare", "solar_sphere");
            case PHASE_2_UNCHAINED -> List.of("light_beam", "fire_melee", "solar_sphere");
            case PHASE_3_ZENITH -> List.of("total_ignite", "light_beam", "fire_melee", "zenith_nova");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getHeliosPhase() != HeliosPhase.DEATH) {
            transitionToPhase(HeliosPhase.DEATH.ordinal());
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
            HeliosPhase phase = getHeliosPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case AWAKENING -> state.setAndContinue(AWAKEN_ANIM);
                case PHASE_1_CHAINED -> state.setAndContinue(P1_IDLE);
                case TRANSITION_1_2 -> state.setAndContinue(TRANS_12);
                case PHASE_2_UNCHAINED -> {
                    if (state.isMoving()) yield state.setAndContinue(P2_MOVE);
                    yield state.setAndContinue(P2_IDLE);
                }
                case TRANSITION_2_3 -> state.setAndContinue(TRANS_23);
                case PHASE_3_ZENITH -> {
                    if (state.isMoving()) yield state.setAndContinue(P3_MOVE);
                    yield state.setAndContinue(P3_IDLE);
                }
                case ECLIPSE_VULNERABLE -> state.setAndContinue(ECLIPSE_ANIM);
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            HeliosPhase phase = getHeliosPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_CHAINED -> state.setAndContinue(P1_FLARE);
                    case PHASE_2_UNCHAINED -> state.setAndContinue(P2_BEAM);
                    case PHASE_3_ZENITH -> state.setAndContinue(P3_ZENITH);
                    default -> state.setAndContinue(P1_FLARE);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
