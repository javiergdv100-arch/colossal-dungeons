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
import net.minecraft.tags.DamageTypeTags;
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
 * Summit Keeper - A miniboss from the Veiled Peak dungeon.
 *
 * An ice/wind colossus that controls arena wind direction and triggers
 * small avalanches. Fire breaks its ice shell (removes armor temporarily).
 *
 * Implements IBossPhase with 2 phases:
 * Phase 1: Wind control + ice attacks + avalanche triggers
 * Phase 2: Enraged mode with faster attacks and constant wind pressure
 *
 * Stats: 340 HP, 7 armor, 0.2 speed, 13 attack damage.
 */
public class SummitKeeperEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum KeeperPhase {
        DORMANT,
        PHASE_1,
        TRANSITION,
        PHASE_2,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(SummitKeeperEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> ICE_SHELL_BROKEN =
        SynchedEntityData.defineId(SummitKeeperEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> WIND_DIRECTION =
        SynchedEntityData.defineId(SummitKeeperEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.45f;

    // Combat parameters
    private static final int WIND_CHANGE_INTERVAL = 160; // 8 seconds
    private static final double WIND_PUSH_STRENGTH = 0.3;
    private static final int AVALANCHE_COOLDOWN = 200; // 10 seconds
    private static final int AVALANCHE_RADIUS = 8;
    private static final float AVALANCHE_DAMAGE = 8.0f;
    private static final int ICE_SHELL_REGEN_TIME = 200; // 10 seconds to regenerate shell
    private static final float BASE_ARMOR = 7.0f;

    private final BossPhaseManager<SummitKeeperEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 60;

    private int windChangeTimer = 0;
    private int avalancheCooldown = 0;
    private int iceShellRegenTimer = 0;
    private int attackCooldown = 0;

    // Wind directions: 0=North, 1=East, 2=South, 3=West
    private static final Vec3[] WIND_VECTORS = {
        new Vec3(0, 0, -1), new Vec3(1, 0, 0),
        new Vec3(0, 0, 1), new Vec3(-1, 0, 0)
    };

    // Animations
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.summit_keeper.dormant");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.summit_keeper.phase1_idle");
    private static final RawAnimation P1_WALK = RawAnimation.begin().thenLoop("animation.summit_keeper.phase1_walk");
    private static final RawAnimation P1_SLAM = RawAnimation.begin().thenPlay("animation.summit_keeper.phase1_slam");
    private static final RawAnimation WIND_CAST = RawAnimation.begin().thenPlay("animation.summit_keeper.wind_cast");
    private static final RawAnimation AVALANCHE = RawAnimation.begin().thenPlay("animation.summit_keeper.avalanche");
    private static final RawAnimation TRANSITION_ANIM = RawAnimation.begin().thenPlay("animation.summit_keeper.transition");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.summit_keeper.phase2_idle");
    private static final RawAnimation P2_WALK = RawAnimation.begin().thenLoop("animation.summit_keeper.phase2_walk");
    private static final RawAnimation P2_FRENZY = RawAnimation.begin().thenPlay("animation.summit_keeper.phase2_frenzy");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.summit_keeper.death");

    public SummitKeeperEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(KeeperPhase.TRANSITION.ordinal(), PHASE_2_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Summit Keeper.
     * 340 HP, 7 armor, 0.2 speed, 13 damage, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 340.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 13.0)
            .add(Attributes.ARMOR, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, KeeperPhase.DORMANT.ordinal());
        builder.define(ICE_SHELL_BROKEN, false);
        builder.define(WIND_DIRECTION, 0);
    }

    public KeeperPhase getKeeperPhase() {
        return KeeperPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setKeeperPhase(KeeperPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isIceShellBroken() {
        return this.entityData.get(ICE_SHELL_BROKEN);
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick() due to phase complexity
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (avalancheCooldown > 0) avalancheCooldown--;

            KeeperPhase currentPhase = getKeeperPhase();

            switch (currentPhase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 14.0);
                    if (nearest != null) {
                        transitionToPhase(KeeperPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1 -> {
                    phaseManager.tick();
                    tickPhase1();
                    tickWind();
                    tickIceShellRegen();
                }
                case TRANSITION -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(KeeperPhase.PHASE_2.ordinal());
                    }
                }
                case PHASE_2 -> {
                    tickPhase2();
                    tickWind();
                    tickIceShellRegen();
                }
                case DEATH -> {
                    // Death animation
                }
            }
        }
    }

    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 40.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Melee slam
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            attackCooldown = 40;
        }

