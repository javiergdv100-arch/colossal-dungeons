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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * The Dying Atlas - a multi-phase boss entity following section 7.5 of the technical plan.
 *
 * A colossal, crumbling stone titan that awakens when players enter its chamber.
 * Phases transition based on health thresholds with distinct attack patterns per phase.
 *
 * BossPhase progression: DORMANT -> AWAKENING -> PHASE_1 -> TRANSITION_1_2 -> PHASE_2
 *                        -> TRANSITION_2_3 -> PHASE_3 -> DEATH
 *
 * Stats: 500 HP, 0.15 speed, 20 damage, 1.0 knockback resistance.
 */
public class DyingAtlasEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phase enumeration following the technical plan exactly.
     */
    public enum BossPhase {
        DORMANT,
        AWAKENING,
        PHASE_1,
        TRANSITION_1_2,
        PHASE_2,
        TRANSITION_2_3,
        PHASE_3,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(DyingAtlasEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds (fraction of max health)
    private static final float PHASE_2_THRESHOLD = 0.60f;
    private static final float PHASE_3_THRESHOLD = 0.25f;

    private final BossPhaseManager<DyingAtlasEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 60; // 3 seconds
    private static final int AWAKENING_DURATION = 100; // 5 seconds

    // ========== Animations ==========
    private static final RawAnimation DORMANT_IDLE = RawAnimation.begin().thenLoop("animation.dying_atlas.dormant");
    private static final RawAnimation AWAKEN = RawAnimation.begin().thenPlay("animation.dying_atlas.awaken");
    private static final RawAnimation PHASE1_IDLE = RawAnimation.begin().thenLoop("animation.dying_atlas.phase1_idle");
    private static final RawAnimation PHASE1_WALK = RawAnimation.begin().thenLoop("animation.dying_atlas.phase1_walk");
    private static final RawAnimation PHASE1_SLAM = RawAnimation.begin().thenPlay("animation.dying_atlas.phase1_slam");
    private static final RawAnimation TRANSITION_12 = RawAnimation.begin().thenPlay("animation.dying_atlas.transition_12");
    private static final RawAnimation PHASE2_IDLE = RawAnimation.begin().thenLoop("animation.dying_atlas.phase2_idle");
    private static final RawAnimation PHASE2_WALK = RawAnimation.begin().thenLoop("animation.dying_atlas.phase2_walk");
    private static final RawAnimation PHASE2_SWEEP = RawAnimation.begin().thenPlay("animation.dying_atlas.phase2_sweep");
    private static final RawAnimation TRANSITION_23 = RawAnimation.begin().thenPlay("animation.dying_atlas.transition_23");
    private static final RawAnimation PHASE3_IDLE = RawAnimation.begin().thenLoop("animation.dying_atlas.phase3_idle");
    private static final RawAnimation PHASE3_WALK = RawAnimation.begin().thenLoop("animation.dying_atlas.phase3_walk");
    private static final RawAnimation PHASE3_FRENZY = RawAnimation.begin().thenPlay("animation.dying_atlas.phase3_frenzy");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.dying_atlas.death");

    public DyingAtlasEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(BossPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(BossPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Dying Atlas.
     * 500 HP, 0.15 speed, 20 damage, full knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.15)
            .add(Attributes.ATTACK_DAMAGE, 20.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, BossPhase.DORMANT.ordinal());
    }

    @Override
    protected void registerGoals() {
        // Goals are managed by the brain/AI system rather than vanilla goals
        // See DyingAtlasBrainProvider for SmartBrainLib integration
    }

    public BossPhase getBossPhase() {
        return BossPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setBossPhase(BossPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            BossPhase currentPhase = getBossPhase();

            switch (currentPhase) {
                case DORMANT -> {
                    // Check for nearby players to trigger awakening
                    Player nearest = this.level().getNearestPlayer(this, 12.0);
                    if (nearest != null) {
                        transitionToPhase(BossPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1, PHASE_2, PHASE_3 -> {
                    // Let the phase manager handle health-based transitions
                    phaseManager.tick();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_2.ordinal());
                    }
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(BossPhase.PHASE_3.ordinal());
                    }
                }
                case DEATH -> {
                    // Death animation playing - entity will be removed after
                }
            }
        }
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getBossPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        BossPhase newPhase = BossPhase.values()[phase];
        BossPhase oldPhase = getBossPhase();

        if (newPhase == oldPhase) return;

        setBossPhase(newPhase);

        // Set transition timers
        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3 -> transitionTimer = TRANSITION_DURATION;
            default -> transitionTimer = 0;
        }

        // Play phase change sound
        this.level().playSound(null, this.blockPosition(),
            CDESounds.BOSS_PHASE_CHANGE.get(), SoundSource.HOSTILE, 2.0f, 1.0f);

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
        return switch (BossPhase.values()[phase]) {
            case PHASE_2, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (BossPhase.values()[phase]) {
            case PHASE_1 -> List.of("slam", "stomp");
            case PHASE_2 -> List.of("slam", "sweep", "ground_pound");
            case PHASE_3 -> List.of("slam", "sweep", "ground_pound", "frenzy", "debris_rain");
            default -> List.of();
        };
    }

    // ========== GeckoLib Animation ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main body controller - phase-based animations
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            BossPhase phase = getBossPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_IDLE);
                case AWAKENING -> state.setAndContinue(AWAKEN);
                case PHASE_1 -> {
                    if (state.isMoving()) yield state.setAndContinue(PHASE1_WALK);
                    yield state.setAndContinue(PHASE1_IDLE);
                }
                case TRANSITION_1_2 -> state.setAndContinue(TRANSITION_12);
                case PHASE_2 -> {
                    if (state.isMoving()) yield state.setAndContinue(PHASE2_WALK);
                    yield state.setAndContinue(PHASE2_IDLE);
                }
                case TRANSITION_2_3 -> state.setAndContinue(TRANSITION_23);
                case PHASE_3 -> {
                    if (state.isMoving()) yield state.setAndContinue(PHASE3_WALK);
                    yield state.setAndContinue(PHASE3_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }).setSoundKeyframeHandler(event -> {
            // Sound keyframe handler for "slam_impact"
            if ("slam_impact".equals(event.getKeyframeData().getSound())) {
                this.level().playSound(null, this.blockPosition(),
                    CDESounds.BOSS_PHASE_CHANGE.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
            }
        }));

        // Attack controller - handles attack animations overlaid on body
        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            BossPhase phase = getBossPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1 -> state.setAndContinue(PHASE1_SLAM);
                    case PHASE_2 -> state.setAndContinue(PHASE2_SWEEP);
                    case PHASE_3 -> state.setAndContinue(PHASE3_FRENZY);
                    default -> state.setAndContinue(PHASE1_SLAM);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(PHASE1_IDLE);
        }));
    }

    @Override
    protected void tickDeath() {
        if (getBossPhase() != BossPhase.DEATH) {
            transitionToPhase(BossPhase.DEATH.ordinal());
        }
        // Allow death animation to play for a few seconds before removal
        ++this.deathTime;
        if (this.deathTime >= 80 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }
}
