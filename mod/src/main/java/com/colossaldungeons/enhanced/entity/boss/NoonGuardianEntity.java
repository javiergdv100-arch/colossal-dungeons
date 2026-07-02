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
 * Noon Guardian - Solar Palace miniboss.
 *
 * Ceremonial colossus at max power during simulated noon.
 * Increases arena light (Zenith event) to strengthen self.
 * Sweeping solar beam rotates around room.
 * Eclipse (water on altar/extinguishing spotlights) weakens dramatically.
 *
 * Implements IBossPhase with 2 phases.
 *
 * Stats: 340 HP, 8 armor, 0.18 speed, 14 damage.
 */
public class NoonGuardianEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum GuardianPhase {
        DORMANT,
        PHASE_1,
        ZENITH,
        PHASE_2,
        ECLIPSE,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(NoonGuardianEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_ECLIPSED =
        SynchedEntityData.defineId(NoonGuardianEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> BEAM_ANGLE =
        SynchedEntityData.defineId(NoonGuardianEntity.class, EntityDataSerializers.INT);

    private static final float PHASE_2_THRESHOLD = 0.45f;
    private static final float BEAM_DAMAGE = 14.0f;
    private static final float ECLIPSE_DAMAGE_MULTIPLIER = 2.5f;
    private static final float ZENITH_DAMAGE_BONUS = 1.5f;
    private static final int BEAM_ROTATION_SPEED = 3; // degrees per tick
    private static final int ECLIPSE_DURATION = 100; // 5 seconds
    private static final double BEAM_REACH = 12.0;

    private final BossPhaseManager<NoonGuardianEntity> phaseManager;
    private int transitionTimer = 0;
    private int eclipseTimer = 0;
    private int attackCooldown = 0;
    private int beamAngle = 0;
    private boolean zenithActive = false;

    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.noon_guardian.dormant");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.noon_guardian.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.noon_guardian.move");
    private static final RawAnimation BEAM_SWEEP = RawAnimation.begin().thenLoop("animation.noon_guardian.beam_sweep");
    private static final RawAnimation ZENITH_ANIM = RawAnimation.begin().thenPlay("animation.noon_guardian.zenith");
    private static final RawAnimation ECLIPSE_ANIM = RawAnimation.begin().thenLoop("animation.noon_guardian.eclipse");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.noon_guardian.attack");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.noon_guardian.death");

    public NoonGuardianEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(GuardianPhase.PHASE_2.ordinal(), PHASE_2_THRESHOLD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 340.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, GuardianPhase.DORMANT.ordinal());
        builder.define(IS_ECLIPSED, false);
        builder.define(BEAM_ANGLE, 0);
    }

    public GuardianPhase getGuardianPhase() {
        return GuardianPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setGuardianPhase(GuardianPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isEclipsed() {
        return this.entityData.get(IS_ECLIPSED);
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

            GuardianPhase phase = getGuardianPhase();

            switch (phase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 14.0);
                    if (nearest != null) {
                        transitionToPhase(GuardianPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1, PHASE_2 -> {
                    phaseManager.tick();
                    tickCombat();
                }
                case ECLIPSE -> {
                    eclipseTimer--;
                    if (eclipseTimer <= 0) {
                        this.entityData.set(IS_ECLIPSED, false);
                        GuardianPhase returnPhase = this.getHealth() / this.getMaxHealth() > PHASE_2_THRESHOLD
                            ? GuardianPhase.PHASE_1 : GuardianPhase.PHASE_2;
                        setGuardianPhase(returnPhase);
                    }
                }
                case DEATH -> {}
            }
        }
    }

    private void tickCombat() {
        Player target = this.level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        // Sweeping beam rotation
        beamAngle = (beamAngle + BEAM_ROTATION_SPEED) % 360;
        this.entityData.set(BEAM_ANGLE, beamAngle);

        // Check if beam hits any player
        checkBeamHit();

        double distance = this.distanceTo(target);

        // Move toward target
        if (distance > 5.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.12).add(0, this.getDeltaMovement().y, 0));
        }

        // Melee attack
        if (distance < 4.5 && attackCooldown <= 0) {
            float damage = BEAM_DAMAGE;
            if (zenithActive) damage *= ZENITH_DAMAGE_BONUS;
            target.hurt(this.damageSources().mobAttack(this), damage);
            attackCooldown = 35;
        }

        // Activate zenith periodically
        if (!zenithActive && this.tickCount % 300 == 0) {
            activateZenith();
        }
    }

    private void checkBeamHit() {
        double radians = Math.toRadians(beamAngle);
        Vec3 beamDir = new Vec3(Math.cos(radians), 0, Math.sin(radians));

        AABB area = this.getBoundingBox().inflate(BEAM_REACH);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            Vec3 toPlayer = player.position().subtract(this.position()).normalize();
            double dot = beamDir.dot(new Vec3(toPlayer.x, 0, toPlayer.z).normalize());

            if (dot > 0.9 && this.distanceTo(player) <= BEAM_REACH) {
                float damage = BEAM_DAMAGE * 0.5f;
                if (zenithActive) damage *= ZENITH_DAMAGE_BONUS;
                player.hurt(this.damageSources().magic(), damage);
                player.setRemainingFireTicks(40);
            }
        }
    }

    private void activateZenith() {
        zenithActive = true;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5f, 1.5f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + 3.0, this.getZ(), 20, 3.0, 1.0, 3.0, 0.1);
        }
    }

    /**
     * Triggers an eclipse, dramatically weakening the guardian.
     */
    public void triggerEclipse() {
        this.entityData.set(IS_ECLIPSED, true);
        setGuardianPhase(GuardianPhase.ECLIPSE);
        eclipseTimer = ECLIPSE_DURATION;
        zenithActive = false;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isEclipsed()) {
            amount *= ECLIPSE_DAMAGE_MULTIPLIER;
        }

        // Water on altar triggers eclipse
        if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
            if (!isEclipsed()) {
                triggerEclipse();
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public int getCurrentPhase() {
        return getGuardianPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        GuardianPhase newPhase = GuardianPhase.values()[phase];
        GuardianPhase oldPhase = getGuardianPhase();
        if (newPhase == oldPhase) return;

        setGuardianPhase(newPhase);

        switch (newPhase) {
            case PHASE_2 -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
            }
            default -> {}
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.8f);

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
        return switch (GuardianPhase.values()[phase]) {
            case PHASE_2 -> PHASE_2_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (GuardianPhase.values()[phase]) {
            case PHASE_1 -> List.of("beam_sweep", "melee_slam", "zenith_activate");
            case PHASE_2 -> List.of("beam_sweep", "melee_slam", "zenith_activate", "double_beam", "solar_nova");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getGuardianPhase() != GuardianPhase.DEATH) {
            transitionToPhase(GuardianPhase.DEATH.ordinal());
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
            GuardianPhase phase = getGuardianPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case PHASE_1, PHASE_2 -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case ECLIPSE -> state.setAndContinue(ECLIPSE_ANIM);
                case DEATH -> state.setAndContinue(DEATH_ANIM);
                default -> state.setAndContinue(IDLE);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK_ANIM);
            }
            if (beamAngle > 0 && !isEclipsed()) {
                return state.setAndContinue(BEAM_SWEEP);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
