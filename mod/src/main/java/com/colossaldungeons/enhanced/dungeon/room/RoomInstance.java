package com.colossaldungeons.enhanced.dungeon.room;

import com.colossaldungeons.enhanced.dungeon.trap.AbstractTrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime instance of a room within an active dungeon.
 * Manages room lifecycle, tracks players, and ticks traps/puzzles.
 */
public class RoomInstance {

    private final ResourceLocation id;
    private final RoomBounds bounds;
    private RoomState state;
    private final List<AbstractTrap> traps;
    private final List<ResourceLocation> puzzles;
    private final List<UUID> trackedEntities;
    private final Set<UUID> playersInRoom;
    private boolean tickEnabled;

    public RoomInstance(ResourceLocation id, RoomBounds bounds) {
        this.id = id;
        this.bounds = bounds;
        this.state = RoomState.UNLOADED;
        this.traps = new ArrayList<>();
        this.puzzles = new ArrayList<>();
        this.trackedEntities = new ArrayList<>();
        this.playersInRoom = new HashSet<>();
        this.tickEnabled = false;
    }

    /**
     * Called when a player enters this room's bounds.
     */
    public void onPlayerEnter(ServerPlayer player) {
        playersInRoom.add(player.getUUID());

        if (state == RoomState.DORMANT || state == RoomState.PREPARED) {
            transitionTo(RoomState.ACTIVE);
        }
        enableTick();
    }

    /**
     * Called when a player exits this room's bounds.
     */
    public void onPlayerExit(ServerPlayer player) {
        playersInRoom.remove(player.getUUID());

        if (playersInRoom.isEmpty() && state == RoomState.ACTIVE) {
            transitionTo(RoomState.DORMANT);
            disableTick();
        }
    }

    /**
     * Enable per-tick processing for this room.
     */
    public void enableTick() {
        this.tickEnabled = true;
    }

    /**
     * Disable per-tick processing for this room.
     */
    public void disableTick() {
        this.tickEnabled = false;
    }

    /**
     * Ticks this room's active systems (traps, puzzles, entities).
     */
    public void tick(ServerLevel level) {
        if (!tickEnabled) return;

        switch (state) {
            case ACTIVE -> {
                // Tick all traps
                for (AbstractTrap trap : traps) {
                    trap.tick(level);
                }
            }
            case PREPARED -> {
                // Pre-activation checks only
            }
            case COMPLETED -> {
                // Wind-down effects
                disableTick();
            }
            default -> {
                // No processing in other states
            }
        }
    }

    /**
     * Attempts to transition this room to a new state.
     * Returns true if the transition was valid and executed.
     */
    public boolean transitionTo(RoomState newState) {
        if (state.canTransitionTo(newState)) {
            this.state = newState;
            return true;
        }
        return false;
    }

    /**
     * Checks if a position is within this room's bounds.
     */
    public boolean containsPos(net.minecraft.core.BlockPos pos) {
        return bounds.contains(pos);
    }

    /**
     * Checks if an entity is within this room's bounds.
     */
    public boolean containsEntity(Entity entity) {
        return bounds.contains(entity);
    }

    // --- Trap Management ---

    public void addTrap(AbstractTrap trap) {
        traps.add(trap);
    }

    public void removeTrap(AbstractTrap trap) {
        traps.remove(trap);
    }

    public List<AbstractTrap> getTraps() {
        return traps;
    }

    // --- Puzzle Management ---

    public void addPuzzle(ResourceLocation puzzleId) {
        puzzles.add(puzzleId);
    }

    public List<ResourceLocation> getPuzzles() {
        return puzzles;
    }

    // --- Entity Tracking ---

    public void trackEntity(UUID entityId) {
        trackedEntities.add(entityId);
    }

    public void untrackEntity(UUID entityId) {
        trackedEntities.remove(entityId);
    }

    public List<UUID> getTrackedEntities() {
        return trackedEntities;
    }

    // --- Getters ---

    public ResourceLocation getId() { return id; }
    public RoomBounds getBounds() { return bounds; }
    public RoomState getState() { return state; }
    public Set<UUID> getPlayersInRoom() { return playersInRoom; }
    public boolean isTickEnabled() { return tickEnabled; }
    public int getPlayerCount() { return playersInRoom.size(); }
}
