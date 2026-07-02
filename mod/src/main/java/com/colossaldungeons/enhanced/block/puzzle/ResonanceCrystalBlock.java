package com.colossaldungeons.enhanced.block.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nullable;

/**
 * Resonance Crystal block - a puzzle element that charges over time.
 * 
 * Behavior:
 * - CHARGE_LEVEL IntegerProperty (0-5)
 * - Light level = CHARGE_LEVEL * 3 (max 15)
 * - Has associated BlockEntity (ResonanceCrystalEntity) for tick logic
 * - Drops self when mined
 * - Nearby players with ResonanceCharge effect increase charge speed
 */
public class ResonanceCrystalBlock extends BaseEntityBlock {

    public static final IntegerProperty CHARGE_LEVEL = IntegerProperty.create("charge_level", 0, 5);

    /** Maximum charge level. */
    public static final int MAX_CHARGE = 5;

    public ResonanceCrystalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE_LEVEL);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(CHARGE_LEVEL) * 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Returns the ResonanceCrystalEntity from the entity/block entity registry
        // The actual BlockEntity class was defined in FEAT-004 (entity system)
        return null; // Will be wired to CDEBlockEntities.RESONANCE_CRYSTAL.create(pos, state)
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Increases the charge level of this crystal by the given amount.
     * Called from the BlockEntity tick or from external puzzle logic.
     *
     * @param level The world level
     * @param pos The block position
     * @param state Current block state
     * @param amount Amount to increase charge by
     */
    public static void increaseCharge(Level level, BlockPos pos, BlockState state, int amount) {
        int currentCharge = state.getValue(CHARGE_LEVEL);
        int newCharge = Math.min(MAX_CHARGE, currentCharge + amount);
        if (newCharge != currentCharge) {
            level.setBlock(pos, state.setValue(CHARGE_LEVEL, newCharge), 3);
        }
    }

    /**
     * Resets the crystal charge to 0.
     */
    public static void resetCharge(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(CHARGE_LEVEL, 0), 3);
    }

    /**
     * Checks if the crystal is fully charged.
     */
    public static boolean isFullyCharged(BlockState state) {
        return state.getValue(CHARGE_LEVEL) >= MAX_CHARGE;
    }
}
