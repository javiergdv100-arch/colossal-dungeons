package com.colossaldungeons.enhanced.block.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Brazier block - lit/unlit state, ignitable by flint and steel.
 * 
 * Behavior:
 * - BooleanProperty LIT: false = unlit (dark), true = lit (light 15)
 * - Ignitable by flint and steel (right-click with flint_and_steel)
 * - Extinguished by water (rain, water bucket, etc.)
 * - Light level 15 when lit, 0 when unlit
 * - Spawns fire particles when lit
 */
public class BrazierBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BrazierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Light with flint and steel
        if (stack.is(Items.FLINT_AND_STEEL) && !state.getValue(LIT)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

                // Damage flint and steel
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // Extinguish with water bucket
        if (stack.is(Items.WATER_BUCKET) && state.getValue(LIT)) {
            if (!level.isClientSide()) {
                extinguish(level, pos, state);
                // Replace water bucket with empty bucket
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Extinguishes the brazier with steam effects.
     */
    public static void extinguish(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LIT, false), 3);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            // Fire and smoke particles
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            level.addParticle(ParticleTypes.FLAME, x + random.nextDouble() * 0.4 - 0.2,
                y, z + random.nextDouble() * 0.4 - 0.2, 0.0, 0.02, 0.0);
            level.addParticle(ParticleTypes.SMOKE, x + random.nextDouble() * 0.4 - 0.2,
                y + 0.3, z + random.nextDouble() * 0.4 - 0.2, 0.0, 0.03, 0.0);
        }
    }
}
