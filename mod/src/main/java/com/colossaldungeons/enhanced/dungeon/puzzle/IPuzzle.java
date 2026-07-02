package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Interface defining the contract for all dungeon puzzles.
 * Puzzles are interactive challenges that players must solve to progress.
 */
public interface IPuzzle {

    /**
     * Gets the unique identifier for this puzzle instance.
     */
    ResourceLocation getId();

    /**
     * Gets the current state of this puzzle.
     */
    PuzzleState getState();

    /**
     * Ticks the puzzle logic each server tick.
     */
    void tick(ServerLevel level);

    /**
     * Called when a player interacts with this puzzle.
     *
     * @param player the interacting player
     * @param context the interaction context with position, item, etc.
     * @return true if the interaction was handled
     */
    boolean onPlayerInteract(ServerPlayer player, InteractionContext context);

    /**
     * Validates the current puzzle state/solution attempt.
     *
     * @return true if the current state is a valid solution
     */
    boolean validate();

    /**
     * Resets the puzzle to its initial state.
     */
    void reset();

    /**
     * Gets the reward resource locations to grant upon solving.
     */
    List<ResourceLocation> getReward();

    /**
     * Whether the puzzle has been completed (solved).
     */
    boolean isCompleted();
}
