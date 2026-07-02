package com.colossaldungeons.enhanced.dungeon;

import com.colossaldungeons.enhanced.dungeon.room.RoomInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Represents a live dungeon instance in the world.
 * Tracks rooms, players, and the overall dungeon state.
 */
public class DungeonInstance {

    private final ResourceLocation id;
    private final List<RoomInstance> rooms;
    private final Map<UUID, RoomInstance> playerRooms;
    private DungeonState state;
    private int ticksActive;

    public DungeonInstance(ResourceLocation id) {
        this.id = id;
        this.rooms = new ArrayList<>();
        this.playerRooms = new HashMap<>();
        this.state = DungeonState.UNDISCOVERED;
        this.ticksActive = 0;
    }

    /**
     * Ticks this dungeon instance, updating all active rooms.
     */
    public void tick(ServerLevel level) {
        if (state != DungeonState.ACTIVE) return;

        ticksActive++;

        for (RoomInstance room : rooms) {
            room.tick(level);
        }
    }

    /**
     * Finds the room that contains the given position.
     *
     * @param pos The block position to search for
     * @return The room containing the position, or null if none
     */
    public RoomInstance findRoomAt(BlockPos pos) {
        for (RoomInstance room : rooms) {
            if (room.containsPos(pos)) {
                return room;
            }
        }
        return null;
    }

    /**
     * Gets the room a player is currently in.
     *
     * @param player The player to look up
     * @return The room the player is in, or null if not tracked
     */
    public RoomInstance getRoomForPlayer(Player player) {
        return playerRooms.get(player.getUUID());
    }

    /**
     * Updates the player's current room assignment.
     */
    public void setPlayerRoom(Player player, RoomInstance room) {
        playerRooms.put(player.getUUID(), room);
    }

    /**
     * Removes a player from dungeon tracking.
     */
    public void removePlayer(Player player) {
        RoomInstance currentRoom = playerRooms.remove(player.getUUID());
        if (currentRoom != null) {
            currentRoom.onPlayerExit((net.minecraft.server.level.ServerPlayer) player);
        }
    }

    /**
     * Adds a room to this dungeon instance.
     */
    public void addRoom(RoomInstance room) {
        rooms.add(room);
    }

    /**
     * Gets all players currently in this dungeon.
     */
    public Set<UUID> getActivePlayers() {
        return Collections.unmodifiableSet(playerRooms.keySet());
    }

    /**
     * Gets the total number of players in this dungeon.
     */
    public int getPlayerCount() {
        return playerRooms.size();
    }

    /**
     * Checks if a player is currently in this dungeon.
     */
    public boolean containsPlayer(Player player) {
        return playerRooms.containsKey(player.getUUID());
    }

    /**
     * Transitions the dungeon to a new state.
     */
    public void setState(DungeonState newState) {
        this.state = newState;
    }

    // --- Getters ---

    public ResourceLocation getId() { return id; }
    public List<RoomInstance> getRooms() { return rooms; }
    public Map<UUID, RoomInstance> getPlayerRooms() { return playerRooms; }
    public DungeonState getState() { return state; }
    public int getTicksActive() { return ticksActive; }
}
