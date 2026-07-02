package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.dungeon.room.RoomInstance;
import com.colossaldungeons.enhanced.network.CDENetworking;
import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages active puzzles within a room.
 * Handles puzzle registration, ticking, interaction dispatch, and reward distribution.
 */
public class PuzzleEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PuzzleEngine.class);

    private final Map<ResourceLocation, IPuzzle> activePuzzles;
    private final Map<ResourceLocation, List<IPuzzle>> puzzlesByRoom;

    public PuzzleEngine() {
        this.activePuzzles = new HashMap<>();
        this.puzzlesByRoom = new HashMap<>();
    }

    /**
     * Registers a puzzle and associates it with a room.
     */
    public void registerPuzzle(IPuzzle puzzle, ResourceLocation roomId) {
        activePuzzles.put(puzzle.getId(), puzzle);
        puzzlesByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(puzzle);
        LOGGER.debug("Registered puzzle {} in room {}", puzzle.getId(), roomId);
    }

    /**
     * Unregisters a puzzle.
     */
    public void unregisterPuzzle(ResourceLocation puzzleId) {
        IPuzzle removed = activePuzzles.remove(puzzleId);
        if (removed != null) {
            puzzlesByRoom.values().forEach(list -> list.removeIf(p -> p.getId().equals(puzzleId)));
        }
    }

    /**
     * Gets a puzzle by ID.
     */
    public IPuzzle getPuzzle(ResourceLocation id) {
        return activePuzzles.get(id);
    }

    /**
     * Gets all puzzles for a given room.
     */
    public List<IPuzzle> getPuzzlesForRoom(ResourceLocation roomId) {
        return puzzlesByRoom.getOrDefault(roomId, Collections.emptyList());
    }

    /**
     * Ticks all active puzzles.
     */
    public void tick(ServerLevel level) {
        for (IPuzzle puzzle : activePuzzles.values()) {
            PuzzleState prevState = puzzle.getState();
            puzzle.tick(level);

            // Send state update if state changed
            if (puzzle.getState() != prevState) {
                syncPuzzleState(puzzle, level);
            }
        }
    }

    /**
     * Dispatches a player interaction to the appropriate puzzle.
     *
     * @param player the interacting player
     * @param puzzleId the target puzzle ID
     * @param context the interaction context
     * @return true if the interaction was handled
     */
    public boolean dispatchInteraction(ServerPlayer player, ResourceLocation puzzleId, InteractionContext context) {
        IPuzzle puzzle = activePuzzles.get(puzzleId);
        if (puzzle == null) {
            LOGGER.warn("Attempted interaction with unknown puzzle: {}", puzzleId);
            return false;
        }

        boolean handled = puzzle.onPlayerInteract(player, context);

        if (handled) {
            syncPuzzleState(puzzle, player.serverLevel());

            if (puzzle.isCompleted()) {
                onPuzzleSolved(player, puzzle);
            }
        }

        return handled;
    }

    /**
     * Dispatches an interaction to any puzzle in the room that can handle it.
     *
     * @param player the interacting player
     * @param room the room the player is in
     * @param context the interaction context
     * @return true if any puzzle handled the interaction
     */
    public boolean dispatchInteractionToRoom(ServerPlayer player, RoomInstance room, InteractionContext context) {
        List<IPuzzle> roomPuzzles = puzzlesByRoom.get(room.getId());
        if (roomPuzzles == null || roomPuzzles.isEmpty()) return false;

        for (IPuzzle puzzle : roomPuzzles) {
            if (puzzle.getState() == PuzzleState.ACTIVE || puzzle.getState() == PuzzleState.IN_PROGRESS) {
                if (puzzle.onPlayerInteract(player, context)) {
                    syncPuzzleState(puzzle, player.serverLevel());
                    if (puzzle.isCompleted()) {
                        onPuzzleSolved(player, puzzle);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when a puzzle is solved. Distributes rewards.
     */
    private void onPuzzleSolved(ServerPlayer solver, IPuzzle puzzle) {
        LOGGER.info("Puzzle {} solved by player {}", puzzle.getId(), solver.getName().getString());

        List<ResourceLocation> rewards = puzzle.getReward();
        if (rewards != null && !rewards.isEmpty()) {
            // Rewards are ResourceLocations referencing loot tables or reward definitions
            // Actual reward granting would be handled by a reward system
            LOGGER.debug("Granting {} rewards for puzzle {}", rewards.size(), puzzle.getId());
        }
    }

    /**
     * Syncs puzzle state to all tracking players via network.
     */
    private void syncPuzzleState(IPuzzle puzzle, ServerLevel level) {
        int progress = 0;
        int maxProgress = 1;

        if (puzzle instanceof AbstractPuzzle abstractPuzzle) {
            PuzzleConfig config = abstractPuzzle.getConfig();
            maxProgress = config.solution().size();
            // Progress based on state
            progress = switch (puzzle.getState()) {
                case SOLVED -> maxProgress;
                case FAILED -> 0;
                default -> abstractPuzzle.getAttempts();
            };
        }

        CDENetworking.PuzzleStatePayload payload = new CDENetworking.PuzzleStatePayload(
            puzzle.getId(),
            progress,
            maxProgress,
            puzzle.isCompleted()
        );

        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    /**
     * Resets all puzzles in a room.
     */
    public void resetRoomPuzzles(ResourceLocation roomId) {
        List<IPuzzle> roomPuzzles = puzzlesByRoom.get(roomId);
        if (roomPuzzles != null) {
            roomPuzzles.forEach(IPuzzle::reset);
            LOGGER.debug("Reset all puzzles in room {}", roomId);
        }
    }

    /**
     * Checks if all puzzles in a room are solved.
     */
    public boolean areAllPuzzlesSolved(ResourceLocation roomId) {
        List<IPuzzle> roomPuzzles = puzzlesByRoom.get(roomId);
        if (roomPuzzles == null || roomPuzzles.isEmpty()) return true;
        return roomPuzzles.stream().allMatch(IPuzzle::isCompleted);
    }

    /**
     * Gets the total number of active puzzles.
     */
    public int getActivePuzzleCount() {
        return activePuzzles.size();
    }

    /**
     * Clears all puzzles (used during dungeon reset).
     */
    public void clearAll() {
        activePuzzles.clear();
        puzzlesByRoom.clear();
    }
}
