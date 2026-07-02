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
 * The Shattered Sovereign - Mirror Castle final boss.
 *
 * A king composed of a thousand suspended mirror shards that recompose into different
 * forms. Can only truly die when all the great reflective surfaces sustaining his
 * reflection in the Inverted Throne Room are destroyed one by one.
 *
 * Stats: 1200 HP (phased), 8 armor, 14-20 damage.
 * Size: 1.2 x 2.8 blocks (tall crowned figure of floating shards).
 *
 * Phase 1 - Mirrored Knight:
 * - Combat as an armored knight; frontal hits reflect damage back.
 * - Shield is essential to survive reflected damage.
 * - Must find and exploit gaps in the mirror armor.
 *
 * Phase 2 - Fragmented:
 * - Shatters into multiple reflections attacking from different mirrors.
 * - Must identify and break the anchor mirror (spyglass reveals it).
 * - Snowballs break anchor mirrors from safe distance.
 *
 * Phase 3 - Inverted Room:
 * - Inverts the room (ceiling becomes floor).
 * - Projects hostile reflections of the player.
 * - Water bucket dampens surfaces revealing true room orientation.
 * - Each destroyed mirror reduces armor and exposes true form.
 *
 * Each great mirror destroyed reduces armor by 2 and exposes his real form.
 *
 * Implements IBossPhase with BossPhaseManager and full phase transition logic.
 *
 * Drops: Shard Crown (unique helmet), Inverted Throne Core (legendary material),
 *        Pure mercury fragment.
 */
