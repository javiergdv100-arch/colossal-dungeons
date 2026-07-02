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
 * Primordial Pebble - A living rock from the Primordial Tower earth floor.
 *
 * Traps feet with emerging stone roots (brief immobilize). Very resistant overall;
 * weak to blunt damage. Creates temporary walls. TNT destroys instantly.
 *
 * Stats: 26 HP, 6 armor, 0.2 speed, 6 damage.
 */
public class PrimordialPebbleEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_ROOTING =
        SynchedEntityData.defineId(PrimordialPebbleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ROOT_COOLDOWN_TICKS = 120; // 6 seconds
    private static final int ROOT_IMMOBILIZE_DURATION = 40; // 2 seconds
    private static final double ROOT_RANGE = 5.0;
    private int rootCooldown = 0;
    private int wallCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.primordial_pebble.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.primordial_pebble.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.primordial_pebble.attack");
    private static final RawAnimation ROOT = RawAnimation.begin().thenPlay("animation.primordial_pebble.root");

    public PrimordialPebbleEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Primordial Pebble.
     * 26 HP, 6 armor, 0.2 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 26.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_ROOTING, false);
    }

    public boolean isRooting() {
        return this.entityData.get(IS_ROOTING);
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
            if (rootCooldown > 0) rootCooldown--;
            if (wallCooldown > 0) wallCooldown--;

            // Stone root attack - immobilize nearby players
            if (rootCooldown <= 0) {
                attemptRootAttack();
            }

            // Rock particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + 0.2, this.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    /**
     * Attempts to immobilize nearby players with stone roots.
     */
    private void attemptRootAttack() {
        AABB area = this.getBoundingBox().inflate(ROOT_RANGE);
        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            if (this.distanceTo(player) <= ROOT_RANGE && this.hasLineOfSight(player)) {
                // Immobilize the player briefly
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOT_IMMOBILIZE_DURATION, 127, false, true));
                this.entityData.set(IS_ROOTING, true);
                rootCooldown = ROOT_COOLDOWN_TICKS;

                this.level().playSound(null, player.blockPosition(),
                    SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.1, 0.3, 0.05);
                }
                break;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // TNT explosion = instant kill
        if (source.isExplosion()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
            }
            this.level().playSound(null, this.blockPosition(),
                SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
            this.discard();
            return true;
        }

        // Weak to blunt/crushing damage (e.g., mace, pickaxe type)
        // Represented as bonus damage from falling/crush sources
        if (source.type().msgId().contains("fall") || source.type().msgId().contains("anvil")) {
            amount *= 2.5f;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (isRooting()) {
                return state.setAndContinue(ROOT);
            }
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
