package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Frame Sentinel - Animated mirror frame that patrols halls.
 *
 * Projects false doors and windows to disorient players. Its crystal is its
 * weak point, breakable with snowball hits. The metallic border resists damage.
 *
 * Stats: 40 HP, 6 armor, 7 damage.
 * Size: 1.2 x 2.0 blocks.
 *
 * Behavior:
 * - Patrols corridors projecting illusory openings (false doors/windows).
 * - Blocks passage by turning its frame as a solid hitbox.
 * - Crystal weak point: 3 snowball hits to break, disabling it.
 * - Metallic border resists normal damage well.
 * - Crossbow bolt to the crystal breaks it in one hit from distance.
 * - Fishing rod can hook and rotate the frame, disrupting its projection.
 *
 * Drops: Silver frame (decorative craftable), Intact crystal.
 */
public class FrameSentinelEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Sentinel states.
     */
    public enum SentinelState {
        PATROLLING,     // Normal patrol - projecting illusions
        BLOCKING,       // Turned to block a passage
        ATTACKING,      // Engaging in melee
        DISABLED        // Crystal broken - inert
    }

    private static final EntityDataAccessor<Integer> SENTINEL_STATE =
        SynchedEntityData.defineId(FrameSentinelEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CRYSTAL_HITS =
        SynchedEntityData.defineId(FrameSentinelEntity.class, EntityDataSerializers.INT);

    private static final int CRYSTAL_MAX_HITS = 3; // Snowball hits to break crystal
    private static final float REDUCED_DAMAGE_MULTIPLIER = 0.3f; // Damage reduction from metallic border

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.frame_sentinel.idle");
    private static final RawAnimation PATROL = RawAnimation.begin().thenLoop("animation.frame_sentinel.patrol");
    private static final RawAnimation BLOCK_ANIM = RawAnimation.begin().thenPlay("animation.frame_sentinel.block");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.frame_sentinel.attack");
    private static final RawAnimation PROJECT_ILLUSION = RawAnimation.begin().thenPlay("animation.frame_sentinel.project");
    private static final RawAnimation DISABLED_ANIM = RawAnimation.begin().thenLoop("animation.frame_sentinel.disabled");
    private static final RawAnimation CRYSTAL_BREAK = RawAnimation.begin().thenPlay("animation.frame_sentinel.crystal_break");

    public FrameSentinelEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Frame Sentinel.
     * 40 HP, 6 armor, 7 damage, 0.22 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SENTINEL_STATE, SentinelState.PATROLLING.ordinal());
        builder.define(CRYSTAL_HITS, 0);
    }

    public SentinelState getSentinelState() {
        return SentinelState.values()[this.entityData.get(SENTINEL_STATE)];
    }

    private void setSentinelState(SentinelState state) {
        this.entityData.set(SENTINEL_STATE, state.ordinal());
    }

    public int getCrystalHits() {
        return this.entityData.get(CRYSTAL_HITS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return getSentinelState() != SentinelState.DISABLED && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return getSentinelState() == SentinelState.PATROLLING && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return getSentinelState() != SentinelState.DISABLED && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (getSentinelState() == SentinelState.DISABLED) {
                // No AI when disabled
                this.setTarget(null);
                return;
            }

            // Determine state based on target proximity
            Player target = this.level().getNearestPlayer(this, 16.0);
            if (target != null && this.getTarget() == null) {
                double dist = this.distanceTo(target);
                if (dist < 3.0) {
                    setSentinelState(SentinelState.BLOCKING);
                } else if (dist < 8.0) {
                    setSentinelState(SentinelState.ATTACKING);
                }
            } else if (this.getTarget() == null) {
                setSentinelState(SentinelState.PATROLLING);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getSentinelState() == SentinelState.DISABLED) {
            return false; // Inert - cannot take more damage
        }

        // Snowball hits the crystal
        if (source.getDirectEntity() instanceof Snowball) {
            return handleCrystalHit();
        }

        // Crossbow bolts break the crystal in one hit (indirect + high damage)
        if (source.isIndirect() && amount >= 5.0f) {
            // Crossbow bolt to crystal - instant break
            breakCrystal();
            return true;
        }

        // Normal damage is reduced by the metallic border
        float reducedDamage = amount * REDUCED_DAMAGE_MULTIPLIER;
        return super.hurt(source, reducedDamage);
    }

    /**
     * Handles a snowball hitting the crystal weak point.
     */
    private boolean handleCrystalHit() {
        int hits = this.entityData.get(CRYSTAL_HITS) + 1;
        this.entityData.set(CRYSTAL_HITS, hits);

        if (hits >= CRYSTAL_MAX_HITS) {
            breakCrystal();
        }
        return true;
    }

    /**
     * Breaks the crystal, disabling the sentinel permanently.
     */
    private void breakCrystal() {
        setSentinelState(SentinelState.DISABLED);
        this.setTarget(null);
        // Play crystal break sound
        this.level().playSound(null, this.blockPosition(),
            net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 1.2f);
        // Remove most health to make it easy to finish off
        this.setHealth(1.0f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            SentinelState sentinelState = getSentinelState();
            return switch (sentinelState) {
                case PATROLLING -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(PATROL);
                    }
                    yield state.setAndContinue(IDLE);
                }
                case BLOCKING -> state.setAndContinue(BLOCK_ANIM);
                case ATTACKING -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(PATROL);
                    }
                    yield state.setAndContinue(IDLE);
                }
                case DISABLED -> state.setAndContinue(DISABLED_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && getSentinelState() == SentinelState.ATTACKING) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
