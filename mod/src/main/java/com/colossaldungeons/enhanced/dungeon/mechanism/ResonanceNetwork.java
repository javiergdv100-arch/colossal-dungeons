package com.colossaldungeons.enhanced.dungeon.mechanism;

import com.colossaldungeons.enhanced.api.IResonanceReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages connected resonance crystals within a room.
 * When one crystal charges, the signal can propagate to connected crystals.
 * Handles overload cascades where one crystal's overload can trigger neighbors.
 */
public class ResonanceNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResonanceNetwork.class);

    /** Maximum propagation distance between connected crystals. */
    private static final double MAX_CONNECTION_DISTANCE = 16.0;
    /** Propagation power loss per hop. */
    private static final int PROPAGATION_LOSS = 5;

    private final ResourceLocation roomId;
    private final Map<BlockPos, ResonanceCrystalEntity> crystals;
    private final Map<BlockPos, Set<BlockPos>> connections;

    public ResonanceNetwork(ResourceLocation roomId) {
        this.roomId = roomId;
        this.crystals = new HashMap<>();
        this.connections = new HashMap<>();
    }

    /**
     * Registers a crystal in this network.
     */
    public void addCrystal(BlockPos pos, ResonanceCrystalEntity crystal) {
        crystals.put(pos, crystal);
        rebuildConnections();
        LOGGER.debug("Added crystal at {} to network in room {}", pos, roomId);
    }

    /**
     * Removes a crystal from this network.
     */
    public void removeCrystal(BlockPos pos) {
        crystals.remove(pos);
        connections.remove(pos);
        connections.values().forEach(set -> set.remove(pos));
        LOGGER.debug("Removed crystal at {} from network in room {}", pos, roomId);
    }

    /**
     * Rebuilds the connection graph based on crystal positions and distance.
     */
    private void rebuildConnections() {
        connections.clear();
        List<BlockPos> positions = new ArrayList<>(crystals.keySet());

        for (int i = 0; i < positions.size(); i++) {
            for (int j = i + 1; j < positions.size(); j++) {
                BlockPos a = positions.get(i);
                BlockPos b = positions.get(j);

                double distance = Math.sqrt(a.distSqr(b));
                if (distance <= MAX_CONNECTION_DISTANCE) {
                    connections.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    connections.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                }
            }
        }
    }

    /**
     * Propagates a resonance pulse from a source crystal to connected crystals.
     *
     * @param source the source position
     * @param power the pulse power
     */
    public void propagateCharge(BlockPos source, int power) {
        Set<BlockPos> visited = new HashSet<>();
        propagateRecursive(source, power, visited);
    }

    private void propagateRecursive(BlockPos current, int power, Set<BlockPos> visited) {
        if (power <= 0) return;
        if (visited.contains(current)) return;
        visited.add(current);

        Set<BlockPos> neighbors = connections.get(current);
        if (neighbors == null) return;

        for (BlockPos neighbor : neighbors) {
            if (visited.contains(neighbor)) continue;

            ResonanceCrystalEntity crystal = crystals.get(neighbor);
            if (crystal != null) {
                int propagatedPower = Math.max(0, power - PROPAGATION_LOSS);
                if (propagatedPower > 0) {
                    crystal.onResonancePulse(propagatedPower);
                    propagateRecursive(neighbor, propagatedPower, visited);
                }
            }
        }
    }

    /**
     * Handles an overload cascade. When one crystal overloads, nearby crystals
     * receive a strong pulse that may push them to overload as well.
     *
     * @param source the overloading crystal position
     * @param level the server level
     */
    public void handleOverloadCascade(BlockPos source, ServerLevel level) {
        LOGGER.info("Overload cascade initiated from {} in room {}", source, roomId);

        Set<BlockPos> neighbors = connections.get(source);
        if (neighbors == null) return;

        for (BlockPos neighbor : neighbors) {
            ResonanceCrystalEntity crystal = crystals.get(neighbor);
            if (crystal != null && crystal.getResonanceState() != ResonanceState.OVERLOADED) {
                // Strong pulse that may cause chain overload
                crystal.onResonancePulse(50);
            }
        }
    }

    /**
     * Ticks all crystals in the network.
     */
    public void tick(ServerLevel level) {
        for (ResonanceCrystalEntity crystal : crystals.values()) {
            crystal.tick(level);
        }
    }

    /**
     * Checks if all crystals in the network are charged.
     */
    public boolean areAllCharged() {
        if (crystals.isEmpty()) return false;
        return crystals.values().stream()
            .allMatch(c -> c.getResonanceState() == ResonanceState.CHARGED);
    }

    /**
     * Checks if any crystal in the network is overloaded.
     */
    public boolean isAnyOverloaded() {
        return crystals.values().stream()
            .anyMatch(c -> c.getResonanceState() == ResonanceState.OVERLOADED);
    }

    /**
     * Gets all crystals in this network.
     */
    public Collection<ResonanceCrystalEntity> getCrystals() {
        return Collections.unmodifiableCollection(crystals.values());
    }

    /**
     * Gets the number of crystals in the network.
     */
    public int getSize() {
        return crystals.size();
    }

    /**
     * Gets the room ID this network belongs to.
     */
    public ResourceLocation getRoomId() {
        return roomId;
    }
}
