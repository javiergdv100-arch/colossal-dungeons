package com.colossaldungeons.enhanced.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ice Plate block - very slippery surface with high friction value.
 * 
 * Behavior:
 * - Extremely slippery (friction 0.99 set in block properties)
 * - Honey bottle interaction: temporarily removes slip (NEUTRALIZED = true)
 * - Used in Veiled Peak dungeon for sliding puzzles
 */
public class IcePlateBlock extends Block {

    /** Whether the ice plate's slipperiness has been neutralized by honey. */
    public static final BooleanProperty NEUTRALIZED = BooleanProperty.create("neutralized");

    /** Duration in ticks before honey wears off and slip returns. */
    private static final int HONEY_DURATION_TICKS = 600; // 30 seconds

    public IcePlateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NEUTRALIZED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NEUTRALIZED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Honey bottle removes slip temporarily
        if (stack.is(Items.HONEY_BOTTLE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(NEUTRALIZED, true), 3);
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

                // Schedule re-freeze
                level.scheduleTick(pos, this, HONEY_DURATION_TICKS);

                // Consume honey, give back glass bottle
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                     net.minecraft.util.RandomSource random) {
        // Honey wears off - restore slipperiness
        if (state.getValue(NEUTRALIZED)) {
            level.setBlock(pos, state.setValue(NEUTRALIZED, false), 3);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5f, 1.5f);
        }
    }

    @Override
    public float getFriction() {
        // High friction = more slippery in Minecraft's system (ice = 0.98)
        return 0.99f;
    }
}
