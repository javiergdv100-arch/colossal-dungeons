package com.colossaldungeons.enhanced.entity;

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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Solar Mirror Colossus - An elite mirror-covered automaton from the Solar Palace.
 *
 * Fires and redirects deadly light beams across the room.
 * Shield REFLECTS beams back (critical damage to self).
 * Breaking mirrors (snowballs) reduces beam options.
 * Water empties all mirrors (disables beams 10s).
 * Fishing rod rotates its mirrors (self-damage).
 *
 * Stats: 150 HP, 7 armor, 0.18 speed, beam 13 damage.
 */
public class SolarMirrorColossusEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> MIRRORS_INTACT =
        SynchedEntityData.defineId(SolarMirrorColossusEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_DISABLED =
        SynchedEntityData.defineId(SolarMirrorColossusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_FIRING_BEAM =
        SynchedEntityData.defineId(SolarMirrorColossusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_MIRRORS = 6;
    private static final float BEAM_DAMAGE = 13.0f;
    private static final float REFLECTED_DAMAGE_MULTIPLIER = 2.0f;
    private static final int BEAM_COOLDOWN = 80; // 4 seconds
    private static final int DISABLE_DURATION = 200; // 10 seconds
    private static final double BEAM_RANGE = 16.0;

    private int beamCooldown = 0;
    private int disableTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.solar_mirror_colossus.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.solar_mirror_colossus.move");
    private static final RawAnimation FIRE_BEAM = RawAnimation.begin().thenPlay("animation.solar_mirror_colossus.fire_beam");
    private static final RawAnimation DISABLED = RawAnimation.begin().thenLoop("animation.solar_mirror_colossus.disabled");
    private static final RawAnimation REDIRECT = RawAnimation.begin().thenPlay("animation.solar_mirror_colossus.redirect");

    public SolarMirrorColossusEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Solar Mirror Colossus.
     * 150 HP, 7 armor, 0.18 speed, beam 13 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 150.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 13.0)
            .add(Attributes.ARMOR, 7.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MIRRORS_INTACT, MAX_MIRRORS);
        builder.define(IS_DISABLED, false);
        builder.define(IS_FIRING_BEAM, false);
    }

    public int getMirrorsIntact() {
        return this.entityData.get(MIRRORS_INTACT);
    }

    public boolean isDisabled() {
        return this.entityData.get(IS_DISABLED);
    }

    public boolean isFiringBeam() {
        return this.entityData.get(IS_FIRING_BEAM);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.8, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.5));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (beamCooldown > 0) beamCooldown--;

            // Handle disabled state
            if (isDisabled()) {
                disableTimer--;
                if (disableTimer <= 0) {
                    this.entityData.set(IS_DISABLED, false);
                }
                return;
            }

            // Fire beam at target
            if (beamCooldown <= 0 && getMirrorsIntact() > 0) {
                Player target = this.level().getNearestPlayer(this, BEAM_RANGE);
                if (target != null && this.hasLineOfSight(target)) {
                    fireBeam(target);
                }
            }

            // Mirror glow particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.5, this.getZ(), 1, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * Fires a deadly light beam at the target player.
     * If the player is blocking with a shield, the beam is reflected back.
     */
    private void fireBeam(Player target) {
        this.entityData.set(IS_FIRING_BEAM, true);
        beamCooldown = BEAM_COOLDOWN;

        // Beam damage scales with mirrors intact
        float damage = BEAM_DAMAGE * ((float) getMirrorsIntact() / MAX_MIRRORS);

        // Check if target is blocking with shield - REFLECT
        if (target.isBlocking()) {
            // Reflect beam back at colossus
            this.hurt(this.damageSources().magic(), damage * REFLECTED_DAMAGE_MULTIPLIER);

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5f, 0.8f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.5, 0.5, 0.5, 0.15);
            }
        } else {
            // Beam hits player
            target.hurt(this.damageSources().magic(), damage);

            if (this.level() instanceof ServerLevel serverLevel) {
                // Beam line particles
                Vec3 start = this.position().add(0, 1.5, 0);
                Vec3 end = target.position().add(0, 1.0, 0);
                Vec3 direction = end.subtract(start).normalize();
                double dist = start.distanceTo(end);
                for (double d = 0; d < dist; d += 0.5) {
                    Vec3 pos = start.add(direction.scale(d));
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                }
            }
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 2.0f);

        // Reset firing state after brief delay (animation handles visuals)
        this.entityData.set(IS_FIRING_BEAM, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Snowball breaks a mirror
        if (source.getDirectEntity() instanceof Snowball) {
            int mirrors = getMirrorsIntact();
            if (mirrors > 0) {
                this.entityData.set(MIRRORS_INTACT, mirrors - 1);
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.0f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.getX(), this.getY() + 1.5, this.getZ(), 8, 0.5, 0.5, 0.5, 0.1);
                }
            }
            return super.hurt(source, 1.0f);
        }

        // Water disables all mirrors temporarily
        if (source.getDirectEntity() instanceof Player player) {
            if (player.getMainHandItem().is(Items.WATER_BUCKET)) {
                this.entityData.set(IS_DISABLED, true);
                disableTimer = DISABLE_DURATION;
                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.5f, 0.8f);
            }
        }

        if (source.type().msgId().contains("drown")) {
            this.entityData.set(IS_DISABLED, true);
            disableTimer = DISABLE_DURATION;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isDisabled()) {
                return state.setAndContinue(DISABLED);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (isFiringBeam()) {
                return state.setAndContinue(FIRE_BEAM);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
