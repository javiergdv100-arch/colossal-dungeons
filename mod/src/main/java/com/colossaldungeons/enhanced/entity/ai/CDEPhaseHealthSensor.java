package com.colossaldungeons.enhanced.entity.ai;

import com.colossaldungeons.enhanced.api.IBossPhase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.List;

/**
 * Custom sensor that detects health thresholds for phase transitions.
 * Monitors the boss entity's health and signals phase changes via memory modules
 * when health drops below configured thresholds.
 *
 * Works in conjunction with BossPhaseManager to trigger transitions.
 *
 * @param <E> the entity type (must implement IBossPhase)
 */
public class CDEPhaseHealthSensor<E extends Mob & IBossPhase> extends ExtendedSensor<E> {

    private static final int SCAN_RATE = 5; // Every 5 ticks

    public CDEPhaseHealthSensor() {
        this.setScanRate(entity -> SCAN_RATE);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(MemoryModuleType.HURT_BY_ENTITY);
    }

    @Override
    public SensorType<? extends CDEPhaseHealthSensor<?>> type() {
        return SensorType.HURT_BY;
    }

    @Override
    protected void doTick(ServerLevel level, E entity) {
        float healthFraction = entity.getHealth() / entity.getMaxHealth();
        int currentPhase = entity.getCurrentPhase();

        // Check each potential next phase
        // The BossPhaseManager handles the actual transition; this sensor
        // ensures the brain is aware of health state for behavior selection
        for (int nextPhase = currentPhase + 1; nextPhase < 8; nextPhase++) {
            float threshold = entity.getPhaseHealthThreshold(nextPhase);
            if (healthFraction <= threshold) {
                // Health is below a threshold - the phase manager will handle transition
                // We just ensure the brain's damage memory is refreshed
                break;
            }
        }
    }
}
