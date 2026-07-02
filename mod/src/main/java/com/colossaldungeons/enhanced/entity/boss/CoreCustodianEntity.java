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
import net.minecraft.world.item.Items;
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
 * Core Custodian - Worldbearer dungeon miniboss.
 *
 * A massive stone automaton that guards the core mechanisms of the Worldbearer.
 * Manipulates tilting platforms in its arena and throws a chained counterweight ball
 * at players. If the ball is redirected (via fishing rod mechanic) to hit a wall,
 * the custodian is stunned for a significant duration, creating a damage window.
 *
 * Stats: 300 HP, 9 armor, 14 damage + heavy knockback.
 * Phases: PATROL -> COMBAT -> STUNNED (cycles)
 * Key Mechanic: Fishing rod redirects the thrown ball to stun the boss.
 */
public class CoreCustodianEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss combat phases.
     */
    public enum BossPhase {
        PATROL,
        COMBAT,
        THROWING_BALL,
        STUNNED,
        ENRAGED,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(CoreCustodianEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> BALL_IN_FLIGHT =
        SynchedEntityData.defineId(CoreCustodianEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> STUN_TICKS =
        SynchedEntityData.defineId(CoreCustodianEntity.class, EntityDataSerializers.INT);

    // Combat constants
    private static final float BALL_DAMAGE = 14.0f;
    private static final float BALL_KNOCKBACK = 3.0f;
    private static final int THROW_WINDUP = 40; // 2 seconds windup
    private static final int STUN_DURATION = 100; // 5 seconds stunned
    private static final int THROW_COOLDOWN = 120; // 6 seconds between throws
    private static final double THROW_RANGE = 12.0;
    private static final float ENRAGE_THRESHOLD = 0.3f; // Enrages below 30% HP

    private int throwTimer = 0;
    private int throwCooldown = 0;
    private Vec3 ballPosition = Vec3.ZERO;
    private Vec3 ballVelocity = Vec3.ZERO;
    private boolean ballRedirected = false;
    private int platformTiltTimer = 0;

    private final BossPhaseManager<CoreCustodianEntity> phaseManager;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.core_custodian.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.core_custodian.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.core_custodian.attack");
    private static final RawAnimation THROW = RawAnimation.begin().thenPlay("animation.core_custodian.throw");
    private static final RawAnimation STUNNED_ANIM = RawAnimation.begin().thenLoop("animation.core_custodian.stunned");
    private static final RawAnimation ENRAGED_IDLE = RawAnimation.begin().thenLoop("animation.core_custodian.enraged_idle");
    private static final RawAnimation ENRAGED_ATTACK = RawAnimation.begin().thenPlay("animation.core_custodian.enraged_attack");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.core_custodian.death");
    private static final RawAnimation PLATFORM_TILT = RawAnimation.begin().thenPlay("animation.core_custodian.platform_tilt");

    public CoreCustodianEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(BossPhase.ENRAGED.ordinal(), ENRAGE_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for Core Custodian.
     * 300 HP, 0.2 speed, 14 damage, 9 armor, 0.9 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 9.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, BossPhase.PATROL.ordinal());
        builder.define(BALL_IN_FLIGHT, false);
        builder.define(STUN_TICKS, 0);
    }

    public BossPhase getBossPhaseEnum() {
        return BossPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setBossPhaseEnum(BossPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isBallInFlight() {
        return this.entityData.get(BALL_IN_FLIGHT);
    }

    private void setBallInFlight(boolean inFlight) {
        this.entityData.set(BALL_IN_FLIGHT, inFlight);
    }

    public int getStunTicks() {
        return this.entityData.get(STUN_TICKS);
    }

    private void setStunTicks(int ticks) {
        this.entityData.set(STUN_TICKS, ticks);
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
                case PATROL -> {
                    Player nearest = this.level().getNearestPlayer(this, 16.0);
                    if (nearest != null) {
                        setBossPhaseEnum(BossPhase.COMBAT);
                    }
                }
                case COMBAT, ENRAGED -> {
                    phaseManager.tick();
                    throwCooldown--;

                    // Throw ball when cooldown is up and target is in range
                    LivingEntity target = this.getTarget();
                    if (target != null && throwCooldown <= 0 && this.distanceTo(target) <= THROW_RANGE) {
                        startThrowBall(target);
                    }

                    // Platform tilt mechanic - periodically tilts the arena
                    platformTiltTimer--;
                    if (platformTiltTimer <= 0 && phase == BossPhase.ENRAGED) {
                        tiltPlatforms();
                        platformTiltTimer = 100; // Every 5 seconds when enraged
                    }
                }
                case THROWING_BALL -> {
                    throwTimer--;
                    if (throwTimer <= 0) {
                        releaseBall();
                    }
                }
                case STUNNED -> {
                    int stun = getStunTicks();
                    stun--;
                    setStunTicks(stun);
                    if (stun <= 0) {
                        // Recover from stun
                        if (this.getHealth() / this.getMaxHealth() <= ENRAGE_THRESHOLD) {
                            setBossPhaseEnum(BossPhase.ENRAGED);
                        } else {
                            setBossPhaseEnum(BossPhase.COMBAT);
                        }
                        throwCooldown = THROW_COOLDOWN;
                    }
                }
                case DEATH -> {
                    // Death animation handled by tickDeath
                }
            }

            // Update ball physics if in flight
            if (isBallInFlight()) {
                updateBallPhysics();
            }
        }
    }

    /**
     * Begins the throw windup animation.
     */
    private void startThrowBall(LivingEntity target) {
        setBossPhaseEnum(BossPhase.THROWING_BALL);
        throwTimer = THROW_WINDUP;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /**
     * Releases the chained ball toward the target.
     */
    private void releaseBall() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            ballPosition = this.position().add(0, 2.0, 0);
            Vec3 direction = target.position().subtract(ballPosition).normalize();
            ballVelocity = direction.scale(0.8);
            setBallInFlight(true);
            ballRedirected = false;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 2.0f, 0.7f);
        }

        // Return to combat phase
        if (this.getHealth() / this.getMaxHealth() <= ENRAGE_THRESHOLD) {
            setBossPhaseEnum(BossPhase.ENRAGED);
        } else {
            setBossPhaseEnum(BossPhase.COMBAT);
        }
        throwCooldown = THROW_COOLDOWN;
    }

    /**
     * Updates the ball physics, checking for collisions with players and walls.
     */
    private void updateBallPhysics() {
        ballPosition = ballPosition.add(ballVelocity);

        // Check collision with players
        AABB ballBox = new AABB(ballPosition.subtract(0.5, 0.5, 0.5), ballPosition.add(0.5, 0.5, 0.5));
        List<LivingEntity> hit = this.level().getEntitiesOfClass(
            LivingEntity.class, ballBox, e -> e != this && !e.isSpectator());

        for (LivingEntity target : hit) {
            if (target instanceof Player player) {
                // Check if player is using fishing rod to redirect
                if (player.getMainHandItem().is(Items.FISHING_ROD) ||
                    player.getOffhandItem().is(Items.FISHING_ROD)) {
                    // Redirect ball back toward a wall
                    ballVelocity = ballVelocity.reverse().scale(1.2);
                    ballRedirected = true;
                    this.level().playSound(null, BlockPos.containing(ballPosition),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.5f, 1.0f);
                    continue;
                }
            }

            // Ball hits the target
            target.hurt(this.damageSources().mobAttack(this), BALL_DAMAGE);
            Vec3 knockDir = ballVelocity.normalize();
            target.knockback(BALL_KNOCKBACK, -knockDir.x, -knockDir.z);
            target.hurtMarked = true;
            setBallInFlight(false);
            return;
        }

        // Check if ball hit a wall (solid block)
        BlockPos ballBlockPos = BlockPos.containing(ballPosition);
        if (!this.level().getBlockState(ballBlockPos).isAir()) {
            if (ballRedirected) {
                // Ball was redirected and hit wall - stun the boss!
                triggerSelfStun();
            }
            setBallInFlight(false);
            this.level().playSound(null, ballBlockPos,
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0f, 0.5f);
        }

        // Ball despawns if travels too far (chain length limit)
        if (ballPosition.distanceTo(this.position()) > 20.0) {
            setBallInFlight(false);
        }
    }

    /**
     * Stuns the boss when its own redirected ball hits a wall.
     */
    private void triggerSelfStun() {
        setBossPhaseEnum(BossPhase.STUNNED);
        setStunTicks(STUN_DURATION);
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 2.0f, 0.3f);
    }

    /**
     * Tilts the platforms in the arena, pushing players in a direction.
     */
    private void tiltPlatforms() {
        AABB arena = this.getBoundingBox().inflate(10.0);
        List<Player> players = this.level().getEntitiesOfClass(
            Player.class, arena, p -> !p.isSpectator() && !p.isCreative());

        Vec3 tiltDir = new Vec3(
            this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize().scale(0.5);

        for (Player player : players) {
            player.push(tiltDir.x, 0.1, tiltDir.z);
            player.hurtMarked = true;
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.5f, 0.5f);
    }

    /**
     * Takes extra damage while stunned.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getBossPhaseEnum() == BossPhase.STUNNED) {
            amount *= 2.0f; // Double damage while stunned
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isNoAi() {
        BossPhase phase = getBossPhaseEnum();
        if (phase == BossPhase.STUNNED || phase == BossPhase.THROWING_BALL) {
            return true;
        }
        return super.isNoAi();
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
            case ENRAGED -> ENRAGE_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (BossPhase.values()[phase]) {
            case COMBAT -> List.of("melee", "throw_ball", "platform_tilt");
            case ENRAGED -> List.of("melee", "throw_ball", "platform_tilt", "rapid_melee");
            case STUNNED -> List.of(); // Cannot attack while stunned
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getBossPhaseEnum() != BossPhase.DEATH) {
            transitionToPhase(BossPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 80 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 10, state -> {
            BossPhase phase = getBossPhaseEnum();
            return switch (phase) {
                case PATROL -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case COMBAT -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case THROWING_BALL -> state.setAndContinue(THROW);
                case STUNNED -> state.setAndContinue(STUNNED_ANIM);
                case ENRAGED -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(ENRAGED_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            BossPhase phase = getBossPhaseEnum();
            if (this.swinging) {
                if (phase == BossPhase.ENRAGED) {
                    return state.setAndContinue(ENRAGED_ATTACK);
                }
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
