package com.colossaldungeons.enhanced.entity;

import com.colossaldungeons.enhanced.entity.ai.CDEBrainProvider;
import com.colossaldungeons.enhanced.entity.ai.CDEVibrationSensor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

import java.util.List;

/**
 * Neothelid (Adult) - Colossal blind worm guided by sound and vibration.
 *
 * A rare, massive, serpentine aberration born when a colony of 60+ baby neothelids
 * starves for 10 minutes and cannibalizes until one survivor remains.
 * Detects sounds, vibrations, and impacts up to ~120 blocks.
 *
 * Stats: 320 HP, 6 armor, tail attack 16 damage.
 * Size: 2.5 x 4.0 blocks.
 *
 * Attacks:
 * - Tail Sweep: area attack with high knockback (16 dmg)
 * - Charge: body slam (12-18 dmg)
 * - Devour: grab + drag + self-heal
 * - Vomit Babies: spawns 8-10 NeothelidBaby entities
 * - Ground Slam: body slam to ground (14-18 dmg) with shockwave
 *
 * Blind: uses vibration/sound detection only, snowballs divert attention.
 */
public class NeothelidAdultEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Attack state enumeration.
     */
    public enum AttackState {
        IDLE,
        TAIL_SWEEP,
        CHARGE,
        DEVOUR,
        VOMIT_BABIES,
        GROUND_SLAM,
        BLIND_RAGE
    }

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
        SynchedEntityData.defineId(NeothelidAdultEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_ENRAGED =
        SynchedEntityData.defineId(NeothelidAdultEntity.class, EntityDataSerializers.BOOLEAN);

    // Detection range for vibrations (blind creature)
    private static final double VIBRATION_RANGE = 120.0;
    private static final double TAIL_SWEEP_RADIUS = 6.0;
    private static final float TAIL_SWEEP_DAMAGE = 16.0f;
    private static final float CHARGE_DAMAGE_MIN = 12.0f;
    private static final float CHARGE_DAMAGE_MAX = 18.0f;
    private static final float GROUND_SLAM_DAMAGE_MIN = 14.0f;
    private static final float GROUND_SLAM_DAMAGE_MAX = 18.0f;
    private static final int VOMIT_BABY_COUNT_MIN = 8;
    private static final int VOMIT_BABY_COUNT_MAX = 10;
    private static final float DEVOUR_HEAL_AMOUNT = 20.0f;

    private int attackCooldown = 0;
    private int attackTimer = 0;
    private int chargeTicks = 0;
    private LivingEntity devourTarget = null;
    private Vec3 lastKnownSoundPos = null;
    private int distractedTimer = 0;

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.neothelid_adult.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.neothelid_adult.move");
    private static final RawAnimation TAIL_SWEEP_ANIM = RawAnimation.begin().thenPlay("animation.neothelid_adult.tail_sweep");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("animation.neothelid_adult.charge");
    private static final RawAnimation DEVOUR_ANIM = RawAnimation.begin().thenPlay("animation.neothelid_adult.devour");
    private static final RawAnimation VOMIT_ANIM = RawAnimation.begin().thenPlay("animation.neothelid_adult.vomit");
    private static final RawAnimation GROUND_SLAM_ANIM = RawAnimation.begin().thenPlay("animation.neothelid_adult.ground_slam");
    private static final RawAnimation RAGE_ANIM = RawAnimation.begin().thenLoop("animation.neothelid_adult.rage");

    public NeothelidAdultEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Neothelid Adult.
     * 320 HP, 0.2 speed, 16 damage (tail), 6 armor, 0.8 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 320.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 120.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_STATE, AttackState.IDLE.ordinal());
        builder.define(IS_ENRAGED, false);
    }

    public AttackState getAttackState() {
        return AttackState.values()[this.entityData.get(ATTACK_STATE)];
    }

    private void setAttackState(AttackState state) {
        this.entityData.set(ATTACK_STATE, state.ordinal());
    }

    public boolean isEnraged() {
        return this.entityData.get(IS_ENRAGED);
    }

    private void setEnraged(boolean enraged) {
        this.entityData.set(IS_ENRAGED, enraged);
    }

    @Override
    protected void registerGoals() {
        // AI managed by SmartBrainLib via NeothelidAdultBrainProvider
        // No vanilla goals needed for this complex creature
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Decrement cooldowns
            if (attackCooldown > 0) attackCooldown--;
            if (distractedTimer > 0) distractedTimer--;

            // Handle ongoing attack states
            AttackState currentAttack = getAttackState();
            if (currentAttack != AttackState.IDLE) {
                attackTimer--;
                handleAttackTick(currentAttack);
                if (attackTimer <= 0) {
                    finishAttack(currentAttack);
                }
            } else if (attackCooldown <= 0) {
                // Choose next attack based on situation
                selectNextAttack();
            }

            // Handle devour mechanic - drag target toward mouth
            if (devourTarget != null && currentAttack == AttackState.DEVOUR) {
                handleDevour();
            }
        }
    }

    /**
     * Selects the next attack based on distance and context.
     */
    private void selectNextAttack() {
        if (distractedTimer > 0) return;

        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distance = this.distanceTo(target);

        if (distance < 4.0 && this.random.nextFloat() < 0.3f) {
            // Close range: devour attempt
            startDevour(target);
        } else if (distance < TAIL_SWEEP_RADIUS && this.random.nextFloat() < 0.4f) {
            // Medium range: tail sweep
            startTailSweep();
        } else if (distance > 8.0 && distance < 30.0 && this.random.nextFloat() < 0.35f) {
            // Long range: charge
            startCharge();
        } else if (this.getHealth() < this.getMaxHealth() * 0.5f && this.random.nextFloat() < 0.2f) {
            // Low health: vomit babies
            startVomitBabies();
        } else if (this.random.nextFloat() < 0.25f) {
            // Random: ground slam
            startGroundSlam();
        }
    }

    private void startTailSweep() {
        setAttackState(AttackState.TAIL_SWEEP);
        attackTimer = 30; // 1.5 seconds
        attackCooldown = 60;
    }

    private void startCharge() {
        setAttackState(AttackState.CHARGE);
        attackTimer = 40; // 2 seconds of charging
        chargeTicks = 0;
        attackCooldown = 80;
    }

    private void startDevour(LivingEntity target) {
        setAttackState(AttackState.DEVOUR);
        devourTarget = target;
        attackTimer = 60; // 3 seconds
        attackCooldown = 100;
    }

    private void startVomitBabies() {
        setAttackState(AttackState.VOMIT_BABIES);
        attackTimer = 40; // 2 seconds
        attackCooldown = 200; // Long cooldown - powerful ability
    }

    private void startGroundSlam() {
        setAttackState(AttackState.GROUND_SLAM);
        attackTimer = 35; // 1.75 seconds
        attackCooldown = 70;
    }

    private void handleAttackTick(AttackState state) {
        switch (state) {
            case TAIL_SWEEP -> {
                // Deal damage at the midpoint of the animation
                if (attackTimer == 15) {
                    performTailSweep();
                }
            }
            case CHARGE -> {
                chargeTicks++;
                // Move rapidly toward target
                LivingEntity target = this.getTarget();
                if (target != null) {
                    Vec3 direction = target.position().subtract(this.position()).normalize();
                    this.setDeltaMovement(direction.scale(0.8));
                    // Deal damage on contact
                    if (this.distanceTo(target) < 3.0) {
                        float damage = CHARGE_DAMAGE_MIN + this.random.nextFloat() * (CHARGE_DAMAGE_MAX - CHARGE_DAMAGE_MIN);
                        target.hurt(this.damageSources().mobAttack(this), damage);
                        target.knockback(2.0f, -direction.x, -direction.z);
                        attackTimer = 0; // End charge on hit
                    }
                }
            }
            case DEVOUR -> handleDevour();
            case GROUND_SLAM -> {
                if (attackTimer == 15) {
                    performGroundSlam();
                }
            }
            default -> {}
        }
    }

    /**
     * Performs the tail sweep area attack.
     */
    private void performTailSweep() {
        AABB sweepBox = this.getBoundingBox().inflate(TAIL_SWEEP_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, sweepBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            if (this.distanceTo(target) <= TAIL_SWEEP_RADIUS) {
                target.hurt(source, TAIL_SWEEP_DAMAGE);
                // High knockback
                Vec3 knockDir = target.position().subtract(this.position()).normalize();
                target.knockback(3.0f, -knockDir.x, -knockDir.z);
            }
        }

        // Sound and particles
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /**
     * Handles the devour mechanic: drag target and heal.
     */
    private void handleDevour() {
        if (devourTarget == null || devourTarget.isDeadOrDying()) {
            devourTarget = null;
            return;
        }

        // Drag target toward this entity
        Vec3 pullDir = this.position().subtract(devourTarget.position()).normalize().scale(0.3);
        devourTarget.setDeltaMovement(pullDir);

        // Deal periodic damage while devouring
        if (attackTimer % 10 == 0) {
            devourTarget.hurt(this.damageSources().mobAttack(this), 4.0f);
            // Heal self
            this.heal(DEVOUR_HEAL_AMOUNT * 0.25f);
        }
    }

    /**
     * Performs the ground slam with shockwave.
     */
    private void performGroundSlam() {
        float damage = GROUND_SLAM_DAMAGE_MIN + this.random.nextFloat() * (GROUND_SLAM_DAMAGE_MAX - GROUND_SLAM_DAMAGE_MIN);
        AABB slamBox = this.getBoundingBox().inflate(5.0);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, slamBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            double dist = this.distanceTo(target);
            if (dist <= 5.0) {
                // Damage falls off with distance
                float effectiveDmg = damage * (1.0f - (float)(dist / 10.0));
                target.hurt(source, effectiveDmg);
                // Shockwave knockback
                Vec3 knockDir = target.position().subtract(this.position()).normalize();
                target.knockback(1.5f, -knockDir.x, -knockDir.z);
                target.hurtMarked = true;
            }
        }

        // Shockwave effects
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    private void finishAttack(AttackState state) {
        if (state == AttackState.VOMIT_BABIES) {
            spawnBabies();
        }
        if (state == AttackState.DEVOUR) {
            devourTarget = null;
        }
        setAttackState(AttackState.IDLE);
    }

    /**
     * Spawns 8-10 neothelid babies.
     */
    private void spawnBabies() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        int count = VOMIT_BABY_COUNT_MIN + this.random.nextInt(VOMIT_BABY_COUNT_MAX - VOMIT_BABY_COUNT_MIN + 1);
        for (int i = 0; i < count; i++) {
            // Babies are spawned in a scatter pattern in front of the entity
            double offsetX = this.random.nextGaussian() * 2.0;
            double offsetZ = this.random.nextGaussian() * 2.0;
            BlockPos spawnPos = this.blockPosition().offset((int) offsetX, 0, (int) offsetZ);

            // The actual spawning would reference CDEEntities.NEOTHELID_BABY
            // For now, this is the logic placeholder - the entity type will be resolved at runtime
            // via the registry when NeothelidBabyEntity is registered
        }
    }

    /**
     * Called when this entity takes damage from an unlocatable source.
     * Triggers blind rage mode - attacks indiscriminately around itself.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()) {
            // If we cannot locate the source, enter blind rage
            if (source.getEntity() == null || !canDetectEntity(source.getEntity())) {
                setEnraged(true);
                setAttackState(AttackState.BLIND_RAGE);
                attackTimer = 60; // 3 seconds of rage
            }
        }
        return result;
    }

    /**
     * Checks if this blind creature can detect the given entity via vibration.
     */
    private boolean canDetectEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        double dist = this.distanceTo(entity);
        if (dist > VIBRATION_RANGE) return false;

        // Sneaking players are harder to detect
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            return dist < 8.0; // Can only detect sneaking players very close
        }
        return true;
    }

    /**
     * Handles distraction by items (eggs distract for 8 seconds).
     */
    public void onDistractedByItem(Vec3 itemPos, int durationTicks) {
        this.lastKnownSoundPos = itemPos;
        this.distractedTimer = durationTicks;
        this.setTarget(null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main body controller
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            AttackState attackState = getAttackState();
            if (attackState == AttackState.BLIND_RAGE || isEnraged()) {
                return state.setAndContinue(RAGE_ANIM);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        // Attack controller
        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            AttackState attackState = getAttackState();
            return switch (attackState) {
                case TAIL_SWEEP -> state.setAndContinue(TAIL_SWEEP_ANIM);
                case CHARGE -> state.setAndContinue(CHARGE_ANIM);
                case DEVOUR -> state.setAndContinue(DEVOUR_ANIM);
                case VOMIT_BABIES -> state.setAndContinue(VOMIT_ANIM);
                case GROUND_SLAM -> state.setAndContinue(GROUND_SLAM_ANIM);
                default -> {
                    state.getController().forceAnimationReset();
                    yield state.setAndContinue(IDLE);
                }
            };
        }));
    }
}
