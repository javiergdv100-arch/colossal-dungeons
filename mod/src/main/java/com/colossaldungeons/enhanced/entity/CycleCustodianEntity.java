package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Cycle Custodian - An elemental elite from the Primordial Tower.
 *
 * Controls floor element order. Can force cycle change events.
 * Protects elemental altars. Vulnerable when desynchronized
 * (use wrong-element item to desync).
 *
 * Stats: 130 HP, 5 armor, 0.22 speed, 11 damage.
 */
public class CycleCustodianEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Synchronization state of the custodian relative to the floor cycle.
     */
    public enum SyncState {
        SYNCHRONIZED,
        DESYNCHRONIZED,
        FORCING_CYCLE
    }

    private static final EntityDataAccessor<Integer> SYNC_STATE =
        SynchedEntityData.defineId(CycleCustodianEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CONTROLLED_ELEMENT =
        SynchedEntityData.defineId(CycleCustodianEntity.class, EntityDataSerializers.INT);

    private static final int DESYNC_DURATION = 100; // 5 seconds of vulnerability
    private static final int FORCE_CYCLE_COOLDOWN = 200; // 10 seconds
    private static final double AURA_RANGE = 8.0;
    private static final float DESYNC_DAMAGE_MULTIPLIER = 2.0f;

    private int desyncTimer = 0;
    private int forceCycleCooldown = 0;
    private int auraAttackCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cycle_custodian.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.cycle_custodian.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.cycle_custodian.attack");
    private static final RawAnimation FORCE_CYCLE = RawAnimation.begin().thenPlay("animation.cycle_custodian.force_cycle");
    private static final RawAnimation DESYNC = RawAnimation.begin().thenLoop("animation.cycle_custodian.desync");

    public CycleCustodianEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Cycle Custodian.
     * 130 HP, 5 armor, 0.22 speed, 11 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 130.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SYNC_STATE, SyncState.SYNCHRONIZED.ordinal());
        builder.define(CONTROLLED_ELEMENT, 0); // Fire by default
    }

    public SyncState getSyncState() {
        return SyncState.values()[this.entityData.get(SYNC_STATE)];
    }

    private void setSyncState(SyncState state) {
        this.entityData.set(SYNC_STATE, state.ordinal());
    }

    public int getControlledElement() {
        return this.entityData.get(CONTROLLED_ELEMENT);
    }

    public boolean isDesynchronized() {
        return getSyncState() == SyncState.DESYNCHRONIZED;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (forceCycleCooldown > 0) forceCycleCooldown--;
            if (auraAttackCooldown > 0) auraAttackCooldown--;

            SyncState state = getSyncState();

            switch (state) {
                case SYNCHRONIZED -> {
                    // Periodically force cycle change
                    if (forceCycleCooldown <= 0 && this.getTarget() != null) {
                        forceCycleChange();
                    }

                    // Elemental aura damage
                    if (auraAttackCooldown <= 0) {
                        auraAttack();
                    }
                }
                case DESYNCHRONIZED -> {
                    desyncTimer--;
                    if (desyncTimer <= 0) {
                        // Re-synchronize
                        setSyncState(SyncState.SYNCHRONIZED);
                        this.level().playSound(null, this.blockPosition(),
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 1.0f);
                    }
                }
                case FORCING_CYCLE -> {
                    // Handled by forceCycleChange timer
                }
            }

            // Particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    this.getX(), this.getY() + 1.5, this.getZ(), 3, 0.4, 0.5, 0.4, 0.05);
            }
        }
    }

    /**
     * Forces a cycle change in the floor element, shifting the dungeon's active element.
     */
    private void forceCycleChange() {
        setSyncState(SyncState.FORCING_CYCLE);
        forceCycleCooldown = FORCE_CYCLE_COOLDOWN;

        // Advance controlled element
        int nextElement = (getControlledElement() + 1) % 4;
        this.entityData.set(CONTROLLED_ELEMENT, nextElement);

        // Reset back to synchronized after brief delay
        setSyncState(SyncState.SYNCHRONIZED);

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 0.7f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                this.getX(), this.getY() + 1.0, this.getZ(), 15, 1.0, 1.0, 1.0, 0.3);
        }
    }

    /**
     * Elemental aura attack on nearby players.
     */
    private void auraAttack() {
        AABB area = this.getBoundingBox().inflate(AURA_RANGE);
        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            if (this.distanceTo(player) <= AURA_RANGE && this.hasLineOfSight(player)) {
                player.hurt(this.damageSources().magic(), 4.0f);
                auraAttackCooldown = 60;
                break;
            }
        }
    }

    /**
     * Desynchronizes the custodian when hit with a wrong-element item.
     * Called externally when player uses an item that doesn't match the current cycle.
     */
    public void desynchronize() {
        if (getSyncState() != SyncState.DESYNCHRONIZED) {
            setSyncState(SyncState.DESYNCHRONIZED);
            desyncTimer = DESYNC_DURATION;

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0f, 0.5f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Takes extra damage when desynchronized
        if (isDesynchronized()) {
            amount *= DESYNC_DAMAGE_MULTIPLIER;
        }

        // Check for wrong-element item interaction to trigger desync
        if (source.getEntity() instanceof Player player && !isDesynchronized()) {
            // If the player attacks with an item that opposes the current element, desync
            // This is a simplified check - in full implementation, check held item type
            if (this.random.nextFloat() < 0.2f) { // placeholder for item check
                desynchronize();
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isDesynchronized()) {
                return state.setAndContinue(DESYNC);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (getSyncState() == SyncState.FORCING_CYCLE) {
                return state.setAndContinue(FORCE_CYCLE);
            }
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
