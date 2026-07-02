package com.colossaldungeons.enhanced.block.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Mirror Panel block - directional, rotatable mirror that redirects light beams.
 * 
 * Behavior:
 * - DirectionProperty FACING: which direction the mirror faces
 * - IntegerProperty ROTATION (0-3): fine rotation within facing direction
 * - Can be rotated by right-click (hand) or fishing rod interaction
 * - Redirects light beams as tracked by the puzzle engine
 * - Each rotation step is 90 degrees
 */
public class MirrorPanelBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    public MirrorPanelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(ROTATION, 0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            rotatePanel(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Rotates the mirror panel by one step (90 degrees).
     * Called by right-click or fishing rod interaction.
     */
    public static void rotatePanel(Level level, BlockPos pos, BlockState state) {
        int currentRotation = state.getValue(ROTATION);
        int newRotation = (currentRotation + 1) % 4;
        level.setBlock(pos, state.setValue(ROTATION, newRotation), 3);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
    }

    /**
     * Calculates the output direction of a light beam based on the mirror's
     * facing and rotation.
     *
     * @param inputDirection The direction the light beam is coming from
     * @param state Current block state
     * @return The direction the light beam exits, or null if blocked
     */
    public static Direction getReflectedDirection(Direction inputDirection, BlockState state) {
        Direction facing = state.getValue(FACING);
        int rotation = state.getValue(ROTATION);

        // Simple reflection logic: mirror reflects perpendicular to facing
        // Rotation offsets the reflection angle by 90-degree increments
        Direction baseReflection = inputDirection.getOpposite();

        // Apply rotation offset
        for (int i = 0; i < rotation; i++) {
            baseReflection = baseReflection.getClockWise();
        }

        return baseReflection;
    }
}
