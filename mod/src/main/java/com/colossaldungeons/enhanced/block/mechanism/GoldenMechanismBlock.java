package com.colossaldungeons.enhanced.block.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Golden Mechanism block - activates when gold ingot is used (right-click).
 * 
 * Behavior:
 * - BooleanProperty ACTIVATED: false = dormant, true = active
 * - Activates when a player right-clicks with a gold ingot (consumes it)
 * - Once activated: emits redstone signal (level 15) and triggers connected mechanisms
 * - Cannot be deactivated once triggered (one-way mechanism)
 * - Provides a strong redstone signal when activated
 */
public class GoldenMechanismBlock extends Block {

    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");

    public GoldenMechanismBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Activate with gold ingot
        if (stack.is(Items.GOLD_INGOT) && !state.getValue(ACTIVATED)) {
            if (!level.isClientSide()) {
                // Consume gold ingot
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Activate the mechanism
                level.setBlock(pos, state.setValue(ACTIVATED, true), 3);
                level.playSound(null, pos, SoundEvents.COPPER_GRATE_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);

                // Notify neighbors of redstone change
                level.updateNeighborsAt(pos, this);
                for (Direction dir : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(dir), this);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(ACTIVATED);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(ACTIVATED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(ACTIVATED) ? 15 : 0;
    }
}
