package com.colossaldungeons.enhanced.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.level.LightLayer;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.List;

/**
 * Custom sensor that detects the light level at the entity's current position.
 * Useful for entities that behave differently in darkness vs. light
 * (e.g., the Glass Harlequin that is more dangerous in darkness).
 *
 * Stores the current light level in the brain's memory for use by behaviors.
 *
 * @param <E> the entity type
 */
public class CDELightLevelSensor<E extends LivingEntity> extends ExtendedSensor<E> {

    private static final int SCAN_RATE = 20; // Every second

    /** The threshold below which an entity is considered "in darkness" */
    public static final int DARKNESS_THRESHOLD = 4;

    /** The threshold above which an entity is considered "in light" */
    public static final int LIGHT_THRESHOLD = 8;

    private int lastLightLevel = 15;

    public CDELightLevelSensor() {
        this.setScanRate(entity -> SCAN_RATE);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    public SensorType<? extends CDELightLevelSensor<?>> type() {
        return SensorType.NEAREST_LIVING_ENTITIES;
    }

    @Override
    protected void doTick(ServerLevel level, E entity) {
        BlockPos pos = entity.blockPosition();
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        lastLightLevel = Math.max(blockLight, skyLight);
    }

    /**
     * Gets the last measured light level.
     *
     * @return light level (0-15)
     */
    public int getLastLightLevel() {
        return lastLightLevel;
    }

    /**
     * Whether the entity is currently in darkness.
     *
     * @return true if light level is below the darkness threshold
     */
    public boolean isInDarkness() {
        return lastLightLevel < DARKNESS_THRESHOLD;
    }

    /**
     * Whether the entity is currently in bright light.
     *
     * @return true if light level is at or above the light threshold
     */
    public boolean isInLight() {
        return lastLightLevel >= LIGHT_THRESHOLD;
    }
}
