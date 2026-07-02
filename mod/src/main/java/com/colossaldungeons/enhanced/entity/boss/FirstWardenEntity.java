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
 * First Warden - Primordial Tower miniboss.
 *
 * Oldest custodian combining TWO elements simultaneously per attack
 * (e.g., steam=water+fire, mud=earth+water). Changes arena dominant element.
 * Only vulnerable to the element NOT being used.
 *
 * Implements IBossPhase with 2 phases.
 *
 * Stats: 330 HP, 7 armor, 0.2 speed, 13 damage.
 */
public class FirstWardenEntity extends CDEGeoEntity implements GeoEntity, IBossPhase {

    /**
     * Combined element pairs used in attacks.
     */
    public enum DualElement {
        STEAM(0),       // Water + Fire
        MUD(1),         // Earth + Water
        DUST_STORM(2),  // Air + Earth
        INFERNO(3);     // Fire + Air

        private final int id;
        DualElement(int id) { this.id = id; }
        public int getId() { return id; }
    }

    public enum WardenPhase {
        DORMANT,
        PHASE_1,
        TRANSITION,
        PHASE_2,
        DEATH
    }

    private static final EntityDataAccessor<Integer> BOSS_PHASE =
        SynchedEntityData.defineId(FirstWardenEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ACTIVE_DUAL_ELEMENT =
        SynchedEntityData.defineId(FirstWardenEntity.class, EntityDataSerializers.INT);

    private static final float PHASE_2_THRESHOLD = 0.45f;
    private static final int ELEMENT_CHANGE_TICKS = 140; // 7 seconds
    private static final int TRANSITION_DURATION = 60;
    private static final float COMBO_DAMAGE = 13.0f;
    private static final float PHASE_2_DAMAGE_BOOST = 1.4f;

    private final BossPhaseManager<FirstWardenEntity> phaseManager;
    private int elementChangeTimer = 0;
    private int transitionTimer = 0;
    private int attackCooldown = 0;
    private int specialCooldown = 0;

    // ========== Animations ==========
    private static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("animation.first_warden.dormant");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.first_warden.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.first_warden.move");
    private static final RawAnimation ATTACK_STEAM = RawAnimation.begin().thenPlay("animation.first_warden.attack_steam");
    private static final RawAnimation ATTACK_MUD = RawAnimation.begin().thenPlay("animation.first_warden.attack_mud");
    private static final RawAnimation ATTACK_DUST = RawAnimation.begin().thenPlay("animation.first_warden.attack_dust");
    private static final RawAnimation ATTACK_INFERNO = RawAnimation.begin().thenPlay("animation.first_warden.attack_inferno");
    private static final RawAnimation TRANSITION_ANIM = RawAnimation.begin().thenPlay("animation.first_warden.transition");
    private static final RawAnimation PHASE2_IDLE = RawAnimation.begin().thenLoop("animation.first_warden.phase2_idle");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay("animation.first_warden.death");

    public FirstWardenEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.phaseManager = new BossPhaseManager<>(this);
        this.phaseManager.addThreshold(WardenPhase.TRANSITION.ordinal(), PHASE_2_THRESHOLD);
    }

    /**
     * Creates the attribute supplier for the First Warden.
     * 330 HP, 7 armor, 0.2 speed, 13 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 330.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 13.0)
            .add(Attributes.ARMOR, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_PHASE, WardenPhase.DORMANT.ordinal());
        builder.define(ACTIVE_DUAL_ELEMENT, DualElement.STEAM.ordinal());
    }

    public WardenPhase getWardenPhase() {
        return WardenPhase.values()[this.entityData.get(BOSS_PHASE)];
    }

    private void setWardenPhase(WardenPhase phase) {
        this.entityData.set(BOSS_PHASE, phase.ordinal());
    }

    public DualElement getActiveDualElement() {
        return DualElement.values()[this.entityData.get(ACTIVE_DUAL_ELEMENT)];
    }

    private void setActiveDualElement(DualElement element) {
        this.entityData.set(ACTIVE_DUAL_ELEMENT, element.ordinal());
    }

    @Override
    protected void registerGoals() {
        // AI managed in tick() due to complex dual-element behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (attackCooldown > 0) attackCooldown--;
            if (specialCooldown > 0) specialCooldown--;

            WardenPhase phase = getWardenPhase();

            switch (phase) {
                case DORMANT -> {
                    Player nearest = this.level().getNearestPlayer(this, 14.0);
                    if (nearest != null) {
                        transitionToPhase(WardenPhase.PHASE_1.ordinal());
                    }
                }
                case PHASE_1, PHASE_2 -> {
                    phaseManager.tick();
                    tickCombat();
                }
                case TRANSITION -> {
                    transitionTimer--;
                    if (transitionTimer <= 0) {
                        transitionToPhase(WardenPhase.PHASE_2.ordinal());
                    }
                }
                case DEATH -> {
                    // Death animation
                }
            }
        }
    }

    /**
     * Main combat tick for both phases.
     */
    private void tickCombat() {
        Player target = this.level().getNearestPlayer(this, 32.0);
        if (target == null) return;

        // Element cycling
        elementChangeTimer++;
        if (elementChangeTimer >= ELEMENT_CHANGE_TICKS) {
            cycleDualElement();
            elementChangeTimer = 0;
        }

        double distance = this.distanceTo(target);

        // Move toward target
        if (distance > 3.5) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(direction.scale(0.15).add(0, this.getDeltaMovement().y, 0));
        }

