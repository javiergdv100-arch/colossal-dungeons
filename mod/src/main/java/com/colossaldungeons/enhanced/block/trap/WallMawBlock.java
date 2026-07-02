package com.colossaldungeons.enhanced.block.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Wall Maw block - animated mouth in wall that bites nearby players.
 * 
 * Behavior:
 * - Faces a direction (FACING property from HorizontalDirectionalBlock)
 * - Bites players who get too close (within 2 blocks in front)
 * - Has cooldown between bites (COOLDOWN state, 60 ticks)
 * - Animated mouth states for client rendering
 */
public class WallMawBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty BITING = BooleanProperty.create("biting");

    /** Detection range in blocks (in front of the maw). */
    private static final double DETECTION_RANGE = 2.0;

    /** Damage dealt per bite. */
    private static final float BITE_DAMAGE = 8.0f;

    /** Cooldown between bites in ticks. */
    private static final int BITE_COOLDOWN = 60;

    /** Ticks the biting animation lasts. */
    private static final int BITE_ANIMATION_TICKS = 10;

    public WallMawBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(BITING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BITING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            // Start the detection tick loop
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(BITING)) {
            // End bite animation, start cooldown
            level.setBlock(pos, state.setValue(BITING, false), 3);
            level.scheduleTick(pos, this, BITE_COOLDOWN);
            return;
        }

        // Detection logic: check for entities in front
        Direction facing = state.getValue(FACING);
        AABB detectionBox = createDetectionBox(pos, facing);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, detectionBox);

        if (!targets.isEmpty()) {
            // Bite the closest target
            LivingEntity target = targets.get(0);
            target.hurt(target.damageSources().generic(), BITE_DAMAGE);

            // Set biting state
            level.setBlock(pos, state.setValue(BITING, true), 3);
            level.playSound(null, pos, SoundEvents.WOLF_AMBIENT, SoundSource.BLOCKS, 1.0f, 0.5f);

            // Schedule end of bite animation
            level.scheduleTick(pos, this, BITE_ANIMATION_TICKS);
        } else {
            // No target, check again soon
            level.scheduleTick(pos, this, 10);
        }
    }

    private AABB createDetectionBox(BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        double dx = facing.getStepX() * DETECTION_RANGE;
        double dz = facing.getStepZ() * DETECTION_RANGE;

        return new AABB(
            x, y, z,
            x + dx, y + 2.0, z + dz
        ).inflate(0.5);
    }
}
