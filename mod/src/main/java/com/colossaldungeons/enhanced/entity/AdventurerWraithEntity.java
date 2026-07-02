package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Adventurer's Wraith - Ghost that appears at locations where adventurers died.
 *
 * A rare apparition that loops through its final moments and attacks anyone
 * who disturbs its memory. Not immediately hostile - gives time to retreat.
 *
 * Stats: 90 HP, 2 armor, 7 damage, spectral nature.
 * Size: 0.6 x 1.8 blocks (humanoid ghost).
 *
 * Behavior:
 * - Appears near remains of previous deaths (death point spawning).
 * - Repeats last moments in a loop animation before becoming aware.
 * - Not immediately hostile: gives a warning before attacking.
 * - Can be pacified with food (memory of last meal).
 * - On defeat or pacification, reveals loot hints (hidden passages, chests).
 * - Light weakens it (-30% damage from torches nearby).
 * - Milk clears its debuff effects (chill on contact).
 * - Note blocks with specific melody calm it instantly.
 *
 * Drops: Memory Fragment (map/secret hint).
 */
public class AdventurerWraithEntity extends CDEGeoEntity implements GeoEntity {

    /**
     * Wraith behavioral states.
     */
    public enum WraithState {
        LOOPING,       // Repeating death loop - not aware of players
        WARNING,       // Has noticed a player - giving warning before attacking
        HOSTILE,       // Actively attacking
        PACIFIED       // Calmed by food or note blocks - reveals loot hints
    }

    private static final EntityDataAccessor<Integer> WRAITH_STATE =
        SynchedEntityData.defineId(AdventurerWraithEntity.class, EntityDataSerializers.INT);

    private static final double AWARENESS_RANGE = 8.0;
    private static final double WARNING_RANGE = 5.0;
    private static final int WARNING_DURATION = 60; // 3 seconds of warning
    private static final int PACIFIED_DURATION = 200; // 10 seconds of hint display
    private static final float LIGHT_DAMAGE_REDUCTION = 0.7f;

    private int warningTimer = 0;
    private int pacifiedTimer = 0;
    private boolean hasRevealedHint = false;

    // Animations
    private static final RawAnimation LOOP_ANIM = RawAnimation.begin().thenLoop("animation.adventurer_wraith.loop");
    private static final RawAnimation WARNING_ANIM = RawAnimation.begin().thenPlay("animation.adventurer_wraith.warning");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.adventurer_wraith.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.adventurer_wraith.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.adventurer_wraith.attack");
    private static final RawAnimation PACIFIED_ANIM = RawAnimation.begin().thenLoop("animation.adventurer_wraith.pacified");
    private static final RawAnimation FADE_ANIM = RawAnimation.begin().thenPlay("animation.adventurer_wraith.fade");

    public AdventurerWraithEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Ghost floats
    }

    /**
     * Creates the attribute supplier for the Adventurer's Wraith.
     * 90 HP, 2 armor, 7 damage, 0.25 speed.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 90.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WRAITH_STATE, WraithState.LOOPING.ordinal());
    }

    public WraithState getWraithState() {
        return WraithState.values()[this.entityData.get(WRAITH_STATE)];
    }

    private void setWraithState(WraithState state) {
        this.entityData.set(WRAITH_STATE, state.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return getWraithState() == WraithState.HOSTILE && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return getWraithState() == WraithState.HOSTILE && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return getWraithState() == WraithState.HOSTILE && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            WraithState currentState = getWraithState();

            switch (currentState) {
                case LOOPING -> {
                    // Check if a player disturbs the wraith by getting too close
                    Player nearest = this.level().getNearestPlayer(this, AWARENESS_RANGE);
                    if (nearest != null) {
                        double dist = this.distanceTo(nearest);
                        if (dist < WARNING_RANGE) {
                            setWraithState(WraithState.WARNING);
                            warningTimer = WARNING_DURATION;
                        }
                    }
                }
                case WARNING -> {
                    warningTimer--;
                    if (warningTimer <= 0) {
                        // Player did not retreat - become hostile
                        Player nearest = this.level().getNearestPlayer(this, WARNING_RANGE);
                        if (nearest != null) {
                            setWraithState(WraithState.HOSTILE);
                        } else {
                            // Player retreated - go back to looping
                            setWraithState(WraithState.LOOPING);
                        }
                    }
                }
                case HOSTILE -> {
                    // Apply chill debuff on contact
                    if (this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.0) {
                        if (this.getTarget() instanceof Player player) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                        }
                    }
                    // Check if light level weakens the wraith
                    int lightLevel = this.level().getMaxLocalRawBrightness(this.blockPosition());
                    if (lightLevel > 10) {
                        // Light weakens the wraith - reduced attack speed
                        this.getAttribute(Attributes.ATTACK_DAMAGE)
                            .setBaseValue(7.0 * LIGHT_DAMAGE_REDUCTION);
                    } else {
                        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0);
                    }
                }
                case PACIFIED -> {
                    pacifiedTimer--;
                    if (!hasRevealedHint) {
                        revealLootHint();
                        hasRevealedHint = true;
                    }
                    if (pacifiedTimer <= 0) {
                        // Fade away after revealing hint
                        this.discard();
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Food/bread pacifies the wraith (memory of last meal)
        if (stack.isEdible()) {
            if (!this.level().isClientSide()) {
                stack.shrink(1);
                setWraithState(WraithState.PACIFIED);
                pacifiedTimer = PACIFIED_DURATION;
                this.setTarget(null);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    /**
     * Reveals a loot hint to nearby players (signals hidden passage or chest location).
     * In a full implementation, this would place glowing particles at a nearby secret.
     */
    private void revealLootHint() {
        // Placeholder: the wraith points toward the nearest hidden chest/passage
        // This would integrate with the dungeon room system to find secrets
    }

    @Override
    protected void tickDeath() {
        // On death, also reveal loot hints
        if (!hasRevealedHint && !this.level().isClientSide()) {
            revealLootHint();
            hasRevealedHint = true;
        }
        super.tickDeath();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 10, state -> {
            WraithState wraithState = getWraithState();
            return switch (wraithState) {
                case LOOPING -> state.setAndContinue(LOOP_ANIM);
                case WARNING -> state.setAndContinue(WARNING_ANIM);
                case HOSTILE -> {
                    if (state.isMoving()) {
                        yield state.setAndContinue(MOVE);
                    }
                    yield state.setAndContinue(IDLE);
                }
                case PACIFIED -> state.setAndContinue(PACIFIED_ANIM);
            };
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (getWraithState() == WraithState.HOSTILE && this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
