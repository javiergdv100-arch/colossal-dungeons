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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Genesis Spark - A living lightning discharge from the Primordial Tower.
 *
 * Chains between metal/water surfaces. If player is wet, chains deal extra damage.
 * Static but reactive (fast counter-attacks). Powder snow creates safe zones
 * (non-conductive). Wooden shield blocks.
 *
 * Stats: 20 HP, 0.1 speed (mostly static), lightning 8 damage.
 */
public class GenesisSparkEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_CHARGED =
        SynchedEntityData.defineId(GenesisSparkEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double CHAIN_RANGE = 8.0;
    private static final float WET_BONUS_DAMAGE = 4.0f;
    private static final int COUNTER_ATTACK_COOLDOWN = 15; // very fast
    private int counterAttackTimer = 0;
    private int chainCooldown = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.genesis_spark.idle");
    private static final RawAnimation DISCHARGE = RawAnimation.begin().thenPlay("animation.genesis_spark.discharge");
    private static final RawAnimation CHAIN = RawAnimation.begin().thenPlay("animation.genesis_spark.chain");
    private static final RawAnimation COUNTER = RawAnimation.begin().thenPlay("animation.genesis_spark.counter");

    public GenesisSparkEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Genesis Spark.
     * 20 HP, 0.1 speed (mostly static), 8 lightning damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_CHARGED, true);
    }

    public boolean isCharged() {
        return this.entityData.get(IS_CHARGED);
    }

    @Override
    protected void registerGoals() {
        // Mostly static - only attacks when players come close
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (counterAttackTimer > 0) counterAttackTimer--;
            if (chainCooldown > 0) chainCooldown--;

            // Chain lightning to nearby players standing on metal/water
            if (chainCooldown <= 0 && isCharged()) {
                attemptChainLightning();
            }

            // Check for powder snow nearby - discharges and becomes non-conductive
            if (this.level().getBlockState(this.blockPosition()).is(Blocks.POWDER_SNOW)) {
                this.entityData.set(IS_CHARGED, false);
            }

            // Lightning particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0 && isCharged()) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + 0.5, this.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    /**
     * Attempts chain lightning to nearby players on conductive surfaces.
     */
    private void attemptChainLightning() {
        AABB area = this.getBoundingBox().inflate(CHAIN_RANGE);
        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : nearbyPlayers) {
            double distance = this.distanceTo(player);
            if (distance <= CHAIN_RANGE) {
                // Check if player is on conductive surface (metal/water blocks)
                boolean onConductive = isOnConductiveSurface(player);
                boolean isWet = player.isInWaterOrRain();

                if (onConductive || isWet) {
                    float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    if (isWet) {
                        damage += WET_BONUS_DAMAGE;
                    }

                    // Check if player is blocking with wooden shield
                    if (player.isBlocking()) {
                        // Wooden shield blocks lightning
                        chainCooldown = 40;
                        return;
                    }

                    player.hurt(this.damageSources().lightningBolt(), damage);
                    chainCooldown = 60;

                    // Chain lightning particles
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.5, 0.3, 0.1);
                    }
                    this.level().playSound(null, this.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.5f, 2.0f);
                    break;
                }
            }
        }
    }

    /**
     * Checks if a player is standing on a conductive surface (iron blocks, water, etc.).
     */
    private boolean isOnConductiveSurface(Player player) {
        var blockBelow = this.level().getBlockState(player.blockPosition().below());
        return blockBelow.is(Blocks.IRON_BLOCK)
            || blockBelow.is(Blocks.COPPER_BLOCK)
            || blockBelow.is(Blocks.GOLD_BLOCK)
            || blockBelow.is(Blocks.WATER);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fast counter-attack when hit
        if (counterAttackTimer <= 0 && source.getEntity() instanceof LivingEntity attacker) {
            if (!attacker.isBlocking() && isCharged()) {
                float counterDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (attacker.isInWaterOrRain()) {
                    counterDamage += WET_BONUS_DAMAGE;
                }
                attacker.hurt(this.damageSources().lightningBolt(), counterDamage * 0.5f);
                counterAttackTimer = COUNTER_ATTACK_COOLDOWN;

                this.level().playSound(null, this.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8f, 1.5f);
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(DISCHARGE);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
