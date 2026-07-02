package com.colossaldungeons.enhanced.dungeon;

import net.neoforged.bus.api.Event;

/**
 * Fired when a dungeon begins the reset process.
 * Listeners can use this to clean up custom state, respawn decorations, etc.
 */
public class DungeonResetEvent extends Event {

    private final DungeonInstance dungeon;

    public DungeonResetEvent(DungeonInstance dungeon) {
        this.dungeon = dungeon;
    }

    public DungeonInstance getDungeon() { return dungeon; }
}
