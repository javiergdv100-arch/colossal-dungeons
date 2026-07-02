package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Veiled Soul - A ghost entity from the Veiled Peak dungeon.
 *
 * Invisible during blizzard conditions, materializes to attack players.
 * Beacon or torch light within radius permanently reveals it.
 * Can pass through obstacles partially (no-clip on certain blocks).
 *
 * Stats: 18 HP, 0.32 speed, 5 attack damage.
 */
public class VeiledSoulEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_REVEALED =
        SynchedEntityData.defineId(VeiledSoulEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_MATERIALIZED =
        SynchedEntityData.defineId(VeiledSoulEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double LIGHT_REVEAL_RADIUS = 8.0;
    private static final int LIGHT_LEVEL_THRESHOLD = 10;
    private static final int MATERIALIZE_RANGE = 5;

    private int materializeTimer = 0;
    private static final int MATERIALIZE_DURATION = 20; // 1 second to materialize

    private static final RawAnimation IDLE_INVISIBLE = RawAnimation.begin().thenLoop("animation.veiled_soul.idle_invisible");
    private static final RawAnimation IDLE_VISIBLE = RawAnimation.begin().thenLoop("animation.veiled_soul.idle_visible");
    private static final RawAnimation MATERIALIZE = RawAnimation.begin().thenPlay("animation.veiled_soul.materialize");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.veiled_soul.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.veiled_soul.attack");

    public VeiledSoulEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // Partial no-clip for passing through obstacles
    }

    /**
     * Creates the attribute supplier for the Veiled Soul.
     * 18 HP, 0.32 speed, 5 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_REVEALED, false);
        builder.define(IS_MATERIALIZED, false);
    }

    public boolean isRevealed() {
        return this.entityData.get(IS_REVEALED);
    }

    public boolean isMaterialized() {
        return this.entityData.get(IS_MATERIALIZED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Check if light source reveals this entity permanently
            if (!isRevealed()) {
                checkLightReveal();
            }

            // Handle materialization before attacking
            Player target = this.level().getNearestPlayer(this, MATERIALIZE_RANGE);
            if (target != null && !isMaterialized()) {
                materializeTimer++;
                if (materializeTimer >= MATERIALIZE_DURATION) {
                    this.entityData.set(IS_MATERIALIZED, true);
                    this.noPhysics = false;
                }
            }

            // De-materialize if no target nearby and not revealed
            if (target == null && isMaterialized() && !isRevealed()) {
                this.entityData.set(IS_MATERIALIZED, false);
                this.noPhysics = true;
                materializeTimer = 0;
            }

            // Spawn ghostly particles
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 10 == 0) {
                if (isMaterialized() || isRevealed()) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }
    }

    /**
     * Checks if a beacon or torch light source reveals this soul permanently.
     */
    private void checkLightReveal() {
        BlockPos pos = this.blockPosition();
        int lightLevel = this.level().getBrightness(LightLayer.BLOCK, pos);

        if (lightLevel >= LIGHT_LEVEL_THRESHOLD) {
            this.entityData.set(IS_REVEALED, true);
            this.entityData.set(IS_MATERIALIZED, true);
            this.noPhysics = false;
        }
    }

    @Override
    public boolean isInvisible() {
        // Invisible when not revealed and not materialized
        if (!isRevealed() && !isMaterialized()) {
            return true;
        }
        return super.isInvisible();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Cannot be hurt while invisible/non-materialized
        if (!isMaterialized() && !isRevealed()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (!isMaterialized() && !isRevealed()) {
                return state.setAndContinue(IDLE_INVISIBLE);
            }
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE_VISIBLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && isMaterialized()) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE_VISIBLE);
        }));
    }
}
