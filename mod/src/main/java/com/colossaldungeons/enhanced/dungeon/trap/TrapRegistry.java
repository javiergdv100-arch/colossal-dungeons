package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Registry for trap types. Maps ResourceLocation identifiers to trap factory functions.
 * Addons can register their own trap types here.
 */
public class TrapRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrapRegistry.class);

    private static final Map<ResourceLocation, BiFunction<BlockPos, TrapConfig, AbstractTrap>> REGISTRY = new HashMap<>();

    /**
     * Initializes the registry with built-in trap types.
     */
    public static void initialize() {
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "spike_plate"),
            SpikePlateTrap::new);

        LOGGER.info("TrapRegistry initialized with {} trap types", REGISTRY.size());
    }

    /**
     * Registers a trap factory under the given ID.
     *
     * @param id The unique identifier for this trap type
     * @param factory A function that creates a trap given position and config
     */
    public static void register(ResourceLocation id, BiFunction<BlockPos, TrapConfig, AbstractTrap> factory) {
        if (REGISTRY.containsKey(id)) {
            LOGGER.warn("Overwriting existing trap registration: {}", id);
        }
        REGISTRY.put(id, factory);
    }

    /**
     * Creates a trap instance for the given type at the specified position.
     *
     * @param id The trap type identifier
     * @param pos The world position for the trap
     * @param config The trap configuration
     * @return A new trap instance, or null if the type is not registered
     */
    public static AbstractTrap create(ResourceLocation id, BlockPos pos, TrapConfig config) {
        BiFunction<BlockPos, TrapConfig, AbstractTrap> factory = REGISTRY.get(id);
        if (factory == null) {
            LOGGER.error("Unknown trap type: {}", id);
            return null;
        }
        return factory.apply(pos, config);
    }

    /**
     * Checks if a trap type is registered.
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * Returns the number of registered trap types.
     */
    public static int getRegisteredCount() {
        return REGISTRY.size();
    }
}
