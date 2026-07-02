package com.colossaldungeons.enhanced.dungeon;

import com.mojang.serialization.Codec;

/**
 * Represents the lifecycle state of a dungeon instance.
 */
public enum DungeonState {
    /** Dungeon has not yet been found by any player. */
    UNDISCOVERED,
    /** Dungeon has been found but no one has entered yet. */
    DISCOVERED,
    /** Dungeon is actively being played. */
    ACTIVE,
    /** Dungeon has been fully completed (boss killed, objectives met). */
    COMPLETED,
    /** Dungeon is in the process of resetting for another run. */
    RESETTING;

    public static final Codec<DungeonState> CODEC = Codec.STRING.xmap(
        DungeonState::valueOf,
        DungeonState::name
    );
}
