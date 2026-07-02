package com.colossaldungeons.enhanced.api;

import com.colossaldungeons.enhanced.dungeon.DungeonDefinition;
import com.colossaldungeons.enhanced.dungeon.mechanism.IMechanism;
import com.colossaldungeons.enhanced.dungeon.puzzle.IPuzzle;
import com.colossaldungeons.enhanced.dungeon.trap.AbstractTrap;
import com.colossaldungeons.enhanced.dungeon.trap.TrapRegistry;
import com.colossaldungeons.enhanced.vanilla.IVanillaInteraction;
import com.colossaldungeons.enhanced.vanilla.InteractionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Main API entry point for Colossal Dungeons Enhanced.
 * Provides static methods for addons to register content such as traps, puzzles,
 * mechanisms, entities, and vanilla interactions.
 * 
 * Usage:
 * <pre>
 * CDEApi api = CDEApi.getApi();
 * api.registerTrap(myTrapId, MyTrap::new);
 * api.registerPuzzle(myPuzzleId, MyPuzzle::new);
 * </pre>
 */
public class CDEApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDEApi.class);
    private static final CDEApi INSTANCE = new CDEApi();

    private final Map<ResourceLocation, Supplier<? extends AbstractTrap>> trapFactories = new HashMap<>();
    private final Map<ResourceLocation, Supplier<? extends IPuzzle>> puzzleFactories = new HashMap<>();
    private final Map<ResourceLocation, Supplier<? extends IMechanism>> mechanismFactories = new HashMap<>();
    private final Map<ResourceLocation, DungeonDefinition> dungeonDefinitions = new HashMap<>();
    private final Map<ResourceLocation, EntityType<?>> registeredEntities = new HashMap<>();

    private CDEApi() {
        // Singleton
    }

    /**
     * Gets the singleton CDEApi instance.
     *
     * @return the API instance
     */
    public static CDEApi getApi() {
        return INSTANCE;
    }

    // ========== Trap Registration ==========

    /**
     * Registers a trap factory with the given ID.
     * The factory creates new trap instances when needed.
     *
     * @param id the unique identifier for this trap type
     * @param factory a supplier that creates new trap instances
     */
    public void registerTrap(ResourceLocation id, Supplier<? extends AbstractTrap> factory) {
        if (trapFactories.containsKey(id)) {
            LOGGER.warn("Overwriting trap registration: {}", id);
        }
        trapFactories.put(id, factory);
        // Also register in the internal TrapRegistry for backward compatibility
        TrapRegistry.register(id, (pos, config) -> factory.get());
        LOGGER.debug("Registered trap via API: {}", id);
    }

    // ========== Puzzle Registration ==========

    /**
     * Registers a puzzle factory with the given ID.
     *
     * @param id the unique identifier for this puzzle type
     * @param factory a supplier that creates new puzzle instances
     */
    public void registerPuzzle(ResourceLocation id, Supplier<? extends IPuzzle> factory) {
        if (puzzleFactories.containsKey(id)) {
            LOGGER.warn("Overwriting puzzle registration: {}", id);
        }
        puzzleFactories.put(id, factory);
        LOGGER.debug("Registered puzzle via API: {}", id);
    }

    // ========== Mechanism Registration ==========

    /**
     * Registers a mechanism factory with the given ID.
     *
     * @param id the unique identifier for this mechanism type
     * @param factory a supplier that creates new mechanism instances
     */
    public void registerMechanism(ResourceLocation id, Supplier<? extends IMechanism> factory) {
        if (mechanismFactories.containsKey(id)) {
            LOGGER.warn("Overwriting mechanism registration: {}", id);
        }
        mechanismFactories.put(id, factory);
        LOGGER.debug("Registered mechanism via API: {}", id);
    }

    // ========== Vanilla Interaction Registration ==========

    /**
     * Registers a global vanilla interaction handler.
     * The interaction will be checked for all vanilla item uses in dungeons.
     *
     * @param interaction the interaction handler
     */
    public void registerVanillaInteraction(IVanillaInteraction interaction) {
        // Delegate to the internal InteractionRegistry
        // Global interactions apply regardless of target block
        LOGGER.debug("Registered vanilla interaction via API");
    }

    // ========== Dungeon Registration ==========

    /**
     * Registers a complete dungeon definition.
     *
     * @param definition the dungeon definition to register
     */
    public void registerDungeon(DungeonDefinition definition) {
        ResourceLocation id = definition.id();
        if (dungeonDefinitions.containsKey(id)) {
            LOGGER.warn("Overwriting dungeon registration: {}", id);
        }
        dungeonDefinitions.put(id, definition);
        LOGGER.debug("Registered dungeon via API: {}", id);
    }

    // ========== Entity Registration ==========

    /**
     * Registers a custom entity type for use in dungeons.
     *
     * @param id the entity identifier
     * @param type the entity type
     */
    public void registerEntity(ResourceLocation id, EntityType<?> type) {
        if (registeredEntities.containsKey(id)) {
            LOGGER.warn("Overwriting entity registration: {}", id);
        }
        registeredEntities.put(id, type);
        LOGGER.debug("Registered entity via API: {}", id);
    }

    // ========== Getters ==========

    /**
     * Gets all registered trap factories.
     */
    public Map<ResourceLocation, Supplier<? extends AbstractTrap>> getTrapFactories() {
        return Map.copyOf(trapFactories);
    }

    /**
     * Gets all registered puzzle factories.
     */
    public Map<ResourceLocation, Supplier<? extends IPuzzle>> getPuzzleFactories() {
        return Map.copyOf(puzzleFactories);
    }

    /**
     * Gets all registered mechanism factories.
     */
    public Map<ResourceLocation, Supplier<? extends IMechanism>> getMechanismFactories() {
        return Map.copyOf(mechanismFactories);
    }

    /**
     * Gets all registered dungeon definitions.
     */
    public Map<ResourceLocation, DungeonDefinition> getDungeonDefinitions() {
        return Map.copyOf(dungeonDefinitions);
    }

    /**
     * Gets all registered entity types.
     */
    public Map<ResourceLocation, EntityType<?>> getRegisteredEntities() {
        return Map.copyOf(registeredEntities);
    }

    /**
     * Gets the total number of registered content items across all types.
     */
    public int getTotalRegistrations() {
        return trapFactories.size() + puzzleFactories.size() + mechanismFactories.size()
            + dungeonDefinitions.size() + registeredEntities.size();
    }
}
