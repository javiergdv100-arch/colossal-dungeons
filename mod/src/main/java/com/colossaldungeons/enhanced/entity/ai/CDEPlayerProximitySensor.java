package com.colossaldungeons.enhanced.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.List;

/**
 * Custom sensor that detects the nearest player within a configurable range.
 * Unlike the vanilla NearbyPlayersSensor, this sensor is configurable per entity
 * and provides additional proximity data for AI decision-making.
 *
 * @param <E> the entity type
 */
public class CDEPlayerProximitySensor<E extends LivingEntity> extends ExtendedSensor<E> {

    private static final int SCAN_RATE = 10; // Every 10 ticks
    private final double detectionRange;
    private ServerPlayer nearestPlayer;
    private double nearestDistance;

    /**
     * Creates a proximity sensor with the default range of 16 blocks.
     */
    public CDEPlayerProximitySensor() {
        this(16.0);
    }

    /**
     * Creates a proximity sensor with a custom detection range.
     *
     * @param range the detection range in blocks
     */
    public CDEPlayerProximitySensor(double range) {
        this.detectionRange = range;
        this.nearestPlayer = null;
        this.nearestDistance = Double.MAX_VALUE;
        this.setScanRate(entity -> SCAN_RATE);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
    }

    @Override
    public SensorType<? extends CDEPlayerProximitySensor<?>> type() {
        return SensorType.NEAREST_PLAYERS;
    }

    @Override
    protected void doTick(ServerLevel level, E entity) {
        nearestPlayer = null;
        nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;

            double distance = entity.distanceTo(player);
            if (distance <= detectionRange && distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer != null) {
            entity.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, nearestPlayer);
        } else {
            entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        }
    }

    /**
     * Gets the nearest detected player, or null if no player is in range.
     *
     * @return the nearest player or null
     */
    public ServerPlayer getNearestPlayer() {
        return nearestPlayer;
    }

    /**
     * Gets the distance to the nearest detected player.
     *
     * @return the distance, or Double.MAX_VALUE if no player is in range
     */
    public double getNearestDistance() {
        return nearestDistance;
    }

    /**
     * Checks if any player is within the detection range.
     *
     * @return true if a player is detected
     */
    public boolean hasPlayerInRange() {
        return nearestPlayer != null;
    }
}
