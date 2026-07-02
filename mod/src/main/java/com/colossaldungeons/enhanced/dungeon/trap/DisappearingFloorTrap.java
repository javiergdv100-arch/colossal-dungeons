package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Disappearing Floor Trap - Floor sections become illusory without warning.
 * 
 * Mechanics:
 * - Floor looks solid but is actually non-existent in certain sections
 * - No visual warning - floor appears completely normal
 * - Snowballs bounce on real floor, PASS THROUGH fake (diagnostic tool)
 * - Powder snow settles on real floor only (visual indicator)
 * - Falling through deals void/fall damage
 * - Pattern of real/fake changes periodically
 * 
 * Config values used:
 * - damage.amount: fall damage when passing through
 * - damage.radius: area of disappearing floor sections
 * - timing.warningTicks: time before floor pattern changes
 * - timing.activeTicks: pattern cycle duration
 */
public class DisappearingFloorTrap extends AbstractTrap {

    private static final int PATTERN_CHANGE_INTERVAL = 200;
    private static final float FALL_DISTANCE_DAMAGE = 10.0f;

    private List<BlockPos> illusoryPositions;
    private List<BlockPos> realPositions;
    private int patternIndex;

    public DisappearingFloorTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.illusoryPositions = new ArrayList<>();
        this.realPositions = new ArrayList<>();
        this.patternIndex = 0;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        List<Player> players = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.activator().range()));
        return !players.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Generate floor pattern - some sections real, some illusory
        generateFloorPattern(level);
    }

    @Override
    protected void arm(ServerLevel level) {
        // Floor trap is active - illusory sections are set
        // No visible difference - that is the danger
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Player steps on illusory section - falls through
    }

    @Override
    protected void damage(ServerLevel level) {
        // Change pattern periodically
        if (tickCounter % PATTERN_CHANGE_INTERVAL == 0) {
            patternIndex++;
            generateFloorPattern(level);
        }

        // Check for entities on illusory positions
        for (BlockPos illusoryPos : illusoryPositions) {
            AABB checkArea = new AABB(illusoryPos).inflate(0.5, 1.0, 0.5);
            List<LivingEntity> onIllusion = level.getEntitiesOfClass(LivingEntity.class, checkArea);

            DamageSource fallSource = level.damageSources().fall();

            for (LivingEntity target : onIllusion) {
                // Entity falls through illusory floor
                target.hurt(fallSource, config.damage().amount());
                // Push entity downward (falling through)
                target.setDeltaMovement(target.getDeltaMovement().add(0, -0.5, 0));
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        illusoryPositions.clear();
        realPositions.clear();
        patternIndex = 0;
    }

    /**
     * Generates the pattern of real/illusory floor sections.
     * Pattern changes based on patternIndex.
     */
    private void generateFloorPattern(ServerLevel level) {
        illusoryPositions.clear();
        realPositions.clear();

        int radius = (int) config.damage().radius();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos floorPos = position.offset(x, 0, z);
                // Alternating pattern based on index
                boolean isIllusory = ((x + z + patternIndex) % 3 == 0);
                if (isIllusory) {
                    illusoryPositions.add(floorPos);
                } else {
                    realPositions.add(floorPos);
                }
            }
        }
    }

    /**
     * Tests if a position is illusory (for snowball diagnostic).
     * @return true if the position is fake floor
     */
    public boolean isIllusory(BlockPos testPos) {
        return illusoryPositions.contains(testPos);
    }

    /**
     * Gets all real floor positions (for powder snow indicator).
     */
    public List<BlockPos> getRealPositions() {
        return realPositions;
    }

    public List<BlockPos> getIllusoryPositions() {
        return illusoryPositions;
    }
}
