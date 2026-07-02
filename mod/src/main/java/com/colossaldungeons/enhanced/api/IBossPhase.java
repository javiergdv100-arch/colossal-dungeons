package com.colossaldungeons.enhanced.api;

import java.util.List;

/**
 * Public API interface for boss entities with phased combat.
 * Implement on boss mobs to enable phase-based attack patterns,
 * health thresholds, and transition animations.
 *
 * Addon mods can implement this to create custom bosses compatible
 * with the CDE boss framework.
 */
public interface IBossPhase {

    /**
     * Gets the current phase ordinal of the boss.
     *
     * @return the current phase index (0-based)
     */
    int getCurrentPhase();

    /**
     * Transitions the boss to the specified phase.
     * Implementations should handle animation transitions, attack pattern changes,
     * and network synchronization.
     *
     * @param phase the target phase ordinal
     */
    void transitionToPhase(int phase);

    /**
     * Gets the health threshold (as a fraction 0.0-1.0) at which the boss
     * transitions to the specified phase.
     *
     * @param phase the phase ordinal
     * @return health fraction threshold (e.g., 0.75 means 75% HP triggers this phase)
     */
    float getPhaseHealthThreshold(int phase);

    /**
     * Gets the list of attack pattern identifiers available during the specified phase.
     *
     * @param phase the phase ordinal
     * @return list of attack pattern IDs for this phase
     */
    List<String> getPhaseAttackPatterns(int phase);
}
