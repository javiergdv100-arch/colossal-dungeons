package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Strategy pattern interface for trap activation conditions.
 * Each implementation defines a different trigger mechanism.
 */
public interface TrapActivator {

    /**
     * Determines whether the trap at the given position should trigger this tick.
     *
     * @param level The server level
     * @param trapPos The position of the trap
     * @return true if the trap should trigger
     */
    boolean shouldTrigger(ServerLevel level, BlockPos trapPos);

    /**
     * Factory method to create the appropriate activator from config.
     */
    static TrapActivator fromConfig(TrapConfig.ActivatorConfig config) {
        return switch (config.type().toLowerCase()) {
            case "pressure" -> new PressureActivator();
            case "proximity" -> new ProximityActivator(config.range());
            case "timer" -> new TimerActivator(config.delay());
            case "manual" -> new ManualActivator();
            default -> new PressureActivator(); // Default fallback
        };
    }

    // ========== Implementations ==========

    /**
     * Triggers when a living entity steps on the trap block.
     */
    class PressureActivator implements TrapActivator {
        @Override
        public boolean shouldTrigger(ServerLevel level, BlockPos trapPos) {
            AABB checkArea = new AABB(trapPos).inflate(0.1, 0.5, 0.1);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, checkArea);
            return !entities.isEmpty();
        }
    }

    /**
     * Triggers when a living entity comes within range of the trap.
     */
    class ProximityActivator implements TrapActivator {
        private final double range;

        public ProximityActivator(double range) {
            this.range = range;
        }

        @Override
        public boolean shouldTrigger(ServerLevel level, BlockPos trapPos) {
            AABB checkArea = new AABB(trapPos).inflate(range);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, checkArea);
            return !entities.isEmpty();
        }
    }

    /**
     * Triggers periodically based on a timer interval.
     */
    class TimerActivator implements TrapActivator {
        private final int intervalTicks;
        private int tickCount;

        public TimerActivator(int intervalTicks) {
            this.intervalTicks = intervalTicks;
            this.tickCount = 0;
        }

        @Override
        public boolean shouldTrigger(ServerLevel level, BlockPos trapPos) {
            tickCount++;
            if (tickCount >= intervalTicks) {
                tickCount = 0;
                return true;
            }
            return false;
        }
    }

    /**
     * Triggers only when receiving a redstone signal.
     */
    class ManualActivator implements TrapActivator {
        @Override
        public boolean shouldTrigger(ServerLevel level, BlockPos trapPos) {
            return level.hasNeighborSignal(trapPos);
        }
    }
}
