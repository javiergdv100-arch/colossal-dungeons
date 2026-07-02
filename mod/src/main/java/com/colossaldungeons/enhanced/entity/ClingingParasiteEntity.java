package com.colossaldungeons.enhanced.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Clinging Parasite - Hollow Leviathan dungeon creature.
 *
 * A ceiling-dwelling leech that waits motionless until a player passes below, then drops
 * to attach itself. While attached, drains 1 HP per second from the host. Torches placed
 * on the ceiling in the area prevent the parasite from dropping. Water contact detaches
 * the parasite instantly.
 *
 * Stats: 16 HP, 4 damage on initial drop.
 * Behavior: Wait on ceiling -> Drop -> Attach -> Drain -> Detach if water/killed.
 */
public class ClingingParasiteEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Parasite behavioral states.
     */
    public enum ParasiteState {
        CEILING_WAIT,   // Waiting on ceiling for prey
        DROPPING,       // Falling toward target
        ATTACHED,       // Attached to a player, draining HP
        DETACHED        // On the ground after being removed
    }

    private static final EntityDataAccessor<Integer> PARASITE_STATE =
        SynchedEntityData.defineId(ClingingParasiteEntity.class, EntityDataSerializers.INT);

    private static final double DROP_TRIGGER_RANGE = 2.0; // Player must be within 2 blocks below
    private static final float DROP_DAMAGE = 4.0f;
    private static final float DRAIN_DAMAGE_PER_SECOND = 1.0f;
    private static final int DRAIN_INTERVAL = 20; // 1 second
    private static final double TORCH_PREVENTION_RADIUS = 4.0;

    private int drainTimer = 0;
    private LivingEntity attachedTarget = null;
    private int detachedCooldown = 0;

    private static final RawAnimation CEILING_IDLE = RawAnimation.begin().thenLoop("animation.clinging_parasite.ceiling_idle");
    private static final RawAnimation DROP_ANIM = RawAnimation.begin().thenPlay("animation.clinging_parasite.drop");
    private static final RawAnimation ATTACH = RawAnimation.begin().thenLoop("animation.clinging_parasite.attach");
    private static final RawAnimation DRAIN = RawAnimation.begin().thenLoop("animation.clinging_parasite.drain");
    private static final RawAnimation DETACHED_IDLE = RawAnimation.begin().thenLoop("animation.clinging_parasite.detached_idle");

    public ClingingParasiteEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Clings to ceiling
    }

    /**
     * Creates the attribute supplier for Clinging Parasite.
     * 16 HP, 0.2 speed (when detached), 4 damage, 0 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATE, ParasiteState.CEILING_WAIT.ordinal());
    }

    public ParasiteState getParasiteState() {
        return ParasiteState.values()[this.entityData.get(PARASITE_STATE)];
    }

    private void setParasiteState(ParasiteState state) {
        this.entityData.set(PARASITE_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            ParasiteState state = getParasiteState();

            switch (state) {
                case CEILING_WAIT -> {
                    // Check for players below and absence of torches
                    if (!isTorchNearby()) {
                        Player nearest = this.level().getNearestPlayer(this, DROP_TRIGGER_RANGE);
                        if (nearest != null && nearest.getY() < this.getY() - 1.0) {
                            // Drop on the player
                            setParasiteState(ParasiteState.DROPPING);
                            this.setNoGravity(false);
                        }
                    }
                }
                case DROPPING -> {
                    // Falling - check if reached a player or the ground
                    if (this.onGround()) {
                        setParasiteState(ParasiteState.DETACHED);
                        detachedCooldown = 40;
                    } else {
                        // Check collision with players while falling
                        Player nearest = this.level().getNearestPlayer(this, 1.0);
                        if (nearest != null) {
                            attachToTarget(nearest);
                        }
                    }
                }
                case ATTACHED -> {
                    if (attachedTarget == null || attachedTarget.isDeadOrDying() || attachedTarget.isRemoved()) {
                        detach();
                        break;
                    }

                    // Check for water detachment
                    if (this.isInWaterOrBubble() || attachedTarget.isInWaterOrBubble()) {
                        detach();
                        break;
                    }

                    // Follow attached target position
                    this.teleportTo(
                        attachedTarget.getX(),
                        attachedTarget.getY() + attachedTarget.getBbHeight() * 0.8,
                        attachedTarget.getZ());

                    // Drain HP periodically
                    drainTimer++;
                    if (drainTimer >= DRAIN_INTERVAL) {
                        drainTimer = 0;
                        attachedTarget.hurt(this.damageSources().mobAttack(this), DRAIN_DAMAGE_PER_SECOND);
                    }
                }
                case DETACHED -> {
                    this.setNoGravity(false);
                    detachedCooldown--;
                    // Vulnerable state - try to flee or re-attach
                }
            }
        }
    }

    /**
     * Checks if there are torches on the ceiling near this parasite that would prevent dropping.
     */
    private boolean isTorchNearby() {
        BlockPos pos = this.blockPosition();
        int radius = (int) TORCH_PREVENTION_RADIUS;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (this.level().getBlockState(checkPos).getBlock() instanceof TorchBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Attaches to a target, dealing initial drop damage.
     */
    private void attachToTarget(LivingEntity target) {
        setParasiteState(ParasiteState.ATTACHED);
        attachedTarget = target;
        this.setNoGravity(true);
        drainTimer = 0;

        // Initial drop damage
        target.hurt(this.damageSources().mobAttack(this), DROP_DAMAGE);
        this.level().playSound(null, target.blockPosition(),
            SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    /**
     * Detaches from the current target (water or forced removal).
     */
    private void detach() {
        setParasiteState(ParasiteState.DETACHED);
        attachedTarget = null;
        this.setNoGravity(false);
        detachedCooldown = 60;
        this.level().playSound(null, this.blockPosition(),
            SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 0.8f, 0.8f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        // Detach when taking damage while attached
        if (result && getParasiteState() == ParasiteState.ATTACHED) {
            if (amount >= 3.0f) { // Only detach from significant hits
                detach();
            }
        }
        return result;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            ParasiteState parasiteState = getParasiteState();
            return switch (parasiteState) {
                case CEILING_WAIT -> state.setAndContinue(CEILING_IDLE);
                case DROPPING -> state.setAndContinue(DROP_ANIM);
                case ATTACHED -> state.setAndContinue(DRAIN);
                case DETACHED -> state.setAndContinue(DETACHED_IDLE);
            };
        }));
    }
}
