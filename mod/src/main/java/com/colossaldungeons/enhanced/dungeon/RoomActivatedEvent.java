package com.colossaldungeons.enhanced.dungeon;

import com.colossaldungeons.enhanced.dungeon.room.RoomInstance;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired when a room transitions to the ACTIVE state.
 * Listeners can use this to spawn mobs, start timers, etc.
 */
public class RoomActivatedEvent extends Event {

    private final RoomInstance room;
    private final ServerPlayer trigger;

    public RoomActivatedEvent(RoomInstance room, ServerPlayer trigger) {
        this.room = room;
        this.trigger = trigger;
    }

    public RoomInstance getRoom() { return room; }
    public ServerPlayer getTrigger() { return trigger; }
}
