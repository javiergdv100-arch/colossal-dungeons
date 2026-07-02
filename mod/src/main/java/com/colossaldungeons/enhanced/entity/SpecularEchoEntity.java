package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Specular Echo - Delayed copy that repeats player's last action with 1-second delay.
 *
 * A deferred copy that reproduces the player's last action after a one-second delay.
 * Has no attacks of its own - only repeats. Teaches the player to control their actions
 * within the dungeon. Neutralized by doing nothing (standing still with shield up).
 *
 * Stats: 18 HP, 0 armor, damage = mirrors player's last attack.
 * Size: 0.6 x 1.8 blocks (player-sized translucent figure).
 *
 * Behavior:
 * - Reproduces with delay the last strike or ability used by the player.
 * - Does not initiate any actions on its own - only repeats.
 * - Neutralized by standing still long enough (no action to copy).
 * - Shield: raising shield and not attacking neutralizes it.
 * - Snowballs/Eggs: throwing these gives it something harmless to copy.
 *
 * Drops: Echo Fragment (sonic crafting component).
 */
public class SpecularEchoEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Echo states.
     */
    public enum EchoState {
        WAITING,       // Waiting for player action to copy
        COPYING,       // Executing a copied action
        NEUTRALIZED    // Player did nothing - echo fades
    }

    private static final EntityDataAccessor<Integer> ECHO_STATE =
        SynchedEntityData.defineId(SpecularEchoEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> COPIED_DAMAGE =
        SynchedEntityData.defineId(SpecularEchoEntity.class, EntityDataSerializers.FLOAT);

    private static final int COPY_DELAY_TICKS = 20; // 1 second delay
    private static final int INACTION_THRESHOLD = 100; // 5 seconds of inaction neutralizes
    private static final int COPY_EXECUTION_TICKS = 10; // Time to execute copied action

    // Queue to track player actions with timestamps
    private final Queue<PlayerAction> actionQueue = new LinkedList<>();
    private int inactionTimer = 0;
    private int copyExecutionTimer = 0;
    private float lastCopiedDamage = 0.0f;
    private Vec3 copyTargetPos = null;

    /**
     * Record of a player action to be copied.
     */
    private record PlayerAction(long gameTime, float damage, Vec3 position, boolean isHarmless) {}

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.specular_echo.idle");
    private static final RawAnimation COPY_ATTACK = RawAnimation.begin().thenPlay("animation.specular_echo.copy_attack");
    private static final RawAnimation COPY_HARMLESS = RawAnimation.begin().thenPlay("animation.specular_echo.copy_harmless");
    private static final RawAnimation FADE = RawAnimation.begin().thenPlay("animation.specular_echo.fade");
    private static final RawAnimation WAITING_ANIM = RawAnimation.begin().thenLoop("animation.specular_echo.waiting");

    public SpecularEchoEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Specular Echo.
     * 18 HP, 0 armor, 0 base damage (copies player's damage), 0.3 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 0.0) // No own attack - copies player
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ECHO_STATE, EchoState.WAITING.ordinal());
        builder.define(COPIED_DAMAGE, 0.0f);
    }

    public EchoState getEchoState() {
        return EchoState.values()[this.entityData.get(ECHO_STATE)];
    }

    private void setEchoState(EchoState state) {
        this.entityData.set(ECHO_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // No attack goals - this entity does not initiate attacks
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            EchoState currentState = getEchoState();

            if (currentState == EchoState.NEUTRALIZED) {
                // Fading away
                this.discard();
                return;
            }

            LivingEntity target = this.getTarget();
            if (target instanceof Player player) {
                // Monitor player actions
                boolean playerActed = player.swinging || player.getAttackStrengthScale(0) < 1.0f;

                if (playerActed) {
                    // Record player action
                    float playerDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    boolean isHarmless = false; // Could detect snowball/egg throws
                    actionQueue.add(new PlayerAction(
                        this.level().getGameTime(), playerDamage, player.position(), isHarmless));
                    inactionTimer = 0;
                } else {
                    inactionTimer++;
                }

                // Check for neutralization (player does nothing long enough)
                if (inactionTimer >= INACTION_THRESHOLD) {
                    setEchoState(EchoState.NEUTRALIZED);
                    return;
                }

                // Process delayed actions
                if (!actionQueue.isEmpty()) {
                    PlayerAction oldest = actionQueue.peek();
                    long currentTime = this.level().getGameTime();
                    if (currentTime - oldest.gameTime() >= COPY_DELAY_TICKS) {
                        actionQueue.poll();
                        executeCopiedAction(oldest, player);
                    }
                }
            }

            // Handle copy execution
            if (currentState == EchoState.COPYING) {
                copyExecutionTimer--;
                if (copyExecutionTimer <= 0) {
                    setEchoState(EchoState.WAITING);
                }
            }
        }
    }

    /**
     * Executes a copied player action after the delay.
     */
    private void executeCopiedAction(PlayerAction action, Player player) {
        setEchoState(EchoState.COPYING);
        copyExecutionTimer = COPY_EXECUTION_TICKS;

        if (action.isHarmless()) {
            // Harmless action - just animate
            this.entityData.set(COPIED_DAMAGE, 0.0f);
            return;
        }

        // Deal damage equal to what the player dealt
        lastCopiedDamage = action.damage();
        this.entityData.set(COPIED_DAMAGE, lastCopiedDamage);

        // Deal damage to the player (the echo attacks the source)
        if (this.distanceTo(player) <= 4.0) {
            DamageSource source = this.damageSources().mobAttack(this);
            player.hurt(source, lastCopiedDamage);
        }
    }

    /**
     * Called externally when the player throws a snowball or egg near this entity.
     * Records it as a harmless action for the echo to copy.
     */
    public void onHarmlessActionNearby(Player player) {
        actionQueue.add(new PlayerAction(
            this.level().getGameTime(), 0.0f, player.position(), true));
        inactionTimer = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            EchoState echoState = getEchoState();
            return switch (echoState) {
                case WAITING -> state.setAndContinue(WAITING_ANIM);
                case COPYING -> {
                    float dmg = this.entityData.get(COPIED_DAMAGE);
                    if (dmg <= 0.0f) {
                        yield state.setAndContinue(COPY_HARMLESS);
                    }
                    yield state.setAndContinue(COPY_ATTACK);
                }
                case NEUTRALIZED -> state.setAndContinue(FADE);
            };
        }));
    }
}
