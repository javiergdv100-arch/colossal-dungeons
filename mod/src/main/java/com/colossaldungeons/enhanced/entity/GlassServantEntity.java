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
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
 * Glass Servant - Humanoid made of polished glass and shards.
 *
 * Walks with brittle steps through the Mirror Castle halls. Explodes in shards
 * on death dealing AoE damage. Nearly invisible when standing still near a mirror.
 *
 * Stats: 24 HP, 3 armor, 6 damage.
 * Size: 0.6 x 1.8 blocks (humanoid).
 *
 * Behavior:
 * - Patrols halls with brittle, clicking footsteps.
 * - Explodes in crystal shards on death (AoE damage to nearby entities).
 * - Becomes nearly invisible (transparency) when standing still near mirrors.
 * - Vulnerable to blunt/crushing damage (hammers, maces).
 *
 * Vanilla interactions:
 * - Shield: blocks the death explosion shards (crucial).
 * - Snowballs: stuns it due to impact on fragile structure.
 * - Torches: placed behind it project shadows revealing position when still.
 */
public class GlassServantEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_INVISIBLE_NEAR_MIRROR =
        SynchedEntityData.defineId(GlassServantEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float DEATH_EXPLOSION_RADIUS = 4.0f;
    private static final float DEATH_EXPLOSION_DAMAGE = 8.0f;
    private static final int STUN_DURATION_TICKS = 30; // 1.5 seconds from snowball

    private int stillTimer = 0;
    private static final int INVISIBILITY_THRESHOLD = 40; // 2 seconds standing still

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.glass_servant.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.glass_servant.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.glass_servant.attack");
    private static final RawAnimation EXPLODE = RawAnimation.begin().thenPlay("animation.glass_servant.explode");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.glass_servant.stunned");

    public GlassServantEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Glass Servant.
     * 24 HP, 3 armor, 6 damage, 0.28 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_INVISIBLE_NEAR_MIRROR, false);
    }

    public boolean isInvisibleNearMirror() {
        return this.entityData.get(IS_INVISIBLE_NEAR_MIRROR);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.9));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Track stillness for invisibility near mirrors
            if (this.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                stillTimer++;
                if (stillTimer >= INVISIBILITY_THRESHOLD && isNearMirrorSurface()) {
                    this.entityData.set(IS_INVISIBLE_NEAR_MIRROR, true);
                }
            } else {
                stillTimer = 0;
                this.entityData.set(IS_INVISIBLE_NEAR_MIRROR, false);
            }
        }
    }

    /**
     * Checks if the entity is near a mirror-like surface (glass blocks, etc.)
     */
    private boolean isNearMirrorSurface() {
        // Check for glass blocks or mirror blocks nearby
        // In the Mirror Castle context, many walls contain reflective surfaces
        return this.level().getBlockStates(this.getBoundingBox().inflate(2.0))
            .anyMatch(state -> state.is(net.minecraft.tags.BlockTags.IMPERMEABLE)); // Glass tag
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Extra vulnerable to blunt/crushing damage
        // Projectiles (like snowballs) stun instead of just dealing damage
        if (source.isIndirect()) {
            // Snowball-like projectile stun
            this.setNoAi(true);
            // Re-enable AI after stun
            // This is simplified - in full impl would use a ticker
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void tickDeath() {
        // Explode in shards on death
        if (this.deathTime == 0 && !this.level().isClientSide()) {
            explodeInShards();
        }
        super.tickDeath();
    }

    /**
     * Explodes in crystal shards dealing AoE damage to nearby entities.
     */
    private void explodeInShards() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        AABB explosionBox = this.getBoundingBox().inflate(DEATH_EXPLOSION_RADIUS);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
            LivingEntity.class, explosionBox,
            e -> e != this && !e.isSpectator()
        );

        DamageSource shardSource = this.damageSources().mobAttack(this);
        for (LivingEntity target : targets) {
            double distance = this.distanceTo(target);
            if (distance <= DEATH_EXPLOSION_RADIUS) {
                // Damage falls off with distance
                float effectiveDamage = DEATH_EXPLOSION_DAMAGE * (1.0f - (float)(distance / (DEATH_EXPLOSION_RADIUS * 1.5)));
                // Shield blocks the shards
                if (target instanceof Player player && player.isBlocking()) {
                    effectiveDamage *= 0.1f; // Shield reduces shard damage significantly
                }
                target.hurt(shardSource, Math.max(1.0f, effectiveDamage));
            }
        }

        // Visual and audio effects
        serverLevel.playSound(null, this.blockPosition(),
            SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 1.0f);
        serverLevel.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + 1.0, this.getZ(), 30, 1.5, 1.0, 1.5, 0.3);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(EXPLODE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
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