        // Melee combo attack
        if (distance < 4.0 && attackCooldown <= 0) {
            performDualElementAttack(target);
            attackCooldown = 40;
        }

        // Area denial special
        if (specialCooldown <= 0 && distance < 8.0) {
            performAreaDenial(target);
            specialCooldown = 100;
        }
    }

    /**
     * Cycles to the next dual element combination.
     */
    private void cycleDualElement() {
        DualElement current = getActiveDualElement();
        DualElement next = DualElement.values()[(current.ordinal() + 1) % DualElement.values().length];
        setActiveDualElement(next);

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    /**
     * Performs a dual-element attack with combined effects.
     */
    private void performDualElementAttack(Player target) {
        float damage = COMBO_DAMAGE;
        if (getWardenPhase() == WardenPhase.PHASE_2) {
            damage *= PHASE_2_DAMAGE_BOOST;
        }

        target.hurt(this.damageSources().mobAttack(this), damage);

        // Dual element effects
        switch (getActiveDualElement()) {
            case STEAM -> {
                // Water + Fire = burn + slow
                target.setRemainingFireTicks(40);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
            case MUD -> {
                // Earth + Water = heavy immobilize
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            }
            case DUST_STORM -> {
                // Air + Earth = blindness + knockback
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                Vec3 dir = target.position().subtract(this.position()).normalize();
                target.push(dir.x * 1.2, 0.3, dir.z * 1.2);
            }
            case INFERNO -> {
                // Fire + Air = intense burn + launch
                target.setRemainingFireTicks(80);
                target.push(0, 0.6, 0);
            }
        }
    }

    /**
     * Performs an area denial attack changing the arena's dominant element.
     */
    private void performAreaDenial(Player target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 1.0, this.getZ(), 5, 2.0, 0.5, 2.0, 0.1);
        }
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only vulnerable to the element NOT being used
        DualElement active = getActiveDualElement();
        boolean isVulnerable = false;

        switch (active) {
            case STEAM -> {
                // Uses Water+Fire, vulnerable to Air or Earth
                if (source.type().msgId().contains("fall") || source.isExplosion()) {
                    isVulnerable = true; // Earth/blunt
                }
            }
            case MUD -> {
                // Uses Earth+Water, vulnerable to Fire or Air
                if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    isVulnerable = true;
                }
            }
            case DUST_STORM -> {
                // Uses Air+Earth, vulnerable to Water or Fire
                if (source.type().msgId().contains("drown") || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    isVulnerable = true;
                }
            }
            case INFERNO -> {
                // Uses Fire+Air, vulnerable to Water or Earth
                if (source.type().msgId().contains("drown") || source.type().msgId().contains("freeze")) {
                    isVulnerable = true;
                }
            }
        }

        // If not vulnerable element, reduce damage significantly
        if (!isVulnerable && source.getEntity() != null) {
            amount *= 0.3f;
        }

        return super.hurt(source, amount);
    }

    // ========== IBossPhase Implementation ==========

    @Override
    public int getCurrentPhase() {
        return getWardenPhase().ordinal();
    }

    @Override
    public void transitionToPhase(int phase) {
        WardenPhase newPhase = WardenPhase.values()[phase];
        WardenPhase oldPhase = getWardenPhase();

        if (newPhase == oldPhase) return;

        setWardenPhase(newPhase);

        switch (newPhase) {
            case TRANSITION -> transitionTimer = TRANSITION_DURATION;
            case PHASE_2 -> {
                this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(COMBO_DAMAGE * PHASE_2_DAMAGE_BOOST);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
            }
            default -> transitionTimer = 0;
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);

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
        return switch (WardenPhase.values()[phase]) {
            case PHASE_2, TRANSITION -> PHASE_2_THRESHOLD;
            default -> 1.0f;
        };
    }

    @Override
    public List<String> getPhaseAttackPatterns(int phase) {
        return switch (WardenPhase.values()[phase]) {
            case PHASE_1 -> List.of("dual_strike", "area_denial", "element_shift");
            case PHASE_2 -> List.of("dual_strike", "area_denial", "element_shift", "combined_storm", "arena_override");
            default -> List.of();
        };
    }

    @Override
    protected void tickDeath() {
        if (getWardenPhase() != WardenPhase.DEATH) {
            transitionToPhase(WardenPhase.DEATH.ordinal());
        }
        ++this.deathTime;
        if (this.deathTime >= 80 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ========== GeckoLib Animation ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 10, state -> {
            WardenPhase phase = getWardenPhase();
            return switch (phase) {
                case DORMANT -> state.setAndContinue(DORMANT_ANIM);
                case PHASE_1 -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(IDLE);
                }
                case TRANSITION -> state.setAndContinue(TRANSITION_ANIM);
                case PHASE_2 -> {
                    if (state.isMoving()) yield state.setAndContinue(MOVE);
                    yield state.setAndContinue(PHASE2_IDLE);
                }
                case DEATH -> state.setAndContinue(DEATH_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return switch (getActiveDualElement()) {
                    case STEAM -> state.setAndContinue(ATTACK_STEAM);
                    case MUD -> state.setAndContinue(ATTACK_MUD);
                    case DUST_STORM -> state.setAndContinue(ATTACK_DUST);
                    case INFERNO -> state.setAndContinue(ATTACK_INFERNO);
                };
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
