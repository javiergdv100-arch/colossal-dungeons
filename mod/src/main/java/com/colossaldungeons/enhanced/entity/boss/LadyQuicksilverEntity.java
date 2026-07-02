package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * Lady of Quicksilver - Mirror Castle miniboss. Mercury witch.
 *
 * A witch whose body is living mercury. Converts floors to liquid mirrors,
 * summons mercury hands from reflective surfaces, and is vulnerable only when
 * her true reflection is exposed by breaking the correct surface.
 *
 * Stats: 180 HP, 3 armor, 11 damage.
 * Size: 0.6 x 1.9 blocks.
 *
 * Behavior:
 * - Converts floor to mirror: stepping on it teleports players to another room.
 * - Summons mercury hands that grab and immobilize players.
 * - Only vulnerable when her true reflection is exposed by a surface break.
 * - Powder snow freezes the mercury floor preventing conversion.
 * - Flint and steel hardens mercury hands making them brittle.
 * - Water bucket dilutes mercury floor making it inert temporarily.
 * - Snowballs break the surface where her real reflection appears.
 *
 * Implements IBossPhase for phase management.
 *
 * Drops: Mercury Heart (relic), Polished Silver Armor recipe.
 */
public class LadyQuicksilverEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Boss phase enumeration.
     */
    public enum LadyPhase {
        ENTRANCE,          // Dramatic emergence from mercury pool
        PHASE_1,           // Floor conversion + mercury hands
        PHASE_2_EXPOSED,   // True reflection partially visible, more aggressive
        PHASE_3_DESPERATE, // Low HP - all surfaces are mirrors, frenzied attacks
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(LadyQuicksilverEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_VULNERABLE =
        SynchedEntityData.defineId(LadyQuicksilverEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> MERCURY_HANDS_ACTIVE =
        SynchedEntityData.defineId(LadyQuicksilverEntity.class, EntityDataSerializers.INT);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.6f;
    private static final float PHASE_3_THRESHOLD = 0.3f;

    // Ability parameters
    private static final double FLOOR_CONVERSION_RADIUS = 8.0;
    private static final int HAND_SUMMON_COOLDOWN = 120; // 6 seconds
    private static final int FLOOR_CONVERT_COOLDOWN = 200; // 10 seconds
    private static final float HAND_GRAB_DAMAGE = 4.0f;
    private static final int HAND_IMMOBILIZE_DURATION = 40; // 2 seconds
    private static final int MAX_MERCURY_HANDS = 4;

    private final BossPhaseManager<LadyQuicksilverEntity> phaseManager;
    private int handCooldown = 0;
    private int floorCooldown = 0;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 60;

    // Animations
    private static final RawAnimation ENTRANCE_ANIM = RawAnimation.begin().thenPlay("animation.lady_quicksilver.entrance");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lady_quicksilver.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.lady_quicksilver.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.lady_quicksilver.attack");
    private static final RawAnimation SUMMON_HANDS = RawAnimation.begin().thenPlay("animation.lady_quicksilver.summon_hands");
    private static final RawAnimation FLOOR_CONVERT = RawAnimation.begin().thenPlay("animation.lady_quicksilver.floor_convert");
    private static final RawAnimation VULNERABLE_ANIM = RawAnimation.begin().thenLoop("animation.lady_quicksilver.vulnerable");
    private static final RawAnimation DESPERATE_IDLE = RawAnimation.begin().thenLoop("animation.lady_quicksilver.desperate");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.lady_quicksilver.death");

    public LadyQuicksilverEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(LadyPhase.PHASE_2_EXPOSED.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(LadyPhase.PHASE_3_DESPERATE.ordinal(), PHASE_3_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for Lady Quicksilver.
     * 180 HP, 3 armor, 11 damage, 0.3 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 180.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, LadyPhase.ENTRANCE.ordinal());
        builder.define(IS_VULNERABLE, false);
        builder.define(MERCURY_HANDS_ACTIVE, 0);
    }

    public LadyPhase getLadyPhase() {
        return LadyPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setLadyPhase(LadyPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isVulnerable() {
        return this.entityData.get(IS_VULNERABLE);
    }

    private void setVulnerable(boolean vulnerable) {
        this.entityData.set(IS_VULNERABLE, vulnerable);
    }

    @Override
    protected void registerGoals() {
        // AI managed via tick() for complex phase-based behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            LadyPhase currentPhase = getLadyPhase();

            // Decrement cooldowns
            if (handCooldown > 0) handCooldown--;
            if (floorCooldown > 0) floorCooldown--;

            switch (currentPhase) {
                case ENTRANCE -> {
                    transitionTimer++;
                    if (transitionTimer >= TRANSITION_DURATION) {
                        transitionToPhase(LadyPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1, PHASE_2_EXPOSED, PHASE_3_DESPERATE -> {
                    phaseManager.tick();
                    performCombatAI(currentPhase);
                }
                case DEATH -> {
                    // Death sequence
                }
            }
        }
    }

    /**
     * Performs phase-specific combat AI.
     */
    private void performCombatAI(LadyPhase phase) {
        Player target = this.level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        // Summon mercury hands
        if (handCooldown <= 0 && this.entityData.get(MERCURY_HANDS_ACTIVE) < MAX_MERCURY_HANDS) {
            summonMercuryHands(target);
            handCooldown = HAND_SUMMON_COOLDOWN;
            if (phase == LadyPhase.PHASE_3_DESPERATE) {
                handCooldown /= 2; // Faster in desperate phase
            }
        }

        // Convert floor to mirror
        if (floorCooldown <= 0) {
            convertFloorToMirror();
            floorCooldown = FLOOR_CONVERT_COOLDOWN;
            if (phase == LadyPhase.PHASE_3_DESPERATE) {
                floorCooldown /= 2;
            }
        }

        // Move toward target for melee
        if (this.distanceTo(target) < 3.0) {
            this.doHurtTarget(target);
        }
    }

    /**
     * Summons mercury hands from nearby reflective surfaces.
     */
    private void summonMercuryHands(Player target) {
        int handsToSummon = getLadyPhase() == LadyPhase.PHASE_3_DESPERATE ? 3 : 2;
        int active = this.entityData.get(MERCURY_HANDS_ACTIVE);
        this.entityData.set(MERCURY_HANDS_ACTIVE, Math.min(active + handsToSummon, MAX_MERCURY_HANDS));

        // Apply immobilization to target if close to a mirror surface
        if (isNearReflectiveSurface(target.blockPosition())) {
            target.hurt(this.damageSources().mobAttack(this), HAND_GRAB_DAMAGE);
            target.setDeltaMovement(Vec3.ZERO);
            // Slowness simulates immobilization
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                HAND_IMMOBILIZE_DURATION, 3));
        }
    }

    /**
     * Converts the floor around the entity to mirror surface.
     */
    private void convertFloorToMirror() {
        // In the full implementation, this would place mirror blocks below
        // For now, it creates a hazardous zone
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /**
     * Checks if a position is near a reflective surface.
     */
    private boolean isNearReflectiveSurface(BlockPos pos) {
        return this.level().getBlockStates(new AABB(pos).inflate(2.0))
            .anyMatch(state -> state.is(net.minecraft.tags.BlockTags.IMPERMEABLE));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only take full damage when vulnerable (true reflection exposed)
        if (!isVulnerable()) {
            // Reduced damage when reflection is hidden
            amount *= 0.25f;
        }
        return super.hurt(source, amount);
    }

    /**
     * Called when the mirror showing her true reflection is broken.
     */
    public void onTrueReflectionExposed() {
        setVulnerable(true);
        // Vulnerability window
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getLadyPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        LadyPhase newPhase = LadyPhase.values()[phase];
        LadyPhase oldPhase = getLadyPhase();
        if (newPhase == oldPhase) return;

        setLadyPhase(newPhase);
        transitionTimer = 0;

        // Phase-specific setup
        switch (newPhase) {
            case PHASE_2_EXPOSED -> {
                // More aggressive, true reflection partially visible
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
            }
            case PHASE_3_DESPERATE -> {
                // All surfaces become mirrors, frenzied
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(14.0);
            }
            default -> {}
        }

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
        return switch (LadyPhase.values()[phase]) {
            case PHASE_2_EXPOSED -> PHASE_2_THRESHOLD;
            case PHASE_3_DESPERATE -> PHASE_3_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (LadyPhase.values()[phase]) {
            case PHASE_1 -> List.of("melee", "summon_hands", "floor_convert");
            case PHASE_2_EXPOSED -> List.of("melee", "summon_hands", "floor_convert", "mercury_wave");
            case PHASE_3_DESPERATE -> List.of("melee", "summon_hands", "floor_convert", "mercury_wave", "full_mirror");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getLadyPhase() != LadyPhase.DEATH) {
            transitionToPhase(LadyPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 60 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            LadyPhase phase = getLadyPhase();
            return switch (phase) {
                case ENTRANCE -> state.setAndContinue(ENTRANCE_ANIM);
                case PHASE_1 -> {
                    if (isVulnerable()) yield state.setAndContinue(VULNERABLE_ANIM);
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case PHASE_2_EXPOSED -> {
                    if (isVulnerable()) yield state.setAndContinue(VULNERABLE_ANIM);
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case PHASE_3_DESPERATE -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(DESPERATE_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "abilities", 5, state -> {
            if (handCooldown == HAND_SUMMON_COOLDOWN - 1) {
                return state.setAndContinue(SUMMON_HANDS);
            }
            if (floorCooldown == FLOOR_CONVERT_COOLDOWN - 1) {
                return state.setAndContinue(FLOOR_CONVERT);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
