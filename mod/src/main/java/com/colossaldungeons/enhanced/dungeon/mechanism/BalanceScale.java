package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.colossaldungeons.enhanced.ColossalDungeons;

/**
 * Balance scale mechanism. Weighs items placed on it and activates when
 * the target weight is matched. Connected to the WeightBalancePuzzle system.
 */
public class BalanceScale extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "balance_scale");

    private double currentWeight;
    private double targetWeight;
    private double tolerance;

    public BalanceScale(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.currentWeight = 0.0;
        this.targetWeight = 10.0;
        this.tolerance = 0.5;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        // Scale activates automatically when weight matches - not manual activation
        return false;
    }

    @Override
    protected int getActivationDelay() {
        return 10; // Short delay for scale tipping animation
    }

    @Override
    protected int getDeactivationDelay() {
        return 10;
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_BREAK,
            SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    protected void onDeactivate() {
        // Scale unbalanced
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_PLACE,
            SoundSource.BLOCKS, 1.0f, 1.5f);
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        // Ready for re-balancing
    }

    @Override
    protected void tickActive(ServerLevel level) {
        // Check if weight is still balanced
        if (Math.abs(currentWeight - targetWeight) > tolerance) {
            deactivate();
        }
    }

    /**
     * Sets the weight on this scale. Called by the puzzle system when items are placed.
     */
    public void setCurrentWeight(double weight) {
        this.currentWeight = weight;
        setChanged();

        // Auto-activate when balanced
        if (Math.abs(currentWeight - targetWeight) <= tolerance && state == MechanismState.READY) {
            state = MechanismState.ACTIVATING;
            tickCounter = 0;
            setChanged();
        }
    }

    /**
     * Configures the target weight for this scale.
     */
    public void setTargetWeight(double target, double tolerance) {
        this.targetWeight = target;
        this.tolerance = tolerance;
        setChanged();
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("current_weight", currentWeight);
        tag.putDouble("target_weight", targetWeight);
        tag.putDouble("tolerance", tolerance);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        currentWeight = tag.getDouble("current_weight");
        targetWeight = tag.getDouble("target_weight");
        tolerance = tag.getDouble("tolerance");
    }

    public double getCurrentWeight() { return currentWeight; }
    public double getTargetWeight() { return targetWeight; }
}
