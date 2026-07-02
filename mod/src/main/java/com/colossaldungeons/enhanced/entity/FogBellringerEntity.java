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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Fog Bellringer - A spectral monk from the Veiled Peak dungeon.
 *
 * Rings a bell to generate fog and summon allied entities.
 * Breaking the bell (via crossbow bolt or snowball) permanently disarms
 * the monk and clears local fog. Without bell, becomes a weak melee fighter.
 *
 * Stats: 26 HP, 2 armor, 0.25 speed, 5 attack damage.
 */
public class FogBellringerEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> BELL_INTACT =
        SynchedEntityData.defineId(FogBellringerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_RINGING =
        SynchedEntityData.defineId(FogBellringerEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int RING_INTERVAL = 100; // 5 seconds between rings
    private static final int RING_DURATION = 40; // 2 seconds of ringing animation
    private static final int FOG_RADIUS = 12;
    private static final int SUMMON_COOLDOWN = 200; // 10 seconds between summons
    private static final int BELL_HP = 3; // Hits to break the bell

    private int ringCooldown = 0;
    private int ringTimer = 0;
    private int summonCooldown = 0;
    private int bellHits = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fog_bellringer.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.fog_bellringer.walk");
    private static final RawAnimation RING_BELL = RawAnimation.begin().thenPlay("animation.fog_bellringer.ring_bell");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.fog_bellringer.attack");
    private static final RawAnimation DISARMED_IDLE = RawAnimation.begin().thenLoop("animation.fog_bellringer.disarmed_idle");

    public FogBellringerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Fog Bellringer.
     * 26 HP, 2 armor, 0.25 speed, 5 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 26.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BELL_INTACT, true);
        builder.define(IS_RINGING, false);
    }

    public boolean isBellIntact() {
        return this.entityData.get(BELL_INTACT);
    }

    public boolean isRinging() {
        return this.entityData.get(IS_RINGING);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (ringCooldown > 0) ringCooldown--;
            if (summonCooldown > 0) summonCooldown--;

            if (isBellIntact()) {
                // Ring the bell periodically
                if (isRinging()) {
                    ringTimer--;
                    if (ringTimer <= 0) {
                        this.entityData.set(IS_RINGING, false);
                        generateFog();
                    }
                } else if (ringCooldown <= 0 && this.getTarget() != null) {
                    // Start ringing
                    this.entityData.set(IS_RINGING, true);
                    ringTimer = RING_DURATION;
                    ringCooldown = RING_INTERVAL;

                    // Bell sound
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0f, 0.6f);
                }

                // Summon allies periodically
                if (summonCooldown <= 0 && this.getTarget() != null) {
                    summonAllies();
                    summonCooldown = SUMMON_COOLDOWN;
                }
            }
        }
    }

    /**
     * Generates fog in the surrounding area by spawning particles.
     */
    private void generateFog() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * FOG_RADIUS * 2;
                double y = this.getY() + this.random.nextDouble() * 3.0;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * FOG_RADIUS * 2;
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * Summons allied entities (other veiled peak mobs) nearby.
     * In the full implementation, this spawns VeiledSoul or similar entities.
     */
    private void summonAllies() {
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5f, 0.8f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                this.getX(), this.getY() + 2.0, this.getZ(), 8, 1.0, 0.5, 1.0, 0.02);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Check if projectile hits the bell
        if (isBellIntact() && (source.getDirectEntity() instanceof Snowball ||
            source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))) {
            bellHits++;
            if (bellHits >= BELL_HP) {
                breakBell();
            }
            // Bell absorbs the hit
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.0f, 1.5f);
            return false;
        }

        return super.hurt(source, amount);
    }

    /**
     * Breaks the bell permanently, disarming the bellringer.
     */
    private void breakBell() {
        this.entityData.set(BELL_INTACT, false);
        this.entityData.set(IS_RINGING, false);

        // Bell breaking sound and particles
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                this.getX(), this.getY() + 1.5, this.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (!isBellIntact()) {
                return state.setAndContinue(DISARMED_IDLE);
            }
            if (isRinging()) {
                return state.setAndContinue(RING_BELL);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
