package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Reflection Twin - Mirror Castle elite that copies player movements/attacks.
 *
 * HP equals the target player's HP. Copies player's appearance, equipment,
 * and recent movements/attacks with a short delay. Linked to a specific mirror
 * surface; destroying or covering that mirror weakens or destroys the twin.
 *
 * Stats: HP = player's HP, variable armor, copies player's damage.
 * Size: 0.6 x 1.8 blocks (player-sized).
 *
 * Behavior:
 * - Imitates the player's last memorized movements.
 * - Linked to a specific mirror surface (BlockPos).
 * - Breaking or covering the linked mirror weakens it (-50% HP) or destroys it.
 * - Cannot copy shield blocking (player can use shield to counter).
 * - Spyglass reveals which mirror the twin is linked to (aura glow).
 *
 * Vanilla interactions:
 * - Water bucket: dampens the linked mirror, weakening the twin.
 * - Snowballs: break the linked fragile mirror in 1-2 hits.
 * - Shield: blocks copied attacks (twin cannot copy blocking).
 * - Spyglass: reveals linked mirror (glows with aura).
 */
public class ReflectionTwinEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_WEAKENED =
        SynchedEntityData.defineId(ReflectionTwinEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> MIRROR_HEALTH =
        SynchedEntityData.defineId(ReflectionTwinEntity.class, EntityDataSerializers.INT);

    // The mirror surface this twin is linked to
    private BlockPos linkedMirrorPos = null;
    private static final int MAX_MIRROR_HEALTH = 3; // Snowball hits to break

    // Movement copying - stores recent player positions with delay
    private static final int COPY_DELAY_TICKS = 15; // ~0.75 second delay
    private final Queue<Vec3> playerMovementQueue = new LinkedList<>();
    private final Queue<Boolean> playerAttackQueue = new LinkedList<>();

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.reflection_twin.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.reflection_twin.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.reflection_twin.attack");
    private static final RawAnimation WEAKEN = RawAnimation.begin().thenPlay("animation.reflection_twin.weaken");
    private static final RawAnimation SHATTER = RawAnimation.begin().thenPlay("animation.reflection_twin.shatter");

    public ReflectionTwinEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Reflection Twin.
     * Base 20 HP (adjusted to match player at spawn), 0.3 speed, 5 base damage.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0) // Adjusted dynamically to player HP
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 5.0) // Adjusted to match player weapon
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_WEAKENED, false);
        builder.define(MIRROR_HEALTH, MAX_MIRROR_HEALTH);
    }

    public boolean isWeakened() {
        return this.entityData.get(IS_WEAKENED);
    }

    private void setWeakened(boolean weakened) {
        this.entityData.set(IS_WEAKENED, weakened);
    }

    /**
     * Sets the linked mirror position and adjusts HP to match the player.
     */
    public void initializeForPlayer(Player player, BlockPos mirrorPos) {
        this.linkedMirrorPos = mirrorPos;
        this.entityData.set(MIRROR_HEALTH, MAX_MIRROR_HEALTH);

        // Match player's health
        float playerMaxHealth = player.getMaxHealth();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(playerMaxHealth);
        this.setHealth(playerMaxHealth);

        // Match player's attack damage (approximate from weapon)
        float playerDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(playerDamage);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Check if linked mirror still exists
            if (linkedMirrorPos != null) {
                checkMirrorIntegrity();
            }

            // Copy player movements with delay
            LivingEntity target = this.getTarget();
            if (target instanceof Player player) {
                // Record player position
                playerMovementQueue.add(player.position());
                playerAttackQueue.add(player.swinging);

                // Replay delayed movements
                if (playerMovementQueue.size() > COPY_DELAY_TICKS) {
                    Vec3 delayedPos = playerMovementQueue.poll();
                    Boolean delayedAttack = playerAttackQueue.poll();

                    if (delayedPos != null) {
                        // Mirror the player's relative movement
                        Vec3 moveDir = delayedPos.subtract(this.position()).normalize().scale(0.15);
                        this.setDeltaMovement(moveDir.x, this.getDeltaMovement().y, moveDir.z);
                    }

                    if (delayedAttack != null && delayedAttack) {
                        // Attempt attack if close enough
                        if (target.distanceTo(this) < 3.0) {
                            this.doHurtTarget(target);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the linked mirror block is still present and intact.
     */
    private void checkMirrorIntegrity() {
        if (linkedMirrorPos == null) return;

        int mirrorHp = this.entityData.get(MIRROR_HEALTH);
        if (mirrorHp <= 0) {
            // Mirror is destroyed - twin dies
            this.hurt(this.damageSources().magic(), this.getMaxHealth());
        }
    }

    /**
     * Called when the linked mirror is hit (by snowball, projectile, etc.)
     */
    public void onMirrorDamaged(int damage) {
        int current = this.entityData.get(MIRROR_HEALTH);
        int newHealth = Math.max(0, current - damage);
        this.entityData.set(MIRROR_HEALTH, newHealth);

        if (newHealth <= 0) {
            // Mirror destroyed
            setWeakened(true);
            this.setHealth(0);
        } else if (newHealth <= 1) {
            setWeakened(true);
            // Halve HP when mirror is damaged
            this.setHealth(this.getHealth() * 0.5f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // If the twin is hit from the front (Law of Reflection), reflect damage to attacker
        if (source.getEntity() instanceof LivingEntity attacker) {
            Vec3 attackDir = attacker.position().subtract(this.position()).normalize();
            Vec3 lookDir = this.getLookAngle();
            double dot = attackDir.dot(lookDir);

            // Frontal hit detection (dot product > 0.5 means roughly facing)
            if (dot > 0.5 && !isWeakened()) {
                // Reflect damage back to attacker (Law of Reflection)
                attacker.hurt(this.damageSources().magic(), amount * 0.5f);
                return false; // Twin takes no damage from frontal hits
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isWeakened()) {
                return state.setAndContinue(WEAKEN);
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