public class ShatteredSovereignEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phase enumeration for the Shattered Sovereign.
     */
    public enum SovereignPhase {
        DORMANT,           // Waiting for player approach
        AWAKENING,         // Shards assembling into form
        PHASE_1_KNIGHT,    // Mirror knight combat
        TRANSITION_1_2,    // Shattering into fragments
        PHASE_2_FRAGMENTED,// Multiple reflections attack
        TRANSITION_2_3,    // Room inversion beginning
        PHASE_3_INVERTED,  // Inverted room with hostile reflections
        DEATH              // Final shattering
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(ShatteredSovereignEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MIRRORS_DESTROYED =
        SynchedEntityData.defineId(ShatteredSovereignEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_REFLECTING =
        SynchedEntityData.defineId(ShatteredSovereignEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> ACTIVE_REFLECTIONS =
        SynchedEntityData.defineId(ShatteredSovereignEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.65f;
    private static final float PHASE_3_THRESHOLD = 0.30f;

    // Combat parameters
    private static final float PHASE_1_DAMAGE = 14.0f;
    private static final float PHASE_2_DAMAGE = 16.0f;
    private static final float PHASE_3_DAMAGE = 20.0f;
    private static final float REFLECT_DAMAGE_MULTIPLIER = 0.75f;
    private static final double REFLECTION_ATTACK_RANGE = 6.0;
    private static final int MAX_MIRRORS = 6;
    private static final float ARMOR_REDUCTION_PER_MIRROR = 2.0f;

    // Phase 2 specifics
    private static final int PHASE_2_REFLECTION_COUNT = 4;
    private static final int ANCHOR_MIRROR_SNOWBALL_HITS = 3;

    // Phase 3 specifics
    private static final int HOSTILE_REFLECTION_DAMAGE = 8;
    private static final int ROOM_INVERT_DURATION = 200; // 10 seconds per cycle

    private final BossPhaseManager<ShatteredSovereignEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 80; // 4 seconds
    private static final int AWAKENING_DURATION = 120; // 6 seconds

    // Phase 1 state
    private int reflectCooldown = 0;
    private static final int REFLECT_COOLDOWN_TICKS = 40;

    // Phase 2 state
    private int anchorMirrorHits = 0;
    private BlockPos anchorMirrorPos = null;
    private final List<Vec3> activeReflectionPositions = new ArrayList<>();

    // Phase 3 state
    private int invertCycleTimer = 0;
    private boolean roomInverted = false;
    private int hostileReflectionSpawnCooldown = 0;

    // Attack cooldowns
    private int attackCooldown = 0;
    private int specialAttackCooldown = 0;

    // ========== Animations ==========
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.shattered_sovereign.dormant");
    private static final RawAnimation AWAKEN_ANIM = RawAnimation.begin().thenPlay("animation.shattered_sovereign.awaken");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.shattered_sovereign.phase1_idle");
    private static final RawAnimation P1_WALK = RawAnimation.begin().thenLoop("animation.shattered_sovereign.phase1_walk");
    private static final RawAnimation P1_SLASH = RawAnimation.begin().thenPlay("animation.shattered_sovereign.phase1_slash");
    private static final RawAnimation P1_REFLECT = RawAnimation.begin().thenPlay("animation.shattered_sovereign.phase1_reflect");
    private static final RawAnimation TRANSITION_12_ANIM = RawAnimation.begin().thenPlay("animation.shattered_sovereign.transition_12");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.shattered_sovereign.phase2_idle");
    private static final RawAnimation P2_FRAGMENT_ATTACK = RawAnimation.begin().thenPlay("animation.shattered_sovereign.phase2_fragment");
    private static final RawAnimation TRANSITION_23_ANIM = RawAnimation.begin().thenPlay("animation.shattered_sovereign.transition_23");
    private static final RawAnimation P3_IDLE = RawAnimation.begin().thenLoop("animation.shattered_sovereign.phase3_idle");
    private static final RawAnimation P3_WALK = RawAnimation.begin().thenLoop("animation.shattered_sovereign.phase3_walk");
    private static final RawAnimation P3_INVERT = RawAnimation.begin().thenPlay("animation.shattered_sovereign.phase3_invert");
    private static final RawAnimation P3_SUMMON_REFLECTIONS = RawAnimation.begin().thenPlay("animation.shattered_sovereign.phase3_summon");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.shattered_sovereign.death");

    public ShatteredSovereignEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(SovereignPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(SovereignPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Shattered Sovereign.
     * 1200 HP, 8 armor, 14 base damage (scales by phase), 0.22 speed, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, SovereignPhase.DORMANT.ordinal());
        builder.define(MIRRORS_DESTROYED, 0);
        builder.define(IS_REFLECTING, false);
        builder.define(ACTIVE_REFLECTIONS, 0);
    }

    public SovereignPhase getSovereignPhase() {
        return SovereignPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setSovereignPhase(SovereignPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public int getMirrorsDestroyed() {
        return this.entityData.get(MIRRORS_DESTROYED);
    }

    public boolean isReflecting() {
        return this.entityData.get(IS_REFLECTING);
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
            if (attackCooldown > 0) attackCooldown--;
            if (specialAttackCooldown > 0) specialAttackCooldown--;
            if (reflectCooldown > 0) reflectCooldown--;

            SovereignPhase currentPhase = getSovereignPhase();

            switch (currentPhase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 16.0);
                    if (nearest != null) {
                        transitionToPhase(SovereignPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SovereignPhase.PHASE_1_KNIGHT.ordinal());
                    }
                }
                case PHASE_1_KNIGHT -> {
                    phaseManager.tick();
                    tickPhase1();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SovereignPhase.PHASE_2_FRAGMENTED.ordinal());
                    }
                }
                case PHASE_2_FRAGMENTED -> {
                    phaseManager.tick();
                    tickPhase2();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(SovereignPhase.PHASE_3_INVERTED.ordinal());
                    }
                }
                case PHASE_3_INVERTED -> {
                    tickPhase3();
                }
                case DEATH -> {
                    // Death animation playing
                }
            }
        }
    }

    // ========== Phase 1: Mirror Knight Combat ==========

    /**
     * Phase 1 tick: Knight combat with damage reflection.
     */
    private void tickPhase1() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        double distance = this.distanceTo(target);

        // Enable/disable reflection shield
        if (reflectCooldown <= 0 && distance < 6.0) {
            this.entityData.set(IS_REFLECTING, true);
        }

        // Melee attack
        if (distance < 3.0 && attackCooldown <= 0) {
            this.doHurtTarget(target);
            attackCooldown = 30;
        }

        // Move toward target
        if (distance > 3.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.15).add(0, this.getDeltaMovement().y, 0));
        }
    }

    // ========== Phase 2: Fragmented Reflections ==========

    /**
     * Phase 2 tick: Multiple fragment attacks from mirrors.
     */
    private void tickPhase2() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Periodically attack from reflection positions
        if (specialAttackCooldown <= 0) {
            performFragmentAttack(target);
            specialAttackCooldown = 80;
        }

        // Manage active reflections
        int reflections = this.entityData.get(ACTIVE_REFLECTIONS);
        if (reflections < PHASE_2_REFLECTION_COUNT) {
            spawnReflection();
        }
    }

    /**
     * Performs a fragment attack from multiple positions simultaneously.
     */
    private void performFragmentAttack(Player target) {
        DamageSource source = this.damageSources().mobAttack(this);
        for (Vec3 reflectionPos : activeReflectionPositions) {
            double dist = target.position().distanceTo(reflectionPos);
            if (dist <= REFLECTION_ATTACK_RANGE) {
                target.hurt(source, PHASE_2_DAMAGE);
                break; // Only one reflection hits per cycle
            }
        }

        // Particles from all reflection positions
        if (this.level() instanceof ServerLevel serverLevel) {
            for (Vec3 pos : activeReflectionPositions) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 1.0, pos.z, 5, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    /**
     * Spawns a new reflection at a random mirror position.
     */
    private void spawnReflection() {
        // Place reflections at random positions around the room
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double distance = 6.0 + this.random.nextDouble() * 6.0;
        Vec3 pos = this.position().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        activeReflectionPositions.add(pos);
        this.entityData.set(ACTIVE_REFLECTIONS, activeReflectionPositions.size());
    }

    /**
     * Called when a mirror in the room is hit by a snowball (anchor mirror check).
     */
    public void onMirrorHit(BlockPos mirrorPos) {
        if (getSovereignPhase() == SovereignPhase.PHASE_2_FRAGMENTED) {
            if (mirrorPos.equals(anchorMirrorPos)) {
                anchorMirrorHits++;
                if (anchorMirrorHits >= ANCHOR_MIRROR_SNOWBALL_HITS) {
                    destroyAnchorMirror();
                }
            }
        } else {
            // Any phase: destroying mirrors reduces armor
            destroyGreatMirror();
        }
    }

    /**
     * Destroys the anchor mirror, clearing phase 2 reflections.
     */
    private void destroyAnchorMirror() {
        activeReflectionPositions.clear();
        this.entityData.set(ACTIVE_REFLECTIONS, 0);
        anchorMirrorHits = 0;
        destroyGreatMirror();

        // Sound effect
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /**
     * Destroys a great mirror surface, reducing the Sovereign's armor.
     */
    private void destroyGreatMirror() {
        int destroyed = this.entityData.get(MIRRORS_DESTROYED) + 1;
        this.entityData.set(MIRRORS_DESTROYED, destroyed);

        // Reduce armor
        float currentArmor = (float) this.getAttributeValue(Attributes.ARMOR);
        float newArmor = Math.max(0, currentArmor - ARMOR_REDUCTION_PER_MIRROR);
        this.getAttribute(Attributes.ARMOR).setBaseValue(newArmor);

        // Sound and particles
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5f, 0.7f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                this.getX(), this.getY() + 1.5, this.getZ(), 20, 1.0, 1.0, 1.0, 0.2);
        }
    }

    // ========== Phase 3: Inverted Room ==========

    /**
     * Phase 3 tick: Room inversion and hostile player reflections.
     */
    private void tickPhase3() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        // Cycle room inversion
        invertCycleTimer++;
        if (invertCycleTimer >= ROOM_INVERT_DURATION) {
            invertCycleTimer = 0;
            roomInverted = !roomInverted;
            // Play inversion sound
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.3f);
        }

        // Spawn hostile reflections of the player
        if (hostileReflectionSpawnCooldown <= 0) {
            spawnHostileReflection(target);
            hostileReflectionSpawnCooldown = 120; // 6 seconds
        }
        hostileReflectionSpawnCooldown--;

        // Direct melee with enhanced damage
        if (this.distanceTo(target) < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_3_DAMAGE);
            attackCooldown = 25;
        }

        // Move toward target
        double distance = this.distanceTo(target);
        if (distance > 3.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.18).add(0, this.getDeltaMovement().y, 0));
        }
    }

    /**
     * Spawns a hostile reflection that mimics and attacks the player.
     * In the full implementation, this would spawn ReflectionTwin entities.
     */
    private void spawnHostileReflection(Player target) {
        // Placeholder for spawning hostile reflection entities
        // These would be ReflectionTwin instances configured to copy the target player
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
        }
    }

    // ========== Damage Handling ==========

    @Override
    public boolean hurt(DamageSource source, float amount) {
        SovereignPhase phase = getSovereignPhase();

        // Phase 1: Reflect frontal damage
        if (phase == SovereignPhase.PHASE_1_KNIGHT && isReflecting()) {
            if (source.getEntity() instanceof LivingEntity attacker) {
                Vec3 attackDir = attacker.position().subtract(this.position()).normalize();
                Vec3 lookDir = this.getLookAngle();
                double dot = attackDir.dot(lookDir);

                // Frontal hit - reflect damage
                if (dot > 0.3) {
                    // Shield on attacker absorbs reflected damage
                    if (attacker instanceof Player player && player.isBlocking()) {
                        // Player's shield reflects the reflected damage back!
                        // This is how you damage the Sovereign in Phase 1
                        this.entityData.set(IS_REFLECTING, false);
                        reflectCooldown = REFLECT_COOLDOWN_TICKS;
                        return super.hurt(source, amount);
                    } else {
                        // No shield - reflect damage to attacker
                        attacker.hurt(this.damageSources().magic(), amount * REFLECT_DAMAGE_MULTIPLIER);
                        return false;
                    }
                }
            }
        }

        // Phase 2: Reduced damage unless anchor mirror is being targeted
        if (phase == SovereignPhase.PHASE_2_FRAGMENTED) {
            // Direct hits on the fragmented form do reduced damage
            amount *= 0.5f;
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getSovereignPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        SovereignPhase newPhase = SovereignPhase.values()[phase];
        SovereignPhase oldPhase = getSovereignPhase();

        if (newPhase == oldPhase) return;

        setSovereignPhase(newPhase);

        // Phase-specific setup
        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_1_KNIGHT -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_1_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
            }
            case PHASE_2_FRAGMENTED -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                // Select random anchor mirror position
                anchorMirrorPos = this.blockPosition().offset(
                    this.random.nextInt(10) - 5, 0, this.random.nextInt(10) - 5);
                anchorMirrorHits = 0;
                activeReflectionPositions.clear();
            }
            case PHASE_3_INVERTED -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_3_DAMAGE);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
                invertCycleTimer = 0;
                roomInverted = false;
            }
            default -> transitionTimer = 0;
        }

        // Play phase change sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 0.5f);

        // Network sync to all tracking players
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
        return switch (SovereignPhase.values()[phase]) {
            case PHASE_2_FRAGMENTED, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_INVERTED, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (SovereignPhase.values()[phase]) {
            case PHASE_1_KNIGHT -> List.of("slash", "reflect", "shield_bash");
            case PHASE_2_FRAGMENTED -> List.of("fragment_barrage", "mirror_dash", "shard_rain");
            case PHASE_3_INVERTED -> List.of("invert_room", "hostile_reflections", "shard_storm", "final_slash");
            default -> List.of();
        };
    }

    // ========== Death ==========

    @Override
    protected void tickDeath() {
        if (getSovereignPhase() != SovereignPhase.DEATH) {
            transitionToPhase(SovereignPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 100 && !this.level().isClientSide() && !this.isRemoved()) {
            // Extended death animation for dramatic shattering
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ========== GeckoLib Animation ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main body controller - phase-based animations
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            SovereignPhase phase = getSovereignPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case AWAKENING -> state.setAndContinue(AWAKEN_ANIM);
                case PHASE_1_KNIGHT -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_WALK);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION_1_2 -> state.setAndContinue(TRANSITION_12_ANIM);
                case PHASE_2_FRAGMENTED -> state.setAndContinue(P2_IDLE);
                case TRANSITION_2_3 -> state.setAndContinue(TRANSITION_23_ANIM);
                case PHASE_3_INVERTED -> {
                    if (state.isMoving()) yield state.setAndContinue(P3_WALK);
                    yield state.setAndContinue(P3_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        // Attack controller - phase-specific attacks
        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            SovereignPhase phase = getSovereignPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_KNIGHT -> {
                        if (isReflecting()) yield state.setAndContinue(P1_REFLECT);
                        yield state.setAndContinue(P1_SLASH);
                    }
                    case PHASE_2_FRAGMENTED -> state.setAndContinue(P2_FRAGMENT_ATTACK);
                    case PHASE_3_INVERTED -> state.setAndContinue(P3_SUMMON_REFLECTIONS);
                    default -> state.setAndContinue(P1_SLASH);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));

        // Special ability controller
        controllers.add(new AnimationController<>(this, "special", 5, state -> {
            if (getSovereignPhase() == SovereignPhase.PHASE_3_INVERTED && roomInverted) {
                return state.setAndContinue(P3_INVERT);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
