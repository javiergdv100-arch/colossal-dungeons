package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Template Method base class for all dungeon puzzles.
 * Provides the lifecycle framework; subclasses implement the puzzle-specific logic.
 */
public abstract class AbstractPuzzle implements IPuzzle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPuzzle.class);

    protected final ResourceLocation id;
    protected final PuzzleConfig config;
    protected PuzzleState state;
    protected int attempts;
    protected int ticksActive;

    protected AbstractPuzzle(ResourceLocation id, PuzzleConfig config) {
        this.id = id;
        this.config = config;
        this.state = PuzzleState.INACTIVE;
        this.attempts = 0;
        this.ticksActive = 0;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public PuzzleState getState() {
        return state;
    }

    @Override
    public void tick(ServerLevel level) {
        if (state == PuzzleState.IN_PROGRESS) {
            ticksActive++;
            if (config.timeLimitTicks() > 0 && ticksActive >= config.timeLimitTicks()) {
                onFailed();
            }
        }
    }

    @Override
    public boolean onPlayerInteract(ServerPlayer player, InteractionContext context) {
        if (state == PuzzleState.INACTIVE) {
            activate();
            return true;
        }
        if (state == PuzzleState.ACTIVE || state == PuzzleState.IN_PROGRESS) {
            if (state == PuzzleState.ACTIVE) {
                state = PuzzleState.IN_PROGRESS;
                ticksActive = 0;
            }
            processInput(player, context);
            if (checkSolution()) {
                onSolved(player);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean validate() {
        return checkSolution();
    }

    @Override
    public void reset() {
        state = PuzzleState.RESET;
        attempts = 0;
        ticksActive = 0;
        doReset();
        state = PuzzleState.INACTIVE;
        LOGGER.debug("Puzzle {} has been reset", id);
    }

    @Override
    public List<ResourceLocation> getReward() {
        return config.rewards();
    }

    @Override
    public boolean isCompleted() {
        return state == PuzzleState.SOLVED;
    }

    /**
     * Activates the puzzle, transitioning from INACTIVE to ACTIVE.
     */
    public void activate() {
        if (state == PuzzleState.INACTIVE || state == PuzzleState.RESET) {
            state = PuzzleState.ACTIVE;
            ticksActive = 0;
            onActivate();
            LOGGER.debug("Puzzle {} activated", id);
        }
    }

    /**
     * Called when the puzzle is solved.
     */
    protected void onSolved(ServerPlayer solver) {
        state = PuzzleState.SOLVED;
        LOGGER.info("Puzzle {} solved by {}", id, solver.getName().getString());
    }

    /**
     * Called when the puzzle attempt fails.
     */
    protected void onFailed() {
        attempts++;
        if (attempts >= config.maxAttempts()) {
            state = PuzzleState.FAILED;
            LOGGER.info("Puzzle {} failed after {} attempts", id, attempts);
        } else {
            // Allow retry within the same activation
            doReset();
            state = PuzzleState.ACTIVE;
            LOGGER.debug("Puzzle {} attempt {} failed, {} remaining", id, attempts, config.maxAttempts() - attempts);
        }
    }

    // ========== Abstract Template Methods ==========

    /**
     * Called when the puzzle first activates.
     */
    protected abstract void onActivate();

    /**
     * Processes player input during the puzzle.
     */
    protected abstract void processInput(ServerPlayer player, InteractionContext context);

    /**
     * Checks if the current state constitutes a valid solution.
     */
    protected abstract boolean checkSolution();

    /**
     * Resets puzzle-specific state for a new attempt or full reset.
     */
    protected abstract void doReset();

    // ========== Getters ==========

    public PuzzleConfig getConfig() {
        return config;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getTicksActive() {
        return ticksActive;
    }
}
