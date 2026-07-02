package com.colossaldungeons.enhanced.api;

import java.util.List;

/**
 * Public API interface for entities with modular AI systems.
 * Provides a contract for registering behaviors, sensors, and memory modules.
 *
 * Addon mods can implement this to create custom creatures that integrate
 * with the CDE AI framework.
 */
public interface ICreatureAI {

    /**
     * Registers all behavior trees and tasks for this creature.
     * Called during entity initialization to set up the AI graph.
     */
    void registerBehaviors();

    /**
     * Gets the list of sensor class names used by this creature's AI.
     *
     * @return list of sensor identifiers
     */
    List<String> getSensors();

    /**
     * Gets the list of memory module identifiers used by this creature's brain.
     *
     * @return list of memory module identifiers
     */
    List<String> getMemoryModules();
}
