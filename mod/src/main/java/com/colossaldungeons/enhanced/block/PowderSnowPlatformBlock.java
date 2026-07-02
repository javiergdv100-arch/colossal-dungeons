package com.colossaldungeons.enhanced.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Powder Snow Platform - temporary platform that self-destructs after a configurable duration.
 * 
 * Behavior:
 * - Self-destructs after LIFETIME ticks (300-600, randomized on placement)
 * - Semi-transparent appearance with snow particles
 * - AGE property tracks remaining time categories
 */
public class PowderSnowPlatformBlock extends Block {

    /** Age of the block (0=fresh, 1=aging, 2=crumbling, 3=about_to_break). */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    /** Minimum lifetime in ticks (15 seconds). */
    public static final int MIN_LIFETIME = 300;

    /** Maximum lifetime in ticks (30 seconds). */
    public static final int MAX_LIFETIME = 600;

    /** Ticks per age increment. */
    private static final int TICKS_PER_AGE = 100;

    public PowderSnowPlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            // Schedule first age tick
            int initialDelay = MIN_LIFETIME + level.random.nextInt(MAX_LIFETIME - MIN_LIFETIME);
            level.scheduleTick(pos, this, initialDelay / 4);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        if (age >= 3) {
            // Break the block
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

            // Snow particles
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.5, 0.3, 0.5, 0.02);
        } else {
            // Age the block
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            // Schedule next tick
            level.scheduleTick(pos, this, TICKS_PER_AGE);
        }
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.8f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
