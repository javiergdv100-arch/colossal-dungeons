package com.colossaldungeons.enhanced.block.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hidden Button block - invisible until revealed, then acts as a standard button.
 * 
 * Behavior:
 * - REVEALED property: when false, block is invisible (render shape INVISIBLE)
 * - Revealed by spyglass (within 10 blocks line of sight) or sculk sensor
 * - Once revealed: becomes visible and can be pressed
 * - PRESSED property: standard button behavior with redstone output
 */
public class HiddenButtonBlock extends Block {

    public static final BooleanProperty REVEALED = BooleanProperty.create("revealed");
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");

    /** Duration the button stays pressed (20 ticks = 1 second). */
    private static final int PRESS_DURATION = 20;

    /** Range at which a spyglass can reveal this block. */
    public static final int SPYGLASS_REVEAL_RANGE = 10;

    /** The button shape (small protrusion from wall). */
    private static final VoxelShape BUTTON_SHAPE = Block.box(5.0, 5.0, 14.0, 11.0, 11.0, 16.0);

    public HiddenButtonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(REVEALED, false)
            .setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REVEALED, PRESSED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(REVEALED)) {
            return Shapes.empty();
        }
        return BUTTON_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(REVEALED)) {
            return Shapes.empty();
        }
        return BUTTON_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        if (!state.getValue(REVEALED)) {
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!state.getValue(REVEALED)) {
            return InteractionResult.PASS;
        }

        if (!state.getValue(PRESSED)) {
            if (!level.isClientSide()) {
                // Press the button
                level.setBlock(pos, state.setValue(PRESSED, true), 3);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3f, 0.6f);

                // Notify neighbors about redstone change
                level.updateNeighborsAt(pos, this);

                // Schedule un-press
                level.scheduleTick(pos, this, PRESS_DURATION);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) {
            level.setBlock(pos, state.setValue(PRESSED, false), 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3f, 0.5f);
            level.updateNeighborsAt(pos, this);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(REVEALED);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(PRESSED) ? 15 : 0;
    }

    /**
     * Reveals this hidden button. Called when a player uses a spyglass
     * within range or when a sculk sensor detects activity near it.
     *
     * @param level The level
     * @param pos The block position
     * @param state Current block state
     */
    public static void reveal(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(REVEALED)) {
            level.setBlock(pos, state.setValue(REVEALED, true), 3);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.5f);
        }
    }
}
