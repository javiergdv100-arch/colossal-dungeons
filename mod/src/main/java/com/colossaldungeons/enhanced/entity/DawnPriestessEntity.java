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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Dawn Priestess - An elite healer of the sun cult from the Solar Palace.
 *
 * Heals allies and self under direct light. Launches blinding flashes.
 * In SHADOW: loses healing, becomes vulnerable.
 * Fishing rod drags to shadow.
 * Bone meal overload stuns (overstimulation).
 *
 * Stats: 120 HP, 3 armor, 0.22 speed, light 10 damage.
 */
public class DawnPriestessEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_IN_LIGHT =
        SynchedEntityData.defineId(DawnPriestessEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_STUNNED =
        SynchedEntityData.defineId(DawnPriestessEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int LIGHT_THRESHOLD = 12;
    private static final double HEAL_RANGE = 10.0;
    private static final float HEAL_AMOUNT = 4.0f;
    private static final float SELF_HEAL_AMOUNT = 2.0f;
    private static final int HEAL_COOLDOWN = 60; // 3 seconds
    private static final int FLASH_COOLDOWN = 80;
    private static final int STUN_DURATION = 60; // 3 seconds
    private static final float SHADOW_DAMAGE_MULTIPLIER = 1.5f;

    private int healCooldown = 0;
    private int flashCooldown = 0;
    private int stunTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dawn_priestess.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.dawn_priestess.move");
    private static final RawAnimation HEAL = RawAnimation.begin().thenPlay("animation.dawn_priestess.heal");
    private static final RawAnimation FLASH = RawAnimation.begin().thenPlay("animation.dawn_priestess.flash");
    private static final RawAnimation SHADOW = RawAnimation.begin().thenLoop("animation.dawn_priestess.shadow");
    private static final RawAnimation STUNNED_ANIM = RawAnimation.begin().thenLoop("animation.dawn_priestess.stunned");

    public DawnPriestessEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Dawn Priestess.
     * 120 HP, 3 armor, 0.22 speed, 10 light damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_IN_LIGHT, true);
        builder.define(IS_STUNNED, false);
    }

    public boolean isInLight() {
        return this.entityData.get(IS_IN_LIGHT);
    }

    public boolean isStunned() {
        return this.entityData.get(IS_STUNNED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 5.0f, 1.0, 1.3));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (healCooldown > 0) healCooldown--;
            if (flashCooldown > 0) flashCooldown--;

            // Handle stun
            if (isStunned()) {
                stunTimer--;
                if (stunTimer <= 0) {
                    this.entityData.set(IS_STUNNED, false);
                }
                this.getNavigation().stop();
                return;
            }

            // Check light level
            int lightLevel = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
            boolean inLight = lightLevel >= LIGHT_THRESHOLD;
            this.entityData.set(IS_IN_LIGHT, inLight);

            if (inLight) {
                // Heal allies and self
                if (healCooldown <= 0) {
                    healAllies();
                    healCooldown = HEAL_COOLDOWN;
                }

                // Self heal
                if (this.getHealth() < this.getMaxHealth() && this.tickCount % 40 == 0) {
                    this.heal(SELF_HEAL_AMOUNT);
                }
            }

            // Blinding flash attack
            if (flashCooldown <= 0) {
                Player target = this.level().getNearestPlayer(this, 12.0);
                if (target != null && inLight) {
                    performFlash(target);
                    flashCooldown = FLASH_COOLDOWN;
                }
            }

            // Light particles
            if (inLight && this.level() instanceof ServerLevel serverLevel && this.tickCount % 6 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.5, this.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * Heals all nearby ally mobs.
     */
    private void healAllies() {
        AABB area = this.getBoundingBox().inflate(HEAL_RANGE);
        List<Mob> nearbyMobs = this.level().getEntitiesOfClass(Mob.class, area,
            mob -> mob != this && !(mob instanceof Player));

        for (Mob mob : nearbyMobs) {
            if (this.distanceTo(mob) <= HEAL_RANGE && mob.getHealth() < mob.getMaxHealth()) {
                mob.heal(HEAL_AMOUNT);

                // Heal particles
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                        mob.getX(), mob.getY() + 1.0, mob.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0f, 1.5f);
    }

    /**
     * Performs a blinding flash attack on nearby players.
     */
    private void performFlash(Player target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
        target.hurt(this.damageSources().magic(), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.5f, 2.0f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH,
                target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Takes more damage in shadow
        if (!isInLight()) {
            amount *= SHADOW_DAMAGE_MULTIPLIER;
        }

        // Bone meal overload = stun
        if (source.getDirectEntity() instanceof Player player) {
            if (player.getMainHandItem().is(Items.BONE_MEAL)) {
                this.entityData.set(IS_STUNNED, true);
                stunTimer = STUN_DURATION;

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0f, 0.3f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isStunned()) {
                return state.setAndContinue(STUNNED_ANIM);
            }
            if (!isInLight()) {
                return state.setAndContinue(SHADOW);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (healCooldown > HEAL_COOLDOWN - 20 && isInLight()) {
                return state.setAndContinue(HEAL);
            }
            if (flashCooldown > FLASH_COOLDOWN - 20) {
                return state.setAndContinue(FLASH);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
