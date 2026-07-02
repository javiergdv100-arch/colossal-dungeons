package com.colossaldungeons.enhanced.entity.npc;

/**
 * Represents the current behavioral state of a companion entity.
 * Each state maps to a different set of AI behaviors in CompanionAI.
 */
public enum CompanionState {
    /** Following the owner at a moderate pace */
    FOLLOW,
    /** Staying at the current position, idle */
    WAIT,
    /** Actively engaging hostile entities */
    ATTACK,
    /** Wandering and exploring the current room */
    EXPLORE;

    /**
     * Whether this state allows the companion to move freely.
     *
     * @return true if the companion can pathfind in this state
     */
    public boolean allowsMovement() {
        return this != WAIT;
    }

    /**
     * Whether this state allows the companion to attack hostiles.
     *
     * @return true if the companion will fight in this state
     */
    public boolean allowsCombat() {
        return this == ATTACK;
    }

    /**
     * Whether this state keeps the companion near the owner.
     *
     * @return true if the companion tries to stay near the owner
     */
    public boolean staysNearOwner() {
        return this == FOLLOW;
    }
}
