package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.colossaldungeons.enhanced.ColossalDungeons;

/**
 * Golden mechanism block entity. Activated by gold items (gold ingots, gold blocks,
 * golden apples). When activated, triggers a chain reaction that can open doors,
 * reveal passages, or activate other mechanisms.
 */
public class GoldenMechanismEntity extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "golden_mechanism");

    /** The amount of gold "power" stored. */
    private int goldPower;
    /** Required gold power to fully activate. */
    private int requiredPower;
    /** Whether the mechanism has been fully charged. */
    private boolean fullyCharged;

    public GoldenMechanismEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.goldPower = 0;
        this.requiredPower = 3; // Default: 3 gold ingots
        this.fullyCharged = false;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        if (!super.canActivate(player)) return false;
        return isGoldItem(player.getMainHandItem());
    }

    private boolean isGoldItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        return itemId.contains("gold") || itemId.contains("golden");
    }

    private int getGoldValue(ItemStack stack) {
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        if (itemId.contains("gold_block")) return 9;
        if (itemId.contains("golden_apple")) return 4;
        if (itemId.contains("gold_ingot")) return 1;
        if (itemId.contains("gold_nugget")) return 0; // Not enough
        return 1; // Default for other gold items
    }

    @Override
    protected int getActivationDelay() {
        return 30; // 1.5 seconds
    }

    @Override
    protected int getDeactivationDelay() {
        return 60; // 3 seconds
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        ItemStack held = player.getMainHandItem();
        int value = getGoldValue(held);
        goldPower += value;
        held.shrink(1);

        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_PLACE,
            SoundSource.BLOCKS, 1.0f, 1.0f + (goldPower * 0.1f));

        if (goldPower >= requiredPower) {
            fullyCharged = true;
        } else {
            // Not enough gold yet - go back to READY for more input
            state = MechanismState.READY;
        }
    }

    @Override
    protected void onDeactivate() {
        fullyCharged = false;
        goldPower = 0;
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        if (fullyCharged) {
            level.playSound(null, getBlockPos(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.0f, 1.5f);
            // Trigger chain reaction - connected mechanisms would be notified here
        }
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        level.playSound(null, getBlockPos(), SoundEvents.BEACON_DEACTIVATE,
            SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    @Override
    protected void tickActive(ServerLevel level) {
        // Golden glow effect while active
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("gold_power", goldPower);
        tag.putInt("required_power", requiredPower);
        tag.putBoolean("fully_charged", fullyCharged);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        goldPower = tag.getInt("gold_power");
        requiredPower = tag.getInt("required_power");
        fullyCharged = tag.getBoolean("fully_charged");
    }

    public int getGoldPower() { return goldPower; }
    public int getRequiredPower() { return requiredPower; }
    public boolean isFullyCharged() { return fullyCharged; }

    public void setRequiredPower(int required) {
        this.requiredPower = required;
        setChanged();
    }
}
