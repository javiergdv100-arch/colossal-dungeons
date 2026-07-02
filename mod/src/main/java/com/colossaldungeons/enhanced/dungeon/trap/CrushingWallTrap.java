package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Crushing Wall Trap - Two walls advancing toward center to crush entities.
 * 
 * Mechanics:
 * - Blocks placed between the walls stop their advance
 * - Water on the floor lubricates for faster player escape
 * - Deals 16 damage on crush (lethal for unarmored players)
 * - Walls advance one block per second (20 ticks)
 * 
 * Config values used:
 * - damage.amount: crush damage (default 16.0)
 * - damage.radius: kill zone width
 * - timing.warningTicks: rumbling before walls start moving
 * - timing.activeTicks: total time walls take to close
 */
public class CrushingWallTrap extends AbstractTrap {

    private static final float CRUSH_DAMAGE = 16.0f;
    private static final int ADVANCE_INTERVAL_TICKS = 20;
    private static final float WATER_SPEED_BONUS = 1.3f;

    private int wallAdvanceSteps;
    private int maxAdvanceSteps;
    private boolean wallsBlocked;

    public CrushingWallTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.wallAdvanceSteps = 0;
        this.maxAdvanceSteps = (int) (config.damage().radius() * 2);
        this.wallsBlocked = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Rumbling sound, dust falls from ceiling
        // Floor vibrates as warning
    }

    @Override
    protected void arm(ServerLevel level) {
        // Walls begin to glow/crack at edges - visible seam appears
        // Mechanical clicking sounds begin
        wallAdvanceSteps = 0;
        wallsBlocked = false;
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Walls start moving inward - loud grinding sound
        // Check if any blocks have been placed between as obstruction
        checkForObstructions(level);
    }

    @Override
    protected void damage(ServerLevel level) {
        if (wallsBlocked) return;

        // Advance walls periodically
        if (tickCounter % ADVANCE_INTERVAL_TICKS == 0) {
            wallAdvanceSteps++;
            checkForObstructions(level);
        }

        // Check if walls have fully closed
        if (wallAdvanceSteps >= maxAdvanceSteps) {
            // Crush all entities in the zone
            List<LivingEntity> targets = getTargetsInRange(level);
            DamageSource crushSource = level.damageSources().cramming();

            for (LivingEntity target : targets) {
                target.hurt(crushSource, CRUSH_DAMAGE);
                // Apply additional suffocation effect
                target.hurt(level.damageSources().inWall(), config.damage().amount());
            }
        } else {
            // Partial crush - entities near walls take reduced damage
            float progressRatio = (float) wallAdvanceSteps / maxAdvanceSteps;
            if (progressRatio > 0.7f) {
                List<LivingEntity> targets = getTargetsInRange(level);
                DamageSource crushSource = level.damageSources().cramming();
                for (LivingEntity target : targets) {
                    target.hurt(crushSource, config.damage().amount() * progressRatio);
                }
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Walls retract back to starting positions
        wallAdvanceSteps = 0;
        wallsBlocked = false;
    }

    /**
     * Checks if any solid blocks have been placed between the advancing walls.
     * Blocks stop the walls from advancing further.
     */
    private void checkForObstructions(ServerLevel level) {
        // Check the center area for player-placed blocks
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = position.offset(x, y, 0);
                BlockState blockState = level.getBlockState(checkPos);
                if (blockState.isSolid() && !blockState.is(Blocks.BEDROCK)) {
                    wallsBlocked = true;
                    return;
                }
            }
        }
    }

    /**
     * Checks if water is on the floor (provides speed bonus to escape).
     */
    public boolean hasWaterLubrication(ServerLevel level) {
        return level.getBlockState(position.below()).is(Blocks.WATER) ||
               level.getBlockState(position).is(Blocks.WATER);
    }

    public int getWallAdvanceSteps() {
        return wallAdvanceSteps;
    }

    public boolean areWallsBlocked() {
        return wallsBlocked;
    }
}
