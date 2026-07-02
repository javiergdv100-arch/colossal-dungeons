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
 * Primordial Concord - The final boss of the Primordial Tower.
 *
 * A 4-phase elemental council that transitions through Earth, Water, Air, and Fire.
 * Phase 1 (Earth): traps with columns, crushes.
 * Phase 2 (Water): floods arena, drags with currents.
 * Phase 3 (Air): reduces arena to floating platforms, pushes.
 * Phase 4 (Fire): ignites floor and oil, culminates with genesis lightning.
 * Each phase requires specific vanilla item counter.
 *
 * Implements IBossPhase with BossPhaseManager, 4 thresholds (75%/50%/25%).
 *
 * Stats: 1500 HP, 8 armor, 0.18 speed, 16-22 damage.
 */
public class PrimordialConcordEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    public enum ConcordPhase {
        DORMANT,
        AWAKENING,
        PHASE_1_EARTH,
        TRANSITION_1_2,
        PHASE_2_WATER,
        TRANSITION_2_3,
        PHASE_3_AIR,
        TRANSITION_3_4,
        PHASE_4_FIRE,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(PrimordialConcordEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_ENRAGED =
        SynchedEntityData.defineId(PrimordialConcordEntity.class, EntityDataSerializers.BOOLEAN);

    // Phase health thresholds
    private static final float PHASE_2_THRESHOLD = 0.75f;
    private static final float PHASE_3_THRESHOLD = 0.50f;
    private static final float PHASE_4_THRESHOLD = 0.25f;

    // Damage values per phase
    private static final float PHASE_1_DAMAGE = 16.0f;
    private static final float PHASE_2_DAMAGE = 18.0f;
    private static final float PHASE_3_DAMAGE = 18.0f;
    private static final float PHASE_4_DAMAGE = 22.0f;

    private final BossPhaseManager<PrimordialConcordEntity> phaseManager;
    private int transitionTimer = 0;
    private static final int TRANSITION_DURATION = 100; // 5 seconds
    private static final int AWAKENING_DURATION = 120; // 6 seconds

    // Attack state
    private int attackCooldown = 0;
    private int specialCooldown = 0;
    private int arenaCooldown = 0;

    // Phase 1 state - Earth
    private int columnTrapCooldown = 0;

    // Phase 2 state - Water
    private int currentPulseCooldown = 0;

    // Phase 3 state - Air
    private int pushCooldown = 0;

    // Phase 4 state - Fire
    private int genesisLightningCooldown = 0;
    private boolean oilIgnited = false;

    // ========== Animations ==========
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.primordial_concord.dormant");
    private static final RawAnimation AWAKEN_ANIM = RawAnimation.begin().thenPlay("animation.primordial_concord.awaken");
    private static final RawAnimation P1_IDLE = RawAnimation.begin().thenLoop("animation.primordial_concord.earth_idle");
    private static final RawAnimation P1_WALK = RawAnimation.begin().thenLoop("animation.primordial_concord.earth_walk");
    private static final RawAnimation P1_CRUSH = RawAnimation.begin().thenPlay("animation.primordial_concord.earth_crush");
    private static final RawAnimation P1_COLUMN = RawAnimation.begin().thenPlay("animation.primordial_concord.earth_column");
    private static final RawAnimation TRANS_12 = RawAnimation.begin().thenPlay("animation.primordial_concord.transition_12");
    private static final RawAnimation P2_IDLE = RawAnimation.begin().thenLoop("animation.primordial_concord.water_idle");
    private static final RawAnimation P2_WALK = RawAnimation.begin().thenLoop("animation.primordial_concord.water_walk");
    private static final RawAnimation P2_FLOOD = RawAnimation.begin().thenPlay("animation.primordial_concord.water_flood");
    private static final RawAnimation P2_DRAG = RawAnimation.begin().thenPlay("animation.primordial_concord.water_drag");
    private static final RawAnimation TRANS_23 = RawAnimation.begin().thenPlay("animation.primordial_concord.transition_23");
    private static final RawAnimation P3_IDLE = RawAnimation.begin().thenLoop("animation.primordial_concord.air_idle");
    private static final RawAnimation P3_WALK = RawAnimation.begin().thenLoop("animation.primordial_concord.air_walk");
    private static final RawAnimation P3_PUSH = RawAnimation.begin().thenPlay("animation.primordial_concord.air_push");
    private static final RawAnimation P3_LIFT = RawAnimation.begin().thenPlay("animation.primordial_concord.air_lift");
    private static final RawAnimation TRANS_34 = RawAnimation.begin().thenPlay("animation.primordial_concord.transition_34");
    private static final RawAnimation P4_IDLE = RawAnimation.begin().thenLoop("animation.primordial_concord.fire_idle");
    private static final RawAnimation P4_WALK = RawAnimation.begin().thenLoop("animation.primordial_concord.fire_walk");
    private static final RawAnimation P4_IGNITE = RawAnimation.begin().thenPlay("animation.primordial_concord.fire_ignite");
    private static final RawAnimation P4_GENESIS = RawAnimation.begin().thenPlay("animation.primordial_concord.fire_genesis");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.primordial_concord.death");

    public PrimordialConcordEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager
            .addThreshold(ConcordPhase.TRANSITION_1_2.ordinal(), PHASE_2_THRESHOLD)
            .addThreshold(ConcordPhase.TRANSITION_2_3.ordinal(), PHASE_3_THRESHOLD)
            .addThreshold(ConcordPhase.TRANSITION_3_4.ordinal(), PHASE_4_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the Primordial Concord.
     * 1500 HP, 8 armor, 0.18 speed, 16 base damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, ConcordPhase.DORMANT.ordinal());
        builder.define(IS_ENRAGED, false);
    }

