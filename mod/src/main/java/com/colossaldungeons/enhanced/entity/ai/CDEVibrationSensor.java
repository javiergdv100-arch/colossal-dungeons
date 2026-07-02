package com.colossaldungeons.enhanced.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.List;

/**
 * Custom sensor that detects vibrations and sounds near the entity.
 * Used by the Dying Atlas to detect players sneaking around or making noise.
 *
 * Checks for nearby game events (footsteps, breaking blocks, etc.)
 * and sets memory modules accordingly.
 *
 * @param <E> the entity type
 */
public class CDEVibrationSensor<E extends LivingEntity> extends ExtendedSensor<E> {

    private static final int SCAN_RATE = 10; // Every 10 ticks
    private static final double DETECTION_RANGE = 16.0;

    public CDEVibrationSensor() {
        this.setScanRate(entity -> SCAN_RATE);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    @Override
    public SensorType<? extends CDEVibrationSensor<?>> type() {
        return SensorType.NEAREST_LIVING_ENTITIES;
    }

    @Override
    protected void doTick(ServerLevel level, E entity) {
        // Detect vibrations/game events within range
        // In a full implementation, this would hook into the vibration listener system
        // For now, it uses entity proximity as a vibration proxy
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(DETECTION_RANGE),
            e -> e != entity && !e.isSpectator()
        );

        // Entities that are moving (not sneaking) produce stronger vibrations
        for (LivingEntity nearby : nearbyEntities) {
            if (!nearby.isShiftKeyDown() && nearby.getDeltaMovement().lengthSqr() > 0.003) {
                // This entity is making vibrations - mark in memory
                entity.getBrain().setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, nearbyEntities);
                return;
            }
        }
    }
}
