package com.colossaldungeons.enhanced.dungeon.puzzle;

/**
 * Lifecycle states for dungeon puzzles.
 */
public enum PuzzleState {
    /** Puzzle has not been activated yet. */
    INACTIVE,
    /** Puzzle is active and waiting for player interaction. */
    ACTIVE,
    /** Player has started interacting with the puzzle. */
    IN_PROGRESS,
    /** Puzzle has been solved successfully. */
    SOLVED,
    /** Puzzle attempt failed (max attempts or time expired). */
    FAILED,
    /** Puzzle has been reset and can be attempted again. */
    RESET;

    /**
     * Checks if this state can transition to the target state.
     */
    public boolean canTransitionTo(PuzzleState target) {
        return switch (this) {
            case INACTIVE -> target == ACTIVE;
            case ACTIVE -> target == IN_PROGRESS || target == RESET;
            case IN_PROGRESS -> target == SOLVED || target == FAILED || target == RESET;
            case SOLVED -> target == RESET || target == INACTIVE;
            case FAILED -> target == RESET || target == INACTIVE;
            case RESET -> target == ACTIVE || target == INACTIVE;
        };
    }
}