    public ConcordPhase getConcordPhase() {
        return ConcordPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setConcordPhase(ConcordPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public boolean isEnraged() {
        return this.entityData.get(IS_ENRAGED);
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick() due to complex multi-phase behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (specialCooldown > 0) specialCooldown--;
            if (arenaCooldown > 0) arenaCooldown--;

            ConcordPhase phase = getConcordPhase();

            switch (phase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 16.0);
                    if (nearest != null) {
                        transitionToPhase(ConcordPhase.AWAKENING.ordinal());
                    }
                }
                case AWAKENING -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(ConcordPhase.PHASE_1_EARTH.ordinal());
                    }
                }
                case PHASE_1_EARTH -> {
                    phaseManager.tick();
                    tickPhase1Earth();
                }
                case TRANSITION_1_2 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(ConcordPhase.PHASE_2_WATER.ordinal());
                    }
                }
                case PHASE_2_WATER -> {
                    phaseManager.tick();
                    tickPhase2Water();
                }
                case TRANSITION_2_3 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(ConcordPhase.PHASE_3_AIR.ordinal());
                    }
                }
                case PHASE_3_AIR -> {
                    phaseManager.tick();
                    tickPhase3Air();
                }
                case TRANSITION_3_4 -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(ConcordPhase.PHASE_4_FIRE.ordinal());
                    }
                }
                case PHASE_4_FIRE -> {
                    tickPhase4Fire();
                }
                case DEATH -> {
                    // Death animation
                }
            }
        }
    }

    // ========== Phase 1: Earth ==========

    private void tickPhase1Earth() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        if (columnTrapCooldown > 0) columnTrapCooldown--;

        double distance = this.distanceTo(target);

        // Move toward target
        if (distance > 4.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.14).add(0, this.getDeltaMovement().y, 0));
        }

        // Column trap - immobilize player
        if (columnTrapCooldown <= 0 && distance < 10.0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 127, false, true));
            columnTrapCooldown = 140;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY(), target.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
            }
            this.level().playSound(null, target.blockPosition(),
                SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
        }

        // Crush attack
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_1_DAMAGE);
            attackCooldown = 35;
        }
    }

    // ========== Phase 2: Water ==========

    private void tickPhase2Water() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        if (currentPulseCooldown > 0) currentPulseCooldown--;

        double distance = this.distanceTo(target);

        // Move toward target
        if (distance > 5.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.14).add(0, this.getDeltaMovement().y, 0));
        }

        // Current drag - pull players toward center
        if (currentPulseCooldown <= 0) {
            AABB area = this.getBoundingBox().inflate(12.0);
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : nearbyPlayers) {
                Vec3 pullDir = this.position().subtract(player.position()).normalize();
                player.push(pullDir.x * 0.2, 0, pullDir.z * 0.2);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
            }
            currentPulseCooldown = 30;
        }

        // Flood attack
        if (distance < 5.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_2_DAMAGE);
            target.setAirSupply(Math.max(0, target.getAirSupply() - 60)); // Simulate drowning
            attackCooldown = 30;
        }
    }

    // ========== Phase 3: Air ==========

    private void tickPhase3Air() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        if (pushCooldown > 0) pushCooldown--;

        double distance = this.distanceTo(target);

        // Move toward target (faster in air phase)
        if (distance > 5.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.2).add(0, this.getDeltaMovement().y, 0));
        }

        // Push attack - strong knockback toward void
        if (pushCooldown <= 0 && distance < 8.0) {
            Vec3 pushDir = target.position().subtract(this.position()).normalize();
            target.push(pushDir.x * 2.5, 0.6, pushDir.z * 2.5);
            pushCooldown = 50;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 2.0f, 0.8f);
        }

        // Direct damage
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_3_DAMAGE);
            attackCooldown = 30;
        }
    }

    // ========== Phase 4: Fire ==========

    private void tickPhase4Fire() {
        Player target = this.level().getNearestPlayer(this, 48.0);
        if (target == null) return;

        if (genesisLightningCooldown > 0) genesisLightningCooldown--;

        double distance = this.distanceTo(target);

        // Move toward target
        if (distance > 4.0) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.16).add(0, this.getDeltaMovement().y, 0));
        }

        // Ignite floor and oil
        if (!oilIgnited && this.tickCount % 60 == 0) {
            oilIgnited = true;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(), 30, 5.0, 0.1, 5.0, 0.05);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 2.0f, 0.5f);
        }

        // Genesis lightning - devastating AoE
        if (genesisLightningCooldown <= 0 && distance < 12.0) {
            // AoE lightning damage
            AABB area = this.getBoundingBox().inflate(10.0);
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);
            for (Player player : nearbyPlayers) {
                player.hurt(this.damageSources().lightningBolt(), 10.0f);
                player.setRemainingFireTicks(100);
            }
            genesisLightningCooldown = 120;

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + 2.0, this.getZ(), 30, 3.0, 2.0, 3.0, 0.2);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5f, 0.5f);
        }

        // Fire melee
        if (distance < 4.0 && attackCooldown <= 0) {
            target.hurt(this.damageSources().mobAttack(this), PHASE_4_DAMAGE);
            target.setRemainingFireTicks(80);
            attackCooldown = 25;
        }
    }

    // ========== Damage Handling ==========

    @Override
    public boolean hurt(DamageSource source, float amount) {
        ConcordPhase phase = getConcordPhase();

        // Phase-specific vanilla item counters
        switch (phase) {
            case PHASE_1_EARTH -> {
                // Pickaxe type attacks (mining damage) deal bonus
                if (source.isExplosion()) {
                    amount *= 2.0f; // TNT is effective vs earth
                }
            }
            case PHASE_2_WATER -> {
                // Fire/lava items deal bonus
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    amount *= 2.0f;
                }
            }
            case PHASE_3_AIR -> {
                // Compact/heavy items (anvil, etc.)
                if (source.type().msgId().contains("fall") || source.type().msgId().contains("anvil")) {
                    amount *= 2.0f;
                }
            }
            case PHASE_4_FIRE -> {
                // Water/ice items
                if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
                    amount *= 2.0f;
                }
            }
            default -> {}
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getConcordPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        ConcordPhase newPhase = ConcordPhase.values()[phase];
        ConcordPhase oldPhase = getConcordPhase();

        if (newPhase == oldPhase) return;

        setConcordPhase(newPhase);

        switch (newPhase) {
            case AWAKENING -> transitionTimer = AWAKENING_DURATION;
            case TRANSITION_1_2, TRANSITION_2_3, TRANSITION_3_4 -> transitionTimer = TRANSITION_DURATION;
            case PHASE_1_EARTH -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_1_DAMAGE);
                this.getAttribute(Attributes.ARMOR).setBaseValue(10.0); // Extra armor in earth phase
            }
            case PHASE_2_WATER -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_2_DAMAGE);
                this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
            }
            case PHASE_3_AIR -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_3_DAMAGE);
                this.getAttribute(Attributes.ARMOR).setBaseValue(6.0);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
            }
            case PHASE_4_FIRE -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PHASE_4_DAMAGE);
                this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
                oilIgnited = false;
            }
            default -> transitionTimer = 0;
        }

        // Phase change sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0f, 0.4f);

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
        return switch (ConcordPhase.values()[phase]) {
            case PHASE_2_WATER, TRANSITION_1_2 -> PHASE_2_THRESHOLD;
            case PHASE_3_AIR, TRANSITION_2_3 -> PHASE_3_THRESHOLD;
            case PHASE_4_FIRE, TRANSITION_3_4 -> PHASE_4_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (ConcordPhase.values()[phase]) {
            case PHASE_1_EARTH -> List.of("column_trap", "crush", "ground_spike");
            case PHASE_2_WATER -> List.of("flood", "current_drag", "whirlpool");
            case PHASE_3_AIR -> List.of("push", "lift_drop", "tornado");
            case PHASE_4_FIRE -> List.of("ignite_oil", "fire_melee", "genesis_lightning");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getConcordPhase() != ConcordPhase.DEATH) {
            transitionToPhase(ConcordPhase.DEATH.ordinal());
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
            ConcordPhase phase = getConcordPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case AWAKENING -> state.setAndContinue(AWAKEN_ANIM);
                case PHASE_1_EARTH -> {
                    if (state.isMoving()) yield state.setAndContinue(P1_WALK);
                    yield state.setAndContinue(P1_IDLE);
                }
                case TRANSITION_1_2 -> state.setAndContinue(TRANS_12);
                case PHASE_2_WATER -> {
                    if (state.isMoving()) yield state.setAndContinue(P2_WALK);
                    yield state.setAndContinue(P2_IDLE);
                }
                case TRANSITION_2_3 -> state.setAndContinue(TRANS_23);
                case PHASE_3_AIR -> {
                    if (state.isMoving()) yield state.setAndContinue(P3_WALK);
                    yield state.setAndContinue(P3_IDLE);
                }
                case TRANSITION_3_4 -> state.setAndContinue(TRANS_34);
                case PHASE_4_FIRE -> {
                    if (state.isMoving()) yield state.setAndContinue(P4_WALK);
                    yield state.setAndContinue(P4_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            ConcordPhase phase = getConcordPhase();
            if (this.swinging) {
                return switch (phase) {
                    case PHASE_1_EARTH -> state.setAndContinue(P1_CRUSH);
                    case PHASE_2_WATER -> state.setAndContinue(P2_FLOOD);
                    case PHASE_3_AIR -> state.setAndContinue(P3_PUSH);
                    case PHASE_4_FIRE -> state.setAndContinue(P4_IGNITE);
                    default -> state.setAndContinue(P1_CRUSH);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(P1_IDLE);
        }));
    }
}
