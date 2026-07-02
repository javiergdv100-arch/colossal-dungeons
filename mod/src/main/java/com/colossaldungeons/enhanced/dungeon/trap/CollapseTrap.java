package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Collapse Trap - Floor or ceiling collapses with 3-6 second warning.
 * 
 * Mechanics:
 * - Warning period of 3-6 seconds (60-120 ticks) with dust/rumbling
 * - Shield protects 80% of debris damage
 * - Blocks placed as pillars prevent total collapse (reduce damage)
 * - Collapse creates rubble terrain that must be navigated
 * 
 * Config values used:
 * - damage.amount: debris impact damage
 * - damage.radius: collapse area radius
 * - timing.warningTicks: warning duration (randomized 60-120)
 * - timing.activeTicks: duration of falling debris
 */
public class CollapseTrap extends AbstractTrap {

    private static final float SHIELD_PROTECTION = 0.80f;
    private static final int MIN_WARNING_TICKS = 60;
    private static final int MAX_WARNING_TICKS = 120;

    private int actualWarningTicks;
    private boolean pillarSupported;
    private boolean isCeilingCollapse;

    public CollapseTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.actualWarningTicks = config.timing().warningTicks();
        this.pillarSupported = false;
        this.isCeilingCollapse = true;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        // Randomize warning time between 3-6 seconds
        if (actualWarningTicks == 0) {
            actualWarningTicks = MIN_WARNING_TICKS +
                level.random.nextInt(MAX_WARNING_TICKS - MIN_WARNING_TICKS);
        }
        return tickCounter >= actualWarningTicks;
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Dust begins falling from ceiling / floor cracks appear
        // Rumbling sound intensifies over the warning period
        // Determine if this is ceiling or floor collapse
        isCeilingCollapse = level.random.nextBoolean();

        // Check for player-placed support pillars
        checkForPillarSupport(level);
    }

    @Override
    protected void arm(ServerLevel level) {
        // Final moments before collapse
        // Loud cracking sounds, large dust clouds
        checkForPillarSupport(level);
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Structure begins to collapse
        // Generate falling block entities or particles
    }

    @Override
    protected void damage(ServerLevel level) {
        List<LivingEntity> targets = getTargetsInRange(level);
        DamageSource debrisSource = level.damageSources().fallingBlock(null);

        float damageAmount = config.damage().amount();

        // Pillar support reduces damage significantly
        if (pillarSupported) {
            damageAmount *= 0.3f;
        }

        for (LivingEntity target : targets) {
            float finalDamage = damageAmount;

            // Shield provides 80% protection
            if (target.isBlocking()) {
                finalDamage *= (1.0f - SHIELD_PROTECTION);
            }

            target.hurt(debrisSource, finalDamage);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Debris settles, dust clears
        // Area becomes navigable rubble terrain
        actualWarningTicks = 0;
        pillarSupported = false;
    }

    /**
     * Checks if players have placed solid blocks as support pillars
     * between floor and ceiling in the collapse area.
     */
    private void checkForPillarSupport(ServerLevel level) {
        int radius = (int) config.damage().radius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Check for vertical column of blocks (pillar)
                boolean isPillar = true;
                for (int y = 1; y <= 3; y++) {
                    BlockPos checkPos = position.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    if (!blockState.isSolid()) {
                        isPillar = false;
                        break;
                    }
                }
                if (isPillar) {
                    pillarSupported = true;
                    return;
                }
            }
        }
        pillarSupported = false;
    }

    public boolean isCeilingCollapse() {
        return isCeilingCollapse;
    }

    public boolean isPillarSupported() {
        return pillarSupported;
    }
}
