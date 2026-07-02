package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Frozen Oracle - An elite ice seer from the Veiled Peak dungeon.
 *
 * Creates ice floor patches (slippery terrain). Prophecies apply Slowness and
 * reduced vision (Blindness). Vulnerable during long prophecy animation.
 * Honey blocks prevent ice sliding effect.
 *
 * Stats: 130 HP, 4 armor, 0.22 speed, 10 frost damage.
 */
public class FrozenOracleEntity extends CDEGeoEntity implements GeoEntity {

    public enum OracleState {
        IDLE,
        CASTING_ICE,
        PROPHESYING,
        ATTACKING
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(FrozenOracleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_VULNERABLE =
        SynchedEntityData.defineId(FrozenOracleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int ICE_CAST_COOLDOWN = 100; // 5 seconds
    private static final int ICE_RADIUS = 5;
    private static final int PROPHECY_DURATION = 80; // 4 seconds (vulnerable window)
    private static final int PROPHECY_COOLDOWN = 200; // 10 seconds
    private static final int SLOWNESS_DURATION = 100; // 5 seconds
    private static final int BLINDNESS_DURATION = 60; // 3 seconds
    private static final double PROPHECY_RANGE = 10.0;

    private int iceCastCooldown = 0;
    private int prophecyCooldown = 0;
    private int stateTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.frozen_oracle.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.frozen_oracle.walk");
    private static final RawAnimation CAST_ICE = RawAnimation.begin().thenPlay("animation.frozen_oracle.cast_ice");
    private static final RawAnimation PROPHECY = RawAnimation.begin().thenPlay("animation.frozen_oracle.prophecy");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.frozen_oracle.attack");

    public FrozenOracleEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Frozen Oracle.
     * 130 HP, 4 armor, 0.22 speed, 10 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 130.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, OracleState.IDLE.ordinal());
        builder.define(IS_VULNERABLE, false);
    }

    public OracleState getOracleState() {
        return OracleState.values()[this.entityData.get(STATE)];
    }

    private void setOracleState(OracleState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    public boolean isVulnerable() {
        return this.entityData.get(IS_VULNERABLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (iceCastCooldown > 0) iceCastCooldown--;
            if (prophecyCooldown > 0) prophecyCooldown--;

            OracleState currentState = getOracleState();

            switch (currentState) {
                case IDLE -> {
                    LivingEntity target = this.getTarget();
                    if (target != null) {
                        // Priority: Prophecy > Ice > Melee
                        if (prophecyCooldown <= 0 && this.distanceTo(target) < PROPHECY_RANGE) {
                            setOracleState(OracleState.PROPHESYING);
                            stateTimer = PROPHECY_DURATION;
                            this.entityData.set(IS_VULNERABLE, true);
                        } else if (iceCastCooldown <= 0) {
                            setOracleState(OracleState.CASTING_ICE);
                            stateTimer = 20;
                        }
                    }
                }
                case CASTING_ICE -> {
                    stateTimer--;
                    if (stateTimer <= 0) {
                        createIcePatches();
                        iceCastCooldown = ICE_CAST_COOLDOWN;
                        setOracleState(OracleState.IDLE);
                    }
                }
                case PROPHESYING -> {
                    stateTimer--;
                    // Vulnerable during prophecy - cannot move
                    this.setDeltaMovement(Vec3.ZERO);

                    // Apply debuffs at the midpoint of prophecy
                    if (stateTimer == PROPHECY_DURATION / 2) {
                        applyProphecyDebuffs();
                    }

                    if (stateTimer <= 0) {
                        this.entityData.set(IS_VULNERABLE, false);
                        prophecyCooldown = PROPHECY_COOLDOWN;
                        setOracleState(OracleState.IDLE);
                    }
                }
                case ATTACKING -> {
                    // Handled by MeleeAttackGoal
                    setOracleState(OracleState.IDLE);
                }
            }

            // Frost particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 8 == 0) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 1.5, this.getZ(), 3, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }

    /**
     * Creates ice floor patches around the Oracle's position.
     */
    private void createIcePatches() {
        BlockPos center = this.blockPosition();

        for (int x = -ICE_RADIUS; x <= ICE_RADIUS; x++) {
            for (int z = -ICE_RADIUS; z <= ICE_RADIUS; z++) {
                if (x * x + z * z > ICE_RADIUS * ICE_RADIUS) continue;
                if (this.random.nextFloat() > 0.4f) continue; // Sparse patches

                BlockPos pos = center.offset(x, -1, z);
                // Only replace solid non-special blocks with packed ice
                if (this.level().getBlockState(pos).isSolidRender(this.level(), pos) &&
                    !this.level().getBlockState(pos).is(Blocks.HONEY_BLOCK)) {
                    this.level().setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
                }
            }
        }

        // Ice creation sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.5f, 0.5f);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                this.getX(), this.getY(), this.getZ(), 30, ICE_RADIUS, 1.0, ICE_RADIUS, 0.02);
        }
    }

    /**
     * Applies prophecy debuffs (Slowness + Blindness) to nearby players.
     */
    private void applyProphecyDebuffs() {
        AABB area = this.getBoundingBox().inflate(PROPHECY_RANGE);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            // Honey blocks provide immunity to prophecy effects (grounding effect)
            BlockPos playerFeet = player.blockPosition().below();
            if (this.level().getBlockState(playerFeet).is(Blocks.HONEY_BLOCK)) {
                continue;
            }

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, 1));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION, 0));
        }

        // Prophecy sound
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 1.5f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            OracleState oracleState = getOracleState();
            return switch (oracleState) {
                case CASTING_ICE -> state.setAndContinue(CAST_ICE);
                case PROPHESYING -> state.setAndContinue(PROPHECY);
                default -> {
                    if (state.isMoving()) yield state.setAndContinue(WALK);
                    yield state.setAndContinue(IDLE);
                }
            };
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
