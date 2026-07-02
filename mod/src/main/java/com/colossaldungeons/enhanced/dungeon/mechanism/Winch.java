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
 * Winch/crank mechanism. Requires player to hold interaction to raise or lower
 * a connected element (gate, drawbridge, platform).
 * Player must keep interacting to maintain position; releases reset over time.
 */
public class Winch extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "winch");

    /** Current crank position (0.0 = fully lowered, 1.0 = fully raised). */
    private float crankPosition;
    /** Ticks since last player interaction. */
    private int ticksSinceInteraction;
    /** How many ticks of interaction required for full activation. */
    private int requiredCrankTicks;
    /** Whether the winch is being held by a player. */
    private boolean beingCranked;

    public Winch(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.crankPosition = 0.0f;
        this.ticksSinceInteraction = 0;
        this.requiredCrankTicks = 60; // 3 seconds of cranking
        this.beingCranked = false;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        return state == MechanismState.READY || state == MechanismState.ACTIVE;
    }

    @Override
    protected int getActivationDelay() {
        return requiredCrankTicks;
    }

    @Override
    protected int getDeactivationDelay() {
        return requiredCrankTicks; // Same time to lower
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        beingCranked = true;
        ticksSinceInteraction = 0;
        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_STEP,
            SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    @Override
    protected void onDeactivate() {
        beingCranked = false;
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        crankPosition = 1.0f;
        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_PLACE,
            SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        crankPosition = 0.0f;
        level.playSound(null, getBlockPos(), SoundEvents.CHAIN_BREAK,
            SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    @Override
    protected void tickActive(ServerLevel level) {
        ticksSinceInteraction++;

        // If not being cranked, slowly lower
        if (!beingCranked && ticksSinceInteraction > 20) {
            crankPosition = Math.max(0.0f, crankPosition - 0.02f);
            if (crankPosition <= 0.0f) {
                deactivate();
            }
            setChanged();
        }
    }

    /**
     * Called each tick while a player is holding interaction on the winch.
     */
    public void onCrankTick(ServerPlayer player) {
        beingCranked = true;
        ticksSinceInteraction = 0;

        if (state == MechanismState.READY) {
            activate(player, player.level());
        } else if (state == MechanismState.ACTIVE) {
            crankPosition = Math.min(1.0f, crankPosition + (1.0f / requiredCrankTicks));
            setChanged();
        }
    }

    /**
     * Called when a player stops interacting.
     */
    public void onCrankRelease() {
        beingCranked = false;
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("crank_position", crankPosition);
        tag.putInt("required_crank_ticks", requiredCrankTicks);
        tag.putBoolean("being_cranked", beingCranked);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        crankPosition = tag.getFloat("crank_position");
        requiredCrankTicks = tag.getInt("required_crank_ticks");
        beingCranked = tag.getBoolean("being_cranked");
    }

    public float getCrankPosition() { return crankPosition; }
    public boolean isBeingCranked() { return beingCranked; }
}
