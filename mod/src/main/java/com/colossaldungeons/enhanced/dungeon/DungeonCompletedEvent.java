package com.colossaldungeons.enhanced.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired when a dungeon is marked as completed.
 * Listeners can use this to grant rewards, update progress, etc.
 */
public class DungeonCompletedEvent extends Event {

    private final ServerPlayer player;
    private final DungeonInstance dungeon;

    public DungeonCompletedEvent(ServerPlayer player, DungeonInstance dungeon) {
        this.player = player;
        this.dungeon = dungeon;
    }

    public ServerPlayer getPlayer() { return player; }
    public DungeonInstance getDungeon() { return dungeon; }
}
