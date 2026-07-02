package com.colossaldungeons.enhanced.dungeon.mechanism;

/**
 * Lifecycle states for dungeon mechanisms.
 */
public enum MechanismState {
    /** Mechanism is not operational. */
    INACTIVE,
    /** Mechanism is ready to be activated. */
    READY,
    /** Mechanism is in the process of activating (animation/delay). */
    ACTIVATING,
    /** Mechanism is fully active and producing its effect. */
    ACTIVE,
    /** Mechanism is in the process of deactivating. */
    DEACTIVATING,
    /** Mechanism is broken and cannot be used until repaired. */
    BROKEN;

    /**
     * Checks if this state can transition to the target state.
     */
    public boolean canTransitionTo(MechanismState target) {
        return switch (this) {
            case INACTIVE -> target == READY;
            case READY -> target == ACTIVATING || target == INACTIVE || target == BROKEN;
            case ACTIVATING -> target == ACTIVE || target == BROKEN || target == INACTIVE;
            case ACTIVE -> target == DEACTIVATING || target == BROKEN;
            case DEACTIVATING -> target == READY || target == INACTIVE || target == BROKEN;
            case BROKEN -> target == INACTIVE; // Can only be repaired back to INACTIVE
        };
    }
}
