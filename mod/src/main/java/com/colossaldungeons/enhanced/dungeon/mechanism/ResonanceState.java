package com.colossaldungeons.enhanced.dungeon.mechanism;

/**
 * States for Resonance Crystals within the dungeon.
 */
public enum ResonanceState {
    /** Crystal is inactive, not responding to stimuli. */
    DORMANT,
    /** Crystal is building up charge from sound inputs. */
    CHARGING,
    /** Crystal is fully charged and active (opens doors, disables barriers). */
    CHARGED,
    /** Crystal has been overloaded (TNT or excessive input) - dangerous state. */
    OVERLOADED;

    /**
     * Checks if this state can transition to the target state.
     */
    public boolean canTransitionTo(ResonanceState target) {
        return switch (this) {
            case DORMANT -> target == CHARGING || target == OVERLOADED;
            case CHARGING -> target == CHARGED || target == OVERLOADED || target == DORMANT;
            case CHARGED -> target == DORMANT || target == OVERLOADED;
            case OVERLOADED -> target == DORMANT;
        };
    }
}
