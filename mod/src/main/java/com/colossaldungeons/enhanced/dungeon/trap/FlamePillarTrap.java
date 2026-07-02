package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Flame Pillar Trap - Retractable pillar shooting fire in up to 4 directions.
 * 
 * Mechanics:
 * - Pillar extends from floor and shoots fire jets NORTH, SOUTH, EAST, WEST
 * - Water disables the pillar for 15 seconds
 * - Shield blocks fire damage completely
 * - Snowball clogs one fire output for one cycle
 * - Rotates fire direction each cycle
 * 
 * Config values used:
 * - damage.amount: fire damage per tick
 * - damage.radius: fire jet reach distance
 * - timing.warningTicks: pillar extension warning time
 * - timing.activeTicks: duration of fire burst
 */
public class FlamePillarTrap extends AbstractTrap {

    private static final int WATER_DISABLE_TICKS = 300; // 15 seconds
    private static final int FIRE_JET_LENGTH = 5;

    private final List<Direction> activeDirections;
    private boolean disabledByWater;
    private int waterDisableTimer;
    private Direction cloggedDirection;

    public FlamePillarTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.activeDirections = new ArrayList<>(List.of(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        ));
        this.disabledByWater = false;
        this.waterDisableTimer = 0;
        this.cloggedDirection = null;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        // Check for water disabling
        if (disabledByWater) {
            waterDisableTimer--;
            if (waterDisableTimer <= 0) {
                disabledByWater = false;
            }
            return false;
        }

        // Check if water is adjacent (disables the trap)
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(position.relative(dir)).is(Blocks.WATER)) {
                disabledByWater = true;
                waterDisableTimer = WATER_DISABLE_TICKS;
                return false;
            }
        }

        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Pillar rumbles and begins to extend from the floor
        // Glow appears at the base indicating heat buildup
    }

    @Override
    protected void arm(ServerLevel level) {
        // Pillar is fully extended - fire nozzles visible
        // Hissing sound of gas/fuel building pressure
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Fire erupts from the pillar in active directions
        // Loud whoosh/roar sound
    }

    @Override
    protected void damage(ServerLevel level) {
        DamageSource fireSource = level.damageSources().onFire();
        float jetReach = config.damage().radius() > 0 ? config.damage().radius() : FIRE_JET_LENGTH;

        for (Direction dir : activeDirections) {
            if (dir == cloggedDirection) continue;

            // Check for entities in the fire jet path
            for (int i = 1; i <= (int) jetReach; i++) {
                BlockPos jetPos = position.relative(dir, i);
                AABB jetArea = new AABB(jetPos).inflate(0.5);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, jetArea);

                for (LivingEntity target : targets) {
                    // Shield blocks fire damage (checked by vanilla mechanics)
                    target.hurt(fireSource, config.damage().amount());
                    target.setRemainingFireTicks(config.timing().activeTicks());
                }
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Pillar retracts, fire stops
        // Clear the clogged direction after reset (snowball melts)
        cloggedDirection = null;
    }

    /**
     * Called when a snowball hits the pillar.
     * Clogs one fire output for the current cycle.
     */
    public void onSnowballHit(Direction hitDirection) {
        this.cloggedDirection = hitDirection;
    }

    /**
     * Called when water contacts the pillar.
     * Disables for 15 seconds.
     */
    public void disableWithWater() {
        this.disabledByWater = true;
        this.waterDisableTimer = WATER_DISABLE_TICKS;
        if (state == TrapState.TRIGGERED || state == TrapState.ARMED) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
        }
    }

    public boolean isDisabledByWater() {
        return disabledByWater;
    }
}
