package com.colossaldungeons.enhanced.block.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spike Plate trap block - extends spikes when stepped on.
 * 
 * Behavior:
 * - BooleanProperty EXTENDED: false = retracted (safe), true = spikes out
 * - When stepped on: extends spikes, deals damage to the entity
 * - Auto-retracts after 20 ticks (1 second)
 * - High blast resistance (50 hardness, 1200 blast resistance)
 * - Visual animation state for client-side rendering
 */
public class SpikePlateBlock extends Block {

    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");

    /** Shape when retracted (flush with ground). */
    private static final VoxelShape RETRACTED_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    /** Shape when extended (spikes protruding). */
    private static final VoxelShape EXTENDED_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

    /** Damage dealt when spikes extend. */
    private static final float SPIKE_DAMAGE = 6.0f;

    /** Ticks before auto-retraction. */
    private static final int RETRACT_DELAY = 20;

    public SpikePlateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EXTENDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EXTENDED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(EXTENDED) ? EXTENDED_SHAPE : RETRACTED_SHAPE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
            if (!state.getValue(EXTENDED)) {
                // Extend spikes
                level.setBlock(pos, state.setValue(EXTENDED, true), 3);

                // Deal damage
                livingEntity.hurt(livingEntity.damageSources().generic(), SPIKE_DAMAGE);

                // Play spike sound
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 1.5f);

                // Schedule retraction
                level.scheduleTick(pos, this, RETRACT_DELAY);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Auto-retract spikes
        if (state.getValue(EXTENDED)) {
            level.setBlock(pos, state.setValue(EXTENDED, false), 3);
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8f, 1.5f);
        }
    }
}
