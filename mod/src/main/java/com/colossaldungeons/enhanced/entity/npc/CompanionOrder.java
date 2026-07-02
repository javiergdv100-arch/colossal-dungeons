package com.colossaldungeons.enhanced.entity.npc;

/**
 * Orders that can be given to a companion entity by its owner.
 * Each order transitions the companion to a corresponding behavior state.
 */
public enum CompanionOrder {
    /** Follow the owner at a moderate distance */
    FOLLOW,
    /** Stay at the current position until given another order */
    WAIT,
    /** Attack the nearest hostile entity */
    ATTACK,
    /** Explore the current room autonomously */
    EXPLORE,
    /** Interact with the nearest interactable block/entity */
    INTERACT,
    /** Return to the owner immediately (overrides current task) */
    RETURN;

    /**
     * Converts this order to the corresponding companion state.
     * INTERACT and RETURN resolve to transitional behaviors rather than persistent states.
     *
     * @return the companion state for this order
     */
    public CompanionState toState() {
        return switch (this) {
            case FOLLOW, RETURN -> CompanionState.FOLLOW;
            case WAIT -> CompanionState.WAIT;
            case ATTACK -> CompanionState.ATTACK;
            case EXPLORE, INTERACT -> CompanionState.EXPLORE;
        };
    }

    /**
     * Parses an order from a string name (case-insensitive).
     *
     * @param name the order name
     * @return the matching order, or FOLLOW as default
     */
    public static CompanionOrder fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FOLLOW;
        }
    }
}
