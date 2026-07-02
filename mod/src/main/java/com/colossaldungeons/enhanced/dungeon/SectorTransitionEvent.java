package com.colossaldungeons.enhanced.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player transitions between dungeon sectors (thematic areas).
 * Sectors represent different regions within a dungeon (e.g., "crypt", "library", "throne_room").
 */
public class SectorTransitionEvent extends Event {

    private final ServerPlayer player;
    private final String fromSector;
    private final String toSector;

    public SectorTransitionEvent(ServerPlayer player, String fromSector, String toSector) {
        this.player = player;
        this.fromSector = fromSector;
        this.toSector = toSector;
    }

    public ServerPlayer getPlayer() { return player; }
    public String getFromSector() { return fromSector; }
    public String getToSector() { return toSector; }
}
