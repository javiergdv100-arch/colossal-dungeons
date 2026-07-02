package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Faceless Pilgrim - A deceptive entity from the Descent into Madness dungeon.
 *
 * Copies player/NPC appearance and blends in until attacking by surprise.
 * Food offering pre-attack pacifies it. On death reveals empty face.
 *
 * Stats: 24 HP, 1 armor, 0.28 speed, 6 attack damage.
 */
public class FacelessPilgrimEntity extends CDEGeoEntity implements GeoEntity {

    public enum PilgrimState {
        DISGUISED,
        APPROACHING,
        ATTACKING,
        PACIFIED,
        DYING_REVEAL
    }

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(FacelessPilgrimEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> FACE_REVEALED =
        SynchedEntityData.defineId(FacelessPilgrimEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double APPROACH_RANGE = 12.0;
    private static final double SURPRISE_ATTACK_RANGE = 2.5;
    private static final float SURPRISE_DAMAGE_MULTIPLIER = 2.0f;
    private static final int PACIFY_DURATION = 600; // 30 seconds

    private int pacifyTimer = 0;
    private boolean hasAttacked = false;

    private static final RawAnimation DISGUISE_IDLE = RawAnimation.begin().thenLoop("animation.faceless_pilgrim.disguise_idle");
    private static final RawAnimation DISGUISE_WALK = RawAnimation.begin().thenLoop("animation.faceless_pilgrim.disguise_walk");
    private static final RawAnimation SURPRISE_ATTACK = RawAnimation.begin().thenPlay("animation.faceless_pilgrim.surprise_attack");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.faceless_pilgrim.attack");
    private static final RawAnimation PACIFIED_IDLE = RawAnimation.begin().thenLoop("animation.faceless_pilgrim.pacified");
    private static final RawAnimation FACE_REVEAL = RawAnimation.begin().thenPlay("animation.faceless_pilgrim.face_reveal");

    public FacelessPilgrimEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Faceless Pilgrim.
     * 24 HP, 1 armor, 0.28 speed, 6 damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, PilgrimState.DISGUISED.ordinal());
        builder.define(FACE_REVEALED, false);
    }

    public PilgrimState getPilgrimState() {
        return PilgrimState.values()[this.entityData.get(STATE)];
    }

    private void setPilgrimState(PilgrimState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            PilgrimState currentState = getPilgrimState();

            switch (currentState) {
                case DISGUISED -> {
                    // Wait for player to get close, then approach
                    Player nearest = this.level().getNearestPlayer(this, APPROACH_RANGE);
                    if (nearest != null) {
                        setPilgrimState(PilgrimState.APPROACHING);
                    }
                }
                case APPROACHING -> {
                    // Move toward player, surprise attack when close enough
                    Player target = this.level().getNearestPlayer(this, APPROACH_RANGE);
                    if (target != null && this.distanceTo(target) < SURPRISE_ATTACK_RANGE && !hasAttacked) {
                        // Surprise attack!
                        hasAttacked = true;
                        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * SURPRISE_DAMAGE_MULTIPLIER;
                        target.hurt(this.damageSources().mobAttack(this), damage);
                        setPilgrimState(PilgrimState.ATTACKING);

                        this.level().playSound(null, this.blockPosition(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5f, 0.5f);
                    }
                }
                case ATTACKING -> {
                    // Normal combat
                }
                case PACIFIED -> {
                    pacifyTimer--;
                    if (pacifyTimer <= 0) {
                        setPilgrimState(PilgrimState.DISGUISED);
                        hasAttacked = false;
                    }
                }
                case DYING_REVEAL -> {
                    // Showing empty face
                }
            }
        }
    }

    /**
     * Food offering pacifies the pilgrim before it attacks.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        PilgrimState state = getPilgrimState();

        // Food offering pacifies before attacking
        if ((state == PilgrimState.DISGUISED || state == PilgrimState.APPROACHING)
            && held.is(Items.BREAD) || held.is(Items.COOKED_BEEF) || held.is(Items.APPLE)
            || held.is(Items.GOLDEN_APPLE) || held.is(Items.COOKIE)) {

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }

            setPilgrimState(PilgrimState.PACIFIED);
            pacifyTimer = PACIFY_DURATION;
            this.setTarget(null);

            this.level().playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void tickDeath() {
        // Reveal empty face on death
        if (!this.entityData.get(FACE_REVEALED)) {
            this.entityData.set(FACE_REVEALED, true);
            setPilgrimState(PilgrimState.DYING_REVEAL);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        ++this.deathTime;
        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isNoAi() {
        PilgrimState state = getPilgrimState();
        return state == PilgrimState.PACIFIED || state == PilgrimState.DYING_REVEAL || super.isNoAi();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            PilgrimState pilgrimState = getPilgrimState();
            return switch (pilgrimState) {
                case DISGUISED, APPROACHING -> {
                    if (state.isMoving()) yield state.setAndContinue(DISGUISE_WALK);
                    yield state.setAndContinue(DISGUISE_IDLE);
                }
                case PACIFIED -> state.setAndContinue(PACIFIED_IDLE);
                case DYING_REVEAL -> state.setAndContinue(FACE_REVEAL);
                default -> state.setAndContinue(DISGUISE_IDLE);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                if (!hasAttacked) {
                    return state.setAndContinue(SURPRISE_ATTACK);
                }
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(DISGUISE_IDLE);
        }));
    }
}
