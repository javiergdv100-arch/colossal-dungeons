package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Block entity for pushable blocks within dungeons.
 * Tracks position and handles push mechanics.
 * Player pushes the block in the direction they are facing.
 */
public class PushableBlockEntity extends MechanismBlockEntity {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
        ColossalDungeons.MOD_ID, "pushable_block");

    /** The original position of this block (for reset). */
    private BlockPos originalPos;
    /** Whether the block is currently sliding. */
    private boolean sliding;
    /** Direction the block is sliding. */
    private Direction slideDirection;
    /** Ticks remaining in current slide. */
    private int slideTicks;
    /** Maximum blocks this can be pushed. */
    private int maxPushDistance;

    public PushableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.originalPos = pos;
        this.sliding = false;
        this.slideDirection = Direction.NORTH;
        this.slideTicks = 0;
        this.maxPushDistance = 8;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        // Can be pushed when not already sliding
        return !sliding && (state == MechanismState.READY || state == MechanismState.INACTIVE);
    }

    @Override
    protected int getActivationDelay() {
        return 5; // Quick push
    }

    @Override
    protected int getDeactivationDelay() {
        return 5;
    }

    @Override
    protected void onActivate(ServerPlayer player, Level level) {
        // Determine push direction from player facing
        slideDirection = player.getDirection();
        sliding = true;
        slideTicks = 10; // Half second slide

        level.playSound(null, getBlockPos(), SoundEvents.STONE_STEP,
            SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    @Override
    protected void onDeactivate() {
        sliding = false;
        slideTicks = 0;
    }

    @Override
    protected void onActivationComplete(ServerLevel level) {
        // Move the block to the new position
        BlockPos targetPos = getBlockPos().relative(slideDirection);
        if (level.getBlockState(targetPos).isAir()) {
            BlockState currentState = level.getBlockState(getBlockPos());
            level.removeBlock(getBlockPos(), false);
            level.setBlock(targetPos, currentState, 3);
            // The new block at targetPos will get a new BlockEntity instance
            sliding = false;

            level.playSound(null, targetPos, SoundEvents.STONE_PLACE,
                SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        state = MechanismState.READY;
        setChanged();
    }

    @Override
    protected void onDeactivationComplete(ServerLevel level) {
        // Nothing special on deactivation
    }

    @Override
    protected void tickActive(ServerLevel level) {
        if (sliding) {
            slideTicks--;
            if (slideTicks <= 0) {
                sliding = false;
                deactivate();
            }
        }
    }

    /**
     * Resets this block to its original position.
     */
    public void resetToOriginal(ServerLevel level) {
        if (!getBlockPos().equals(originalPos)) {
            BlockState currentState = level.getBlockState(getBlockPos());
            level.removeBlock(getBlockPos(), false);
            level.setBlock(originalPos, currentState, 3);
            // New block entity will be created at the original position
        }
    }

    @Override
    protected void saveExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("original_pos", originalPos.asLong());
        tag.putBoolean("sliding", sliding);
        tag.putInt("slide_direction", slideDirection.get3DDataValue());
        tag.putInt("max_push_distance", maxPushDistance);
    }

    @Override
    protected void loadExtraData(CompoundTag tag, HolderLookup.Provider registries) {
        originalPos = BlockPos.of(tag.getLong("original_pos"));
        sliding = tag.getBoolean("sliding");
        slideDirection = Direction.from3DDataValue(tag.getInt("slide_direction"));
        maxPushDistance = tag.getInt("max_push_distance");
    }

    public BlockPos getOriginalPos() { return originalPos; }
    public boolean isSliding() { return sliding; }
}
