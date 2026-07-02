package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A puzzle where the player must rotate mirrors to redirect a light beam to a target.
 * Each mirror has a rotation state (0-3, representing 4 cardinal/diagonal directions).
 * The solution is represented as a list of rotation values for each mirror position.
 */
public class LightRedirectPuzzle extends AbstractPuzzle {

    /** Current rotation state of each mirror (0-3). */
    private final List<Integer> mirrorRotations;
    /** BlockPos positions of the mirrors in order. */
    private final List<BlockPos> mirrorPositions;

    public LightRedirectPuzzle(ResourceLocation id, PuzzleConfig config) {
        super(id, config);
        this.mirrorRotations = new ArrayList<>();
        this.mirrorPositions = new ArrayList<>();

        // Initialize rotations to 0
        for (int i = 0; i < config.solution().size(); i++) {
            mirrorRotations.add(0);
        }
    }

    /**
     * Registers a mirror position. Must be called after construction to map
     * physical mirror blocks to puzzle indices.
     */
    public void addMirrorPosition(BlockPos pos) {
        mirrorPositions.add(pos);
    }

    @Override
    protected void onActivate() {
        // Reset all mirrors to default rotation
        for (int i = 0; i < mirrorRotations.size(); i++) {
            mirrorRotations.set(i, 0);
        }
    }

    @Override
    protected void processInput(ServerPlayer player, InteractionContext context) {
        BlockPos interactPos = context.pos();

        // Find which mirror was interacted with
        int mirrorIndex = -1;
        for (int i = 0; i < mirrorPositions.size(); i++) {
            if (mirrorPositions.get(i).equals(interactPos)) {
                mirrorIndex = i;
                break;
            }
        }

        if (mirrorIndex >= 0 && mirrorIndex < mirrorRotations.size()) {
            // Rotate the mirror by 90 degrees (increment mod 4)
            int current = mirrorRotations.get(mirrorIndex);
            mirrorRotations.set(mirrorIndex, (current + 1) % 4);
        }
    }

    @Override
    protected boolean checkSolution() {
        if (mirrorRotations.size() != config.solution().size()) return false;

        for (int i = 0; i < config.solution().size(); i++) {
            int expected;
            try {
                expected = Integer.parseInt(config.solution().get(i));
            } catch (NumberFormatException e) {
                return false;
            }
            if (!mirrorRotations.get(i).equals(expected)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doReset() {
        for (int i = 0; i < mirrorRotations.size(); i++) {
            mirrorRotations.set(i, 0);
        }
    }

    @Override
    public void tick(ServerLevel level) {
        super.tick(level);
    }

    /**
     * Gets the current mirror rotations for rendering.
     */
    public List<Integer> getMirrorRotations() {
        return List.copyOf(mirrorRotations);
    }
}
