package com.colossaldungeons.enhanced.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player enters a dungeon instance.
 * Listeners can use this to apply effects, display messages, etc.
 */
public class DungeonEnteredEvent extends Event {

    private final ServerPlayer player;
    private final DungeonInstance dungeon;

    public DungeonEnteredEvent(ServerPlayer player, DungeonInstance dungeon) {
        this.player = player;
        this.dungeon = dungeon;
    }

    public ServerPlayer getPlayer() { return player; }
    public DungeonInstance getDungeon() { return dungeon; }
}
