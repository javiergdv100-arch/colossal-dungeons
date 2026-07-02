package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.UUID;

/**
 * The Broken Twins - Mirror Castle miniboss. Two specular knights sharing a damage pool.
 *
 * Two mirrored knights that share a single health pool (2 x 120 HP total = 240 HP shared).
 * Damaging one damages the other. They only die if both fall nearly simultaneously.
 * One attacks from the front (real) and the other from the reflection; they swap roles
 * when the room rotates.
 *
 * Stats: 2 x 120 HP (shared damage pool), 6 armor, 12 damage.
 * Size: 0.8 x 2.2 blocks each (armored knight).
 *
 * Behavior:
 * - Shared health pool: damaging one hurts both proportionally.
 * - Only die if both are brought to zero nearly simultaneously (within 3 seconds).
 * - One is "real" and one is "reflection" - swap periodically.
 * - Hitting the reflection (wrong target) reflects damage to attacker (Law of Reflection).
 * - Spyglass reveals which is real (subtle armor detail differences).
 * - Water bucket dampens floor, slowing them and revealing the real one by footprints.
 * - Snowballs thrown at both simultaneously: the real one reacts first.
 *
 * Implements IBossPhase for phase management.
 *
 * Drops: Pair of mirrored blades, Inverted throne fragment.
 */
public class BrokenTwinsEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Phase states for the Broken Twins encounter.
     */
    public enum TwinPhase {
        ENTRANCE,          // Dramatic entrance
        PHASE_1,           // Normal combat, swap roles periodically
        PHASE_2_ENRAGED,   // Below 50% HP - faster swaps, more aggressive
        DEATH_SYNC         // Both must die within a window
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(BrokenTwinsEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_REAL =
        SynchedEntityData.defineId(BrokenTwinsEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> TWIN_ALIVE =
        SynchedEntityData.defineId(BrokenTwinsEntity.class, EntityDataSerializers.BOOLEAN);

    // Phase thresholds
    private static final float PHASE_2_THRESHOLD = 0.5f;

    // Twin linkage
    private UUID linkedTwinUUID = null;
    private BrokenTwinsEntity linkedTwin = null;
    private static final int SWAP_INTERVAL_PHASE1 = 200; // 10 seconds
    private static final int SWAP_INTERVAL_PHASE2 = 100; // 5 seconds (enraged)
    private static final int DEATH_SYNC_WINDOW = 60; // 3 seconds to kill both

    private final BossPhaseManager<BrokenTwinsEntity> phaseManager;
    private int swapTimer = 0;
    private int deathWindowTimer = 0;
    private boolean deathWindowActive = false;

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.broken_twins.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.broken_twins.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.broken_twins.attack");
    private static final RawAnimation SWAP_ANIM = RawAnimation.begin().thenPlay("animation.broken_twins.swap");
    private static final RawAnimation ENTRANCE_ANIM = RawAnimation.begin().thenPlay("animation.broken_twins.entrance");
    private static final RawAnimation ENRAGED_IDLE = RawAnimation.begin().thenLoop("animation.broken_twins.enraged_idle");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.broken_twins.death");

    public BrokenTwinsEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(TwinPhase.PHASE_2_ENRAGED.ordinal(), PHASE_2_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Broken Twins.
     * 120 HP each (shared pool of 240), 6 armor, 12 damage, 0.28 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, TwinPhase.ENTRANCE.ordinal());
        builder.define(IS_REAL, true);
        builder.define(TWIN_ALIVE, true);
    }

    public TwinPhase getTwinPhase() {
        return TwinPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setTwinPhase(TwinPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isReal() {
        return this.entityData.get(IS_REAL);
    }

    private void setReal(boolean real) {
        this.entityData.set(IS_REAL, real);
    }

    /**
     * Links this twin to its counterpart.
     */
    public void linkTwin(BrokenTwinsEntity twin) {
        this.linkedTwin = twin;
        this.linkedTwinUUID = twin.getUUID();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            TwinPhase currentPhase = getTwinPhase();

            switch (currentPhase) {
                case ENTRANCE -> {
                    // Brief entrance animation then start fight
                    swapTimer++;
                    if (swapTimer >= 60) {
                        transitionToPhase(TwinPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1, PHASE_2_ENRAGED -> {
                    // Phase manager checks health thresholds
                    phaseManager.tick();

                    // Role swap timer
                    int swapInterval = currentPhase == TwinPhase.PHASE_2_ENRAGED
                        ? SWAP_INTERVAL_PHASE2 : SWAP_INTERVAL_PHASE1;
                    swapTimer++;
                    if (swapTimer >= swapInterval) {
                        performRoleSwap();
                        swapTimer = 0;
                    }
                }
                case DEATH_SYNC -> {
                    if (deathWindowActive) {
                        deathWindowTimer--;
                        if (deathWindowTimer <= 0) {
                            // Window expired - if twin still alive, revive this one
                            if (linkedTwin != null && !linkedTwin.isDeadOrDying()) {
                                this.setHealth(this.getMaxHealth() * 0.1f);
                                setTwinPhase(TwinPhase.PHASE_2_ENRAGED);
                                deathWindowActive = false;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Swaps real/reflection roles between twins.
     */
    private void performRoleSwap() {
        boolean currentlyReal = isReal();
        setReal(!currentlyReal);
        if (linkedTwin != null) {
            linkedTwin.setReal(currentlyReal);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // If hit while being the "reflection" (not real), reflect damage
        if (!isReal() && source.getEntity() instanceof LivingEntity attacker) {
            // Law of Reflection: damage reflected to attacker
            attacker.hurt(this.damageSources().magic(), amount * 0.5f);
            return false; // Reflection takes no damage
        }

        boolean result = super.hurt(source, amount);

        // Shared damage pool: forward proportional damage to twin
        if (result && linkedTwin != null && !linkedTwin.isDeadOrDying()) {
            linkedTwin.hurt(this.damageSources().generic(), amount * 0.5f);
        }

        // Check death synchronization
        if (this.getHealth() <= 0 && result) {
            if (linkedTwin != null && linkedTwin.getHealth() > 0) {
                // Start death sync window
                setTwinPhase(TwinPhase.DEATH_SYNC);
                deathWindowActive = true;
                deathWindowTimer = DEATH_SYNC_WINDOW;
                this.setHealth(1.0f); // Keep alive during window
            }
        }

        return result;
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getTwinPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        TwinPhase newPhase = TwinPhase.values()[phase];
        TwinPhase oldPhase = getTwinPhase();
        if (newPhase == oldPhase) return;

        setTwinPhase(newPhase);
        swapTimer = 0;

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
        return switch (TwinPhase.values()[phase]) {
            case PHASE_2_ENRAGED -> PHASE_2_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (TwinPhase.values()[phase]) {
            case PHASE_1 -> List.of("melee_slash", "mirror_swap");
            case PHASE_2_ENRAGED -> List.of("melee_slash", "mirror_swap", "dual_strike", "fast_swap");
            default -> List.of();
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            TwinPhase phase = getTwinPhase();
            return switch (phase) {
                case ENTRANCE -> state.setAndContinue(ENTRANCE_ANIM);
                case PHASE_1 -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case PHASE_2_ENRAGED -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(ENRAGED_IDLE);
                }
                case DEATH_SYNC -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
