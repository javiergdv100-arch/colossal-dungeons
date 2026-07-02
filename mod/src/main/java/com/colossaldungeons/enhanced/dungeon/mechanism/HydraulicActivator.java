package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.colossaldungeons.enhanced.ColossalDungeons;

/**
 * Hydraulic activator mechanism. Fills with a water bucket to activate
 * connected mechanisms via hydraulic pressure.
 */
public class HydraulicActivator extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "hydraulic_activator");

    private boolean filled;

    public HydraulicActivator(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.filled = false;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        if (!super.canActivate(player)) return false;
        // Requires water bucket in hand
        return player.getMainHandItem().is(Items.WATER_BUCKET);
    }

    @Override
    protected int getActivationDelay() {
        return 20; // 1 second fill time
    }

    @Override
    protected int getDeactivationDelay() {
        return 40; // 2 seconds drain time
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        filled = true;
        // Consume water bucket, give back empty bucket
        player.getMainHandItem().shrink(1);
        player.getInventory().add(Items.BUCKET.getDefaultInstance());

        level.playSound(null, getBlockPos(), SoundEvents.BUCKET_EMPTY,
            SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    @Override
    protected void onDeactivate() {
        filled = false;
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        // Trigger connected mechanisms
        level.playSound(null, getBlockPos(), SoundEvents.PISTON_EXTEND,
            SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        level.playSound(null, getBlockPos(), SoundEvents.PISTON_CONTRACT,
            SoundSource.BLOCKS, 0.8f, 0.8f);
    }

    @Override
    protected void tickActive(ServerLevel level) {
        // Hydraulic pressure is maintained while active
        // Connected mechanisms remain activated
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("filled", filled);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        filled = tag.getBoolean("filled");
    }

    public boolean isFilled() {
        return filled;
    }
}
