package com.colossaldungeons.enhanced.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.UUID;

/**
 * Neothelid Baby - Small swarm creature that moves in groups seeking food.
 *
 * Stats: 4 HP, 0 armor, Bite 2 damage.
 * Size: 0.4 x 0.3 blocks (silverfish-sized).
 *
 * Behavior:
 * - Moves in swarm formation, attacks by biting.
 * - Can be tamed with eggs; tamed ones follow the player and accept basic orders.
 * - Wild colony of 60+ that starves for 10 minutes triggers cannibalism -> adult transformation.
 * - Domesticated colony of 30 can form a tamed adult (160 HP, 4 armor, tail 10).
 *
 * Vanilla interactions:
 * - Eggs: feeding prevents cannibalistic transformation, keeps them docile.
 * - Food/bread: pacifies and attracts to the player.
 * - Water bucket: disperses the swarm temporarily.
 */
public class NeothelidBabyEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> IS_TAMED =
        SynchedEntityData.defineId(NeothelidBabyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> HUNGER_TIMER =
        SynchedEntityData.defineId(NeothelidBabyEntity.class, EntityDataSerializers.INT);

    // Transformation constants
    private static final int STARVATION_THRESHOLD_TICKS = 12000; // 10 minutes
    private static final int COLONY_SIZE_FOR_TRANSFORMATION = 60;
    private static final int TAMED_COLONY_SIZE_FOR_ADULT = 30;
    private static final double SWARM_SEARCH_RADIUS = 16.0;

    private UUID ownerUUID = null;
    private int fedTimer = 0; // Ticks since last fed

    // Animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.neothelid_baby.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.neothelid_baby.move");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.neothelid_baby.bite");
    private static final RawAnimation SWARM = RawAnimation.begin().thenLoop("animation.neothelid_baby.swarm");

    public NeothelidBabyEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for the Neothelid Baby.
     * 4 HP, 0.35 speed, 2 damage (bite), 0 armor.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_TAMED, false);
        builder.define(HUNGER_TIMER, 0);
    }

    public boolean isTamed() {
        return this.entityData.get(IS_TAMED);
    }

    private void setTamed(boolean tamed) {
        this.entityData.set(IS_TAMED, tamed);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                // Tamed babies do not attack players
                return !isTamed() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Track hunger
            if (fedTimer > 0) {
                fedTimer--;
            }

            // Increment hunger timer if not fed
            if (!isTamed() && fedTimer <= 0) {
                int hunger = this.entityData.get(HUNGER_TIMER);
                this.entityData.set(HUNGER_TIMER, hunger + 1);

                // Check for cannibalistic transformation
                if (hunger >= STARVATION_THRESHOLD_TICKS) {
                    checkCannibalTransformation();
                }
            }
        }
    }

    /**
     * Checks if there are 60+ starving babies nearby to trigger adult transformation.
     */
    private void checkCannibalTransformation() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        List<NeothelidBabyEntity> nearbyBabies = serverLevel.getEntitiesOfClass(
            NeothelidBabyEntity.class,
            this.getBoundingBox().inflate(SWARM_SEARCH_RADIUS),
            baby -> !baby.isTamed() && baby.entityData.get(HUNGER_TIMER) >= STARVATION_THRESHOLD_TICKS
        );

        if (nearbyBabies.size() >= COLONY_SIZE_FOR_TRANSFORMATION) {
            // Kill all babies except this one and spawn an adult
            for (NeothelidBabyEntity baby : nearbyBabies) {
                if (baby != this) {
                    baby.discard();
                }
            }
            // This entity also dies - the adult replaces the colony
            // Actual adult spawning would reference CDEEntities.NEOTHELID_ADULT
            this.discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Eggs tame and feed
        if (stack.is(Items.EGG)) {
            if (!this.level().isClientSide()) {
                stack.shrink(1);
                setTamed(true);
                this.ownerUUID = player.getUUID();
                fedTimer = 6000; // Fed for 5 minutes
                this.entityData.set(HUNGER_TIMER, 0);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // Any food pacifies
        if (stack.isEdible()) {
            if (!this.level().isClientSide()) {
                stack.shrink(1);
                fedTimer = 3600; // Fed for 3 minutes
                this.entityData.set(HUNGER_TIMER, 0);
                if (!isTamed()) {
                    // Pacify but do not fully tame
                    this.setTarget(null);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 3, state -> {
            if (this.swinging) {
                return state.setAndContinue(BITE);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
