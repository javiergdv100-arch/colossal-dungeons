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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
 * Membrane Weaver - Hollow Leviathan dungeon elite creature.
 *
 * A large organic entity that seals passages with living membrane blocks, creating
 * barriers that players must destroy to progress. Also creates cocoons that periodically
 * spawn larvae and antibodies. When overwhelmed (taking too much damage), it flees
 * and re-seals passages behind it.
 *
 * Stats: 110 HP, 3 armor, 9 damage.
 * Behavior: Seals passages, spawns cocoons, flees when overwhelmed, re-seals.
 * Counter: Destroy membrane blocks and cocoons. Cornering prevents fleeing.
 */
public class MembraneWeaverEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Weaver behavioral states.
     */
    public enum WeaverState {
        PATROLLING,     // Moving through passages, placing membranes
        WEAVING,        // Actively creating a membrane/cocoon
        FIGHTING,       // Engaged in combat
        FLEEING,        // Running away and sealing behind
        RECOVERING      // Brief rest after fleeing
    }

    private static final EntityDataAccessor<Integer> WEAVER_STATE =
        SynchedEntityData.defineId(MembraneWeaverEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MEMBRANES_PLACED =
        SynchedEntityData.defineId(MembraneWeaverEntity.class, EntityDataSerializers.INT);

    private static final float FLEE_HEALTH_THRESHOLD = 0.5f; // Flees below 50% HP
    private static final int WEAVE_DURATION = 40; // 2 seconds to create membrane
    private static final int COCOON_SPAWN_INTERVAL = 200; // 10 seconds between cocoon spawns
    private static final int MAX_MEMBRANES = 6;
    private static final int FLEE_SEAL_INTERVAL = 30; // Places seal every 1.5s while fleeing
    private static final double FLEE_TRIGGER_DAMAGE_BURST = 20.0f; // Damage in short time triggers flee

    private int weaveTimer = 0;
    private int cocoonTimer = 0;
    private int fleeSealTimer = 0;
    private float recentDamage = 0.0f;
    private int recentDamageDecay = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.membrane_weaver.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.membrane_weaver.move");
    private static final RawAnimation WEAVE = RawAnimation.begin().thenPlay("animation.membrane_weaver.weave");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.membrane_weaver.attack");
    private static final RawAnimation FLEE_ANIM = RawAnimation.begin().thenLoop("animation.membrane_weaver.flee");
    private static final RawAnimation COCOON_CREATE = RawAnimation.begin().thenPlay("animation.membrane_weaver.cocoon_create");

    public MembraneWeaverEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Membrane Weaver.
     * 110 HP, 0.22 speed, 9 damage, 3 armor, 0.3 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 110.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WEAVER_STATE, WeaverState.PATROLLING.ordinal());
        builder.define(MEMBRANES_PLACED, 0);
    }

    public WeaverState getWeaverState() {
        return WeaverState.values()[this.entityData.get(WEAVER_STATE)];
    }

    private void setWeaverState(WeaverState state) {
        this.entityData.set(WEAVER_STATE, state.ordinal());
    }

    public int getMembranesPlaced() {
        return this.entityData.get(MEMBRANES_PLACED);
    }

    private void setMembranesPlaced(int count) {
        this.entityData.set(MEMBRANES_PLACED, count);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return getWeaverState() == WeaverState.FLEEING;
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Decay recent damage tracker
            if (recentDamageDecay > 0) {
                recentDamageDecay--;
                if (recentDamageDecay <= 0) {
                    recentDamage = 0;
                }
            }

            WeaverState state = getWeaverState();

            switch (state) {
                case PATROLLING -> {
                    // Periodically create cocoons
                    cocoonTimer--;
                    if (cocoonTimer <= 0 && getMembranesPlaced() < MAX_MEMBRANES) {
                        startWeaving();
                    }

                    // Check for threats
                    if (this.getTarget() != null) {
                        setWeaverState(WeaverState.FIGHTING);
                    }
                }
                case WEAVING -> {
                    weaveTimer--;
                    if (weaveTimer <= 0) {
                        completeMembrane();
                        setWeaverState(WeaverState.PATROLLING);
                        cocoonTimer = COCOON_SPAWN_INTERVAL;
                    }
                }
                case FIGHTING -> {
                    // Check if should flee
                    if (shouldFlee()) {
                        setWeaverState(WeaverState.FLEEING);
                        fleeSealTimer = 0;
                    }
                    if (this.getTarget() == null) {
                        setWeaverState(WeaverState.PATROLLING);
                    }
                }
                case FLEEING -> {
                    fleeSealTimer--;
                    if (fleeSealTimer <= 0) {
                        placeSealBehind();
                        fleeSealTimer = FLEE_SEAL_INTERVAL;
                    }

                    // Stop fleeing when far enough from threats
                    Player nearest = this.level().getNearestPlayer(this, 8.0);
                    if (nearest == null) {
                        setWeaverState(WeaverState.RECOVERING);
                        weaveTimer = 60; // 3 seconds recovery
                    }
                }
                case RECOVERING -> {
                    weaveTimer--;
                    if (weaveTimer <= 0) {
                        setWeaverState(WeaverState.PATROLLING);
                        recentDamage = 0;
                    }
                }
            }
        }
    }

    /**
     * Starts weaving a membrane or cocoon.
     */
    private void startWeaving() {
        setWeaverState(WeaverState.WEAVING);
        weaveTimer = WEAVE_DURATION;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    /**
     * Completes the membrane placement, creating a blocking structure.
     */
    private void completeMembrane() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = this.blockPosition();

        // Place membrane blocks (using cobweb as placeholder for living membrane)
        for (int dy = 0; dy < 3; dy++) {
            BlockPos membranePos = pos.above(dy).relative(this.getDirection());
            if (serverLevel.getBlockState(membranePos).isAir()) {
                serverLevel.setBlock(membranePos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }

        setMembranesPlaced(getMembranesPlaced() + 1);
        this.level().playSound(null, pos,
            SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    /**
     * Places a membrane seal behind while fleeing.
     */
    private void placeSealBehind() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        BlockPos behind = this.blockPosition().relative(this.getDirection().getOpposite());
        for (int dy = 0; dy < 3; dy++) {
            BlockPos sealPos = behind.above(dy);
            if (serverLevel.getBlockState(sealPos).isAir()) {
                serverLevel.setBlock(sealPos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Determines if the weaver should flee based on health and recent damage.
     */
    private boolean shouldFlee() {
        float healthFraction = this.getHealth() / this.getMaxHealth();
        return healthFraction < FLEE_HEALTH_THRESHOLD || recentDamage >= FLEE_TRIGGER_DAMAGE_BURST;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide()) {
            recentDamage += amount;
            recentDamageDecay = 60; // Reset decay timer
        }
        return result;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            WeaverState weaverState = getWeaverState();
            return switch (weaverState) {
                case WEAVING -> state.setAndContinue(WEAVE);
                case FLEEING -> state.setAndContinue(FLEE_ANIM);
                default -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging && getWeaverState() == WeaverState.FIGHTING) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
