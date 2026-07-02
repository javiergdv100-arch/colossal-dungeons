package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A puzzle where the player must play note blocks in the correct sequence.
 * The solution is a list of note values (as strings) that must be triggered in order.
 * Players interact with note blocks at specific positions; each note block maps to a note value.
 */
public class SoundSequencePuzzle extends AbstractPuzzle {

    private final List<String> playerSequence;

    public SoundSequencePuzzle(ResourceLocation id, PuzzleConfig config) {
        super(id, config);
        this.playerSequence = new ArrayList<>();
    }

    @Override
    protected void onActivate() {
        playerSequence.clear();
    }

    @Override
    protected void processInput(ServerPlayer player, InteractionContext context) {
        // The interaction context's block position determines which note was played
        BlockPos pos = context.pos();
        String noteValue = pos.getX() + "," + pos.getY() + "," + pos.getZ();

        // In practice, a mapping from BlockPos to note name would be configured
        // For now we use the block position as a string identifier
        playerSequence.add(noteValue);

        // Play feedback sound
        player.serverLevel().playSound(null, player.blockPosition(),
            SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 1.0f);

        // Check for wrong input immediately
        if (!PuzzleValidator.validatePartialSequence(playerSequence, config.solution())) {
            playerSequence.clear();
            onFailed();
        }
    }

    @Override
    protected boolean checkSolution() {
        return PuzzleValidator.validateSequence(playerSequence, config.solution());
    }

    @Override
    protected void doReset() {
        playerSequence.clear();
    }

    @Override
    public void tick(ServerLevel level) {
        super.tick(level);
        // Could add timeout per input here
    }

    /**
     * Gets the current player sequence for debugging/display.
     */
    public List<String> getPlayerSequence() {
        return List.copyOf(playerSequence);
    }
}
