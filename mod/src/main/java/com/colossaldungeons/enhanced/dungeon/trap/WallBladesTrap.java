package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Wall Blades Trap - Retractable blades/spikes from walls in cycles.
 * 
 * Mechanics:
 * - Blades extend and retract from walls in a timed pattern
 * - Spyglass reveals the pattern timing from a distance
 * - Blocks placed in blade holes prevent extension
 * - Powder snow freezes mechanism briefly (20 second pause)
 * - Pattern has safe windows that observant players can exploit
 * 
 * Config values used:
 * - damage.amount: blade slash/stab damage
 * - damage.radius: blade extension reach
 * - timing.warningTicks: subtle click before each extension
 * - timing.activeTicks: full cycle duration
 */
public class WallBladesTrap extends AbstractTrap {

    private static final int PATTERN_PHASE_TICKS = 20;
    private static final int POWDER_SNOW_FREEZE_TICKS = 400; // 20 seconds
    private static final float BLADE_REACH = 2.0f;

    private int patternPhase;
    private int totalPhases;
    private boolean frozenBySnow;
    private int freezeTimer;
    private boolean[] phaseActive;

    public WallBladesTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.totalPhases = 4;
        this.patternPhase = 0;
        this.frozenBySnow = false;
        this.freezeTimer = 0;
        // Define which phases have active blades (pattern)
        this.phaseActive = new boolean[]{true, false, true, false};
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (frozenBySnow) {
            freezeTimer--;
            if (freezeTimer <= 0) {
                frozenBySnow = false;
            }
            return false;
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
        // Mechanical clicking sound - blades are cycling up
        patternPhase = 0;
    }

    @Override
    protected void arm(ServerLevel level) {
        // Blades are ready - first extension imminent
        // Subtle scraping sound from wall slots
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Full blade cycle begins
    }

    @Override
    protected void damage(ServerLevel level) {
        if (frozenBySnow) {
            return;
        }

        // Calculate current phase in pattern
        patternPhase = (tickCounter / PATTERN_PHASE_TICKS) % totalPhases;

        if (!phaseActive[patternPhase]) {
            return; // Safe window - blades retracted
        }

        // Check if blade holes are blocked by player-placed blocks
        if (areBladesBlocked(level)) {
            return;
        }

        // Blades are extended - damage entities in reach
        List<LivingEntity> targets = getTargetsInRange(level, BLADE_REACH);
        DamageSource slashSource = level.damageSources().generic();

        for (LivingEntity target : targets) {
            target.hurt(slashSource, config.damage().amount());
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        patternPhase = 0;
        frozenBySnow = false;
        freezeTimer = 0;
    }

    /**
     * Checks if players have placed blocks in the blade extension holes.
     */
    private boolean areBladesBlocked(ServerLevel level) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos bladeSlot = position.relative(dir);
            BlockState blockState = level.getBlockState(bladeSlot);
            if (blockState.isSolid() && !blockState.is(Blocks.BEDROCK)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when powder snow contacts the mechanism.
     * Freezes blades for 20 seconds.
     */
    public void freezeWithPowderSnow() {
        this.frozenBySnow = true;
        this.freezeTimer = POWDER_SNOW_FREEZE_TICKS;
    }

    /**
     * Gets the current pattern phase (for spyglass observation).
     */
    public int getPatternPhase() {
        return patternPhase;
    }

    public boolean[] getPattern() {
        return phaseActive;
    }
}
