package com.colossaldungeons.enhanced.dungeon.room;

import com.mojang.serialization.Codec;

/**
 * Represents the lifecycle state of a room within a dungeon.
 * This is the runtime state used by RoomInstance for the state machine.
 */
public enum RoomState {
    /** Room data is not loaded, no systems active. */
    UNLOADED,
    /** Room is loaded and prepared but not yet activated. */
    PREPARED,
    /** Room is fully active with all systems running. */
    ACTIVE,
    /** Room has been completed (all objectives met). */
    COMPLETED,
    /** Room is in a low-power dormant state (no players nearby). */
    DORMANT,
    /** Room is suspended (paused, e.g., during dungeon reset). */
    SUSPENDED;

    public static final Codec<RoomState> CODEC = Codec.STRING.xmap(
        RoomState::valueOf,
        RoomState::name
    );

    /**
     * Checks whether a transition from this state to the target state is valid.
     */
    public boolean canTransitionTo(RoomState target) {
        return switch (this) {
            case UNLOADED -> target == PREPARED;
            case PREPARED -> target == ACTIVE || target == DORMANT;
            case ACTIVE -> target == COMPLETED || target == DORMANT || target == SUSPENDED;
            case COMPLETED -> target == DORMANT || target == UNLOADED;
            case DORMANT -> target == ACTIVE || target == PREPARED || target == UNLOADED;
            case SUSPENDED -> target == ACTIVE || target == DORMANT || target == UNLOADED;
        };
    }
}