        // Trigger avalanche
        if (avalancheCooldown <= 0 && distance < 15.0) {
            triggerAvalanche();
            avalancheCooldown = AVALANCHE_COOLDOWN;
        }

        // Move toward target
        if (distance > 4.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.12).add(0, this.getDeltaMovement().y, 0));
        }
    }

    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 40.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Faster melee in phase 2
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3f);
            attackCooldown = 25;
        }

        // More frequent avalanches
        if (avalancheCooldown <= 0 && distance < 20.0) {
            triggerAvalanche();
            avalancheCooldown = AVALANCHE_COOLDOWN / 2;
        }

        // Faster movement in phase 2
        if (distance > 3.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.scale(0.18).add(0, this.getDeltaMovement().y, 0));
        }
    }

    /**
     * Applies wind push to all players in range based on current wind direction.
     */
    private void tickWind() {
        windChangeTimer++;
        if (windChangeTimer >= WIND_CHANGE_INTERVAL) {
            windChangeTimer = 0;
            int newDir = this.random.nextInt(4);
            this.entityData.set(WIND_DIRECTION, newDir);
        }

        // Push players in wind direction
        int dir = this.entityData.get(WIND_DIRECTION);
        Vec3 windVec = WIND_VECTORS[dir];
        double pushStrength = getKeeperPhase() == KeeperPhase.PHASE_2 ?
            WIND_PUSH_STRENGTH * 1.5 : WIND_PUSH_STRENGTH;

        AABB area = this.getBoundingBox().inflate(20.0);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);
        for (Player player : players) {
            player.push(windVec.x * pushStrength, 0, windVec.z * pushStrength);
        }
    }

    /**
     * Triggers an avalanche in a radius around the keeper.
     */
    private void triggerAvalanche() {
        AABB area = this.getBoundingBox().inflate(AVALANCHE_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            player.hurt(this.damageSources().mobAttack(this), AVALANCHE_DAMAGE);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        // Avalanche particles
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                this.getX(), this.getY() + 3.0, this.getZ(), 50,
                AVALANCHE_RADIUS, 2.0, AVALANCHE_RADIUS, 0.1);
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    /**
     * Regenerates ice shell over time if broken.
     */
    private void tickIceShellRegen() {
        if (isIceShellBroken()) {
            iceShellRegenTimer++;
            if (iceShellRegenTimer >= ICE_SHELL_REGEN_TIME) {
                this.entityData.set(ICE_SHELL_BROKEN, false);
                this.getAttribute(Attributes.ARMOR).setBaseValue(BASE_ARMOR);
                iceShellRegenTimer = 0;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fire breaks ice shell - removes armor temporarily
        if (source.is(DamageTypeTags.IS_FIRE) && !isIceShellBroken()) {
            this.entityData.set(ICE_SHELL_BROKEN, true);
            this.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
            iceShellRegenTimer = 0;

            // Ice cracking sound
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.7f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 2.0, this.getZ(), 20, 1.0, 1.0, 1.0, 0.1);
            }
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getKeeperPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        KeeperPhase newPhase = KeeperPhase.values()[phase];
        KeeperPhase oldPhase = getKeeperPhase();
        if (newPhase == oldPhase) return;

        setKeeperPhase(newPhase);

        switch (newPhase) {
            case TRANSITION -> transitionTimer = TRANSITION_DURATION;
            case PHASE_2 -> {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26);
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(16.0);
            }
            default -> transitionTimer = 0;
        }

        // Play phase change sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);

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
        return switch (KeeperPhase.values()[phase]) {
            case PHASE_2, TRANSITION -> PHASE_2_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (KeeperPhase.values()[phase]) {
            case PHASE_1 -> List.of("slam", "wind_gust", "avalanche");
            case PHASE_2 -> List.of("slam", "wind_gust", "avalanche", "frenzy", "ice_storm");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getKeeperPhase() != KeeperPhase.DEATH) {
            transitionToPhase(KeeperPhase.DEATH.ordinal());
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
            KeeperPhase phase = getKeeperPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case PHASE_1 -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_WALK);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION -> state.setAndContinue(TRANSITION_ANIM);
                case PHASE_2 -> {
                    if (state.isMoving()) yield state.setAndContinue(P2_WALK);
                    yield state.setAndContinue(P2_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            KeeperPhase phase = getKeeperPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1 -> state.setAndContinue(P1_SLAM);
                    case PHASE_2 -> state.setAndContinue(P2_FRENZY);
                    default -> state.setAndContinue(P1_SLAM);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
