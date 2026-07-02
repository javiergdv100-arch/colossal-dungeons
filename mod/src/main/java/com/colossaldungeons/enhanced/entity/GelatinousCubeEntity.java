package com.colossaldungeons.enhanced.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Gelatinous Cube entity following section 7.4 of the technical plan.
 * A large, slow-moving ooze monster that engulfs and damages players.
 *
 * Stats: 80 HP, 0.18 speed, 8 attack damage, 0.8 knockback resistance.
 * Animations: idle (loop), move (loop), attack (play once).
 */
public class GelatinousCubeEntity extends CDEGeoEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gelatinous_cube.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.gelatinous_cube.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.gelatinous_cube.attack");

    public GelatinousCubeEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates the attribute supplier for Gelatinous Cube.
     * 80 HP, 0.18 speed, 8 damage, 0.8 knockback resistance.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 80.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main controller: handles idle and movement
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(MOVE);
            }
            return state.setAndContinue(IDLE);
        }));

        // Attack controller: handles attack animation
        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
