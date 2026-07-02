package com.colossaldungeons.enhanced.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Singleton-per-ServerLevel manager for all dungeon instances.
 * Uses a WeakHashMap to allow garbage collection when levels are unloaded.
 * 
 * Responsibilities:
 * - Tracks active dungeon instances per level
 * - Maps players to their current dungeon
 * - Handles room transitions when players move between rooms
 * - Ticked each server tick via DungeonEventHandler
 */
public class DungeonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonManager.class);

    /** One DungeonManager per ServerLevel, weak-referenced for GC safety. */
    private static final WeakHashMap<ServerLevel, DungeonManager> INSTANCES = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<ResourceLocation, DungeonInstance> activeDungeons;
    private final Map<UUID, DungeonInstance> playerDungeonMap;

    private DungeonManager(ServerLevel level) {
        this.level = level;
        this.activeDungeons = new HashMap<>();
        this.playerDungeonMap = new HashMap<>();
    }

    /**
     * Gets (or creates) the DungeonManager for the given level.
     * Thread-safe singleton-per-level pattern.
     */
    public static DungeonManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, DungeonManager::new);
    }

    /**
     * Ticks all active dungeons in this level.
     * Called from DungeonEventHandler on ServerTickEvent.Post.
     */
    public void tick() {
        for (DungeonInstance dungeon : activeDungeons.values()) {
            dungeon.tick(level);
        }
    }

    /**
     * Registers a new dungeon instance.
     */
    public void registerDungeon(DungeonInstance dungeon) {
        activeDungeons.put(dungeon.getId(), dungeon);
        LOGGER.info("Registered dungeon: {}", dungeon.getId());
    }

    /**
     * Removes a dungeon instance (e.g., after reset completes).
     */
    public void unregisterDungeon(ResourceLocation dungeonId) {
        DungeonInstance removed = activeDungeons.remove(dungeonId);
        if (removed != null) {
            // Remove all player mappings for this dungeon
            playerDungeonMap.values().removeIf(d -> d == removed);
            LOGGER.info("Unregistered dungeon: {}", dungeonId);
        }
    }

    /**
     * Gets a dungeon instance by ID.
     */
    public DungeonInstance getDungeon(ResourceLocation id) {
        return activeDungeons.get(id);
    }

    /**
     * Gets the dungeon a player is currently in.
     */
    public DungeonInstance getPlayerDungeon(Player player) {
        return playerDungeonMap.get(player.getUUID());
    }

    /**
     * Handles a player entering a dungeon.
     */
    public void onPlayerEnterDungeon(ServerPlayer player, DungeonInstance dungeon) {
        playerDungeonMap.put(player.getUUID(), dungeon);

        if (dungeon.getState() == DungeonState.UNDISCOVERED) {
            dungeon.setState(DungeonState.DISCOVERED);
        }
        if (dungeon.getState() == DungeonState.DISCOVERED) {
            dungeon.setState(DungeonState.ACTIVE);
        }

        NeoForge.EVENT_BUS.post(new DungeonEnteredEvent(player, dungeon));
        LOGGER.debug("Player {} entered dungeon {}", player.getName().getString(), dungeon.getId());
    }

    /**
     * Handles a player leaving a dungeon.
     */
    public void onPlayerLeaveDungeon(ServerPlayer player) {
        DungeonInstance dungeon = playerDungeonMap.remove(player.getUUID());
        if (dungeon != null) {
            dungeon.removePlayer(player);

            // If no players remain, consider resetting
            if (dungeon.getPlayerCount() == 0 && dungeon.getState() == DungeonState.ACTIVE) {
                LOGGER.debug("Last player left dungeon {}, no active players remain", dungeon.getId());
            }
        }
    }

    /**
     * Handles room transition when a player moves between rooms within a dungeon.
     */
    public void handleRoomTransition(ServerPlayer player, BlockPos newPos) {
        DungeonInstance dungeon = playerDungeonMap.get(player.getUUID());
        if (dungeon == null) return;

        var currentRoom = dungeon.getRoomForPlayer(player);
        var newRoom = dungeon.findRoomAt(newPos);

        if (newRoom != null && newRoom != currentRoom) {
            if (currentRoom != null) {
                currentRoom.onPlayerExit(player);
            }
            newRoom.onPlayerEnter(player);
            dungeon.setPlayerRoom(player, newRoom);
        }
    }

    /**
     * Marks a dungeon as completed and fires the completion event.
     */
    public void completeDungeon(ServerPlayer player, DungeonInstance dungeon) {
        dungeon.setState(DungeonState.COMPLETED);
        NeoForge.EVENT_BUS.post(new DungeonCompletedEvent(player, dungeon));
        LOGGER.info("Dungeon {} completed by player {}", dungeon.getId(), player.getName().getString());
    }

    /**
     * Initiates a dungeon reset.
     */
    public void resetDungeon(DungeonInstance dungeon) {
        dungeon.setState(DungeonState.RESETTING);
        NeoForge.EVENT_BUS.post(new DungeonResetEvent(dungeon));
        LOGGER.info("Dungeon {} is resetting", dungeon.getId());
    }

    /**
     * Gets all active dungeons in this level.
     */
    public Collection<DungeonInstance> getActiveDungeons() {
        return Collections.unmodifiableCollection(activeDungeons.values());
    }

    /**
     * Gets the number of active dungeons.
     */
    public int getActiveDungeonCount() {
        return activeDungeons.size();
    }

    /**
     * Clears the static instance map (used during server shutdown).
     */
    public static void clearAll() {
        INSTANCES.clear();
    }
}
