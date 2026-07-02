package com.colossaldungeons.enhanced.block.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Pushable Block - can be pushed by players (shift+right-click) and pistons.
 * 
 * Behavior:
 * - Shift+right-click: pushes block in the direction the player is facing
 * - Pistons work normally on this block (default Block behavior)
 * - Used in weight puzzles where blocks must be positioned correctly
 * - Only moves if the destination is air or replaceable
 */
public class PushableBlock extends Block {

    public PushableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        // Only push when shift+right-click
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            Direction pushDirection = player.getDirection();
            BlockPos targetPos = pos.relative(pushDirection);

            // Check if destination is empty or replaceable
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir() || targetState.canBeReplaced()) {
                // Also check that there's a solid block below the target
                BlockPos belowTarget = targetPos.below();
                BlockState belowState = level.getBlockState(belowTarget);

                if (belowState.isFaceSturdy(level, belowTarget, Direction.UP)) {
                    // Move the block
                    level.removeBlock(pos, false);
                    level.setBlock(targetPos, state, 3);
                    level.playSound(null, targetPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
