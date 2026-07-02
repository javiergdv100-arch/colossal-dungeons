package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Deadweight Chest Trap - Fake chest that tilts floor toward precipice.
 * 
 * Mechanics:
 * - Looks like a normal chest but triggers floor tilt when opened
 * - Floor tilts toward a precipice/void/pit
 * - Honey prevents sliding on tilted floor
 * - Shield reduces slide speed significantly
 * - Ender pearl can teleport player back to safety
 * - Fishing rod can open the chest from a safe distance
 * 
 * Config values used:
 * - damage.amount: fall damage at precipice
 * - damage.radius: floor tilt zone radius
 * - timing.warningTicks: click sound before floor tilts
 * - timing.activeTicks: how long floor stays tilted
 */
public class DeadweightChestTrap extends AbstractTrap {

    private static final float SLIDE_FORCE = 0.3f;
    private static final float SHIELD_SLIDE_REDUCTION = 0.4f;
    private static final float HONEY_IMMUNITY_FORCE = 0.0f;

    private boolean chestOpened;
    private boolean openedFromDistance;
    private Vec3 precipiceDirection;

    public DeadweightChestTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.chestOpened = false;
        this.openedFromDistance = false;
        this.precipiceDirection = new Vec3(1, 0, 0); // Default slide direction
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        // Only activates when chest is opened
        return chestOpened;
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Mechanical click heard under the floor
        // Floor begins to creak
    }

    @Override
    protected void arm(ServerLevel level) {
        // Floor is about to tilt - momentary pause
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Floor tilts dramatically toward precipice
        // Loud grinding/crashing sound
    }

    @Override
    protected void damage(ServerLevel level) {
        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());

        for (LivingEntity target : targets) {
            float slideForce = SLIDE_FORCE;

            // Honey blocks provide complete immunity to sliding
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
                slideForce = HONEY_IMMUNITY_FORCE;
                continue;
            }

            // Shield reduces slide speed
            if (target.isBlocking()) {
                slideForce *= SHIELD_SLIDE_REDUCTION;
            }

            // Push entity toward precipice
            target.push(
                precipiceDirection.x * slideForce,
                precipiceDirection.y * slideForce,
                precipiceDirection.z * slideForce
            );

            // If entity reaches the edge, apply fall damage
            if (target.position().distanceTo(Vec3.atCenterOf(position)) > config.damage().radius()) {
                DamageSource fallSource = level.damageSources().fall();
                target.hurt(fallSource, config.damage().amount());
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Floor returns to level position
        chestOpened = false;
        openedFromDistance = false;
    }

    /**
     * Called when a player opens the fake chest.
     */
    public void onChestOpened(Vec3 direction) {
        this.chestOpened = true;
        this.precipiceDirection = direction.normalize();
    }

    /**
     * Called when fishing rod is used to open from distance.
     * Still triggers the trap but player is safely away.
     */
    public void openFromDistance(Vec3 direction) {
        this.chestOpened = true;
        this.openedFromDistance = true;
        this.precipiceDirection = direction.normalize();
    }

    public boolean isOpenedFromDistance() {
        return openedFromDistance;
    }
}
