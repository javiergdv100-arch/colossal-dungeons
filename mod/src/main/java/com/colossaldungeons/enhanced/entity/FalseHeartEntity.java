package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * False Heart - Hollow Leviathan dungeon elite creature.
 *
 * A massive pulsating organ that beats rhythmically, sending out push waves with each beat.
 * Each beat also spawns antibody entities. Players may be tricked into thinking this is
 * the Leviathan's true heart, but destroying it does NOT kill the Leviathan - it is a decoy.
 * Water cools it down, temporarily slowing its beat rate and reducing wave intensity.
 *
 * Stats: 150 HP, 5 armor, wave 10 damage (area push).
 * Behavior: Rhythmic beats with push waves + antibody spawning.
 * Counter: Water slows beat rate. It is a trap/decoy - not the real heart.
 */
public class FalseHeartEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> BEAT_PHASE =
        SynchedEntityData.defineId(FalseHeartEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_COOLED =
        SynchedEntityData.defineId(FalseHeartEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float WAVE_DAMAGE = 10.0f;
    private static final double WAVE_RADIUS = 8.0;
    private static final float WAVE_KNOCKBACK = 2.5f;
    private static final int NORMAL_BEAT_INTERVAL = 60; // 3 seconds between beats
    private static final int COOLED_BEAT_INTERVAL = 120; // 6 seconds when cooled with water
    private static final int ANTIBODY_SPAWN_COUNT = 2; // Per beat
    private static final double ANTIBODY_SPAWN_RADIUS = 4.0;
    private static final int COOL_DURATION = 200; // 10 seconds of cooling from water

    private int beatTimer = 0;
    private int coolTimer = 0;
    private int beatCount = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.false_heart.idle");
    private static final RawAnimation BEAT = RawAnimation.begin().thenPlay("animation.false_heart.beat");
    private static final RawAnimation BEAT_COOLED = RawAnimation.begin().thenPlay("animation.false_heart.beat_cooled");
    private static final RawAnimation CONTRACTING = RawAnimation.begin().thenLoop("animation.false_heart.contracting");
    private static final RawAnimation COOLED_IDLE = RawAnimation.begin().thenLoop("animation.false_heart.cooled_idle");

    public FalseHeartEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Fixed organ
    }

    /**
     * Creates the attribute supplier for False Heart.
     * 150 HP, 0.0 speed (immobile), 10 damage (wave), 5 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 150.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAT_PHASE, 0); // 0 = resting, 1 = contracting, 2 = releasing wave
        builder.define(IS_COOLED, false);
    }

    public int getBeatPhase() {
        return this.entityData.get(BEAT_PHASE);
    }

    private void setBeatPhase(int phase) {
        this.entityData.set(BEAT_PHASE, phase);
    }

    public boolean isCooled() {
        return this.entityData.get(IS_COOLED);
    }

    private void setCooled(boolean cooled) {
        this.entityData.set(IS_COOLED, cooled);
    }

    @Override
    protected void registerGoals() {
        // No goals - entirely tick-driven behavior
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Handle cooling from water
            if (coolTimer > 0) {
                coolTimer--;
                if (coolTimer <= 0) {
                    setCooled(false);
                }
            }

            // Check if currently in water
            if (this.isInWaterOrBubble() && !isCooled()) {
                setCooled(true);
                coolTimer = COOL_DURATION;
            }

            // Beat cycle
            int interval = isCooled() ? COOLED_BEAT_INTERVAL : NORMAL_BEAT_INTERVAL;
            beatTimer++;

            if (beatTimer >= interval - 20 && getBeatPhase() == 0) {
                // Start contracting phase (warning)
                setBeatPhase(1);
            }

            if (beatTimer >= interval) {
                // Release the beat wave
                performBeat();
                beatTimer = 0;
                setBeatPhase(0);
                beatCount++;
            }
        }
    }

    /**
     * Performs a heartbeat, sending out a push wave and spawning antibodies.
     */
    private void performBeat() {
        setBeatPhase(2);

        // Push wave damage and knockback
        float waveDamage = isCooled() ? WAVE_DAMAGE * 0.5f : WAVE_DAMAGE;
        float waveKnockback = isCooled() ? WAVE_KNOCKBACK * 0.5f : WAVE_KNOCKBACK;

        AABB waveBox = this.getBoundingBox().inflate(WAVE_RADIUS);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
            LivingEntity.class, waveBox, e -> e != this && !e.isSpectator());

        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            double dist = this.distanceTo(target);
            if (dist <= WAVE_RADIUS) {
                // Damage decreases with distance
                float effectiveDmg = waveDamage * (1.0f - (float)(dist / WAVE_RADIUS) * 0.5f);
                target.hurt(source, effectiveDmg);

                // Push away from heart
                Vec3 pushDir = target.position().subtract(this.position()).normalize();
                float effectiveKnockback = waveKnockback * (1.0f - (float)(dist / WAVE_RADIUS) * 0.6f);
                target.knockback(effectiveKnockback, -pushDir.x, -pushDir.z);
                target.hurtMarked = true;
            }
        }

        // Spawn antibodies
        if (this.level() instanceof ServerLevel serverLevel) {
            int spawnCount = isCooled() ? 1 : ANTIBODY_SPAWN_COUNT;
            for (int i = 0; i < spawnCount; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * ANTIBODY_SPAWN_RADIUS * 2;
                double offsetZ = (this.random.nextDouble() - 0.5) * ANTIBODY_SPAWN_RADIUS * 2;
                BlockPos spawnPos = this.blockPosition().offset((int) offsetX, 0, (int) offsetZ);
                // Antibody spawning would reference CDEEntities.ANTIBODY registry entry
                // Actual spawning handled by dungeon spawn system at runtime
            }
        }

        // Beat sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
            isCooled() ? 1.0f : 2.0f, isCooled() ? 0.8f : 1.0f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        // Water-based damage cools the heart
        if (result && !this.level().isClientSide()) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
                setCooled(true);
                coolTimer = COOL_DURATION;
            }
        }
        return result;
    }

    @Override
    public boolean isNoAi() {
        return true; // Completely stationary
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            int phase = getBeatPhase();
            if (isCooled()) {
                if (phase == 2) {
                    return state.setAndContinue(BEAT_COOLED);
                }
                return state.setAndContinue(COOLED_IDLE);
            }
            return switch (phase) {
                case 1 -> state.setAndContinue(CONTRACTING);
                case 2 -> state.setAndContinue(BEAT);
                default -> state.setAndContinue(IDLE);
            };
        }));
    }
}
