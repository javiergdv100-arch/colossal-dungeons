package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base BlockEntity for mechanisms. Implements common state management,
 * Command pattern for queued activations, and NBT persistence.
 * Concrete mechanisms extend this class.
 */
public abstract class MechanismBlockEntity extends BlockEntity implements IMechanism {

    private static final Logger LOGGER = LoggerFactory.getLogger(MechanismBlockEntity.class);

    protected MechanismState state;
    protected int tickCounter;
    protected final List<MechanismCommand> commandQueue;

    protected MechanismBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.state = MechanismState.INACTIVE;
        this.tickCounter = 0;
        this.commandQueue = new ArrayList<>();
    }

    @Override
    public MechanismState getState() {
        return state;
    }

    @Override
    public void activate(ServerPlayer player, Level level) {
        if (state == MechanismState.READY) {
            state = MechanismState.ACTIVATING;
            tickCounter = 0;
            onActivate(player, level);
            setChanged();
        }
    }

    @Override
    public void deactivate() {
        if (state == MechanismState.ACTIVE) {
            state = MechanismState.DEACTIVATING;
            tickCounter = 0;
            onDeactivate();
            setChanged();
        }
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        return state == MechanismState.READY;
    }

    @Override
    public void tick(ServerLevel level) {
        tickCounter++;

        // Process queued commands
        if (!commandQueue.isEmpty()) {
            MechanismCommand cmd = commandQueue.remove(0);
            cmd.execute(this, level);
        }

        switch (state) {
            case ACTIVATING -> {
                if (tickCounter >= getActivationDelay()) {
                    state = MechanismState.ACTIVE;
                    tickCounter = 0;
                    onActivationComplete(level);
                    setChanged();
                }
            }
            case ACTIVE -> {
                tickActive(level);
            }
            case DEACTIVATING -> {
                if (tickCounter >= getDeactivationDelay()) {
                    state = MechanismState.READY;
                    tickCounter = 0;
                    onDeactivationComplete(level);
                    setChanged();
                }
            }
            default -> {
                // No processing
            }
        }
    }

    /**
     * Queues a command for execution on the next tick.
     */
    public void queueCommand(MechanismCommand command) {
        commandQueue.add(command);
    }

    /**
     * Sets the mechanism to READY state (used for initialization).
     */
    public void makeReady() {
        if (state == MechanismState.INACTIVE) {
            state = MechanismState.READY;
            setChanged();
        }
    }

    /**
     * Breaks the mechanism, setting it to BROKEN state.
     */
    public void breakMechanism() {
        state = MechanismState.BROKEN;
        tickCounter = 0;
        setChanged();
    }

    /**
     * Repairs the mechanism back to INACTIVE state.
     */
    public void repair() {
        if (state == MechanismState.BROKEN) {
            state = MechanismState.INACTIVE;
            setChanged();
        }
    }

    // ========== Persistence ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mechanism_state", state.ordinal());
        tag.putInt("tick_counter", tickCounter);
        saveExtraData(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int stateOrd = tag.getInt("mechanism_state");
        if (stateOrd >= 0 && stateOrd < MechanismState.values().length) {
            state = MechanismState.values()[stateOrd];
        }
        tickCounter = tag.getInt("tick_counter");
        loadExtraData(tag, registries);
    }

    // ========== Abstract Methods ==========

    /**
     * Delay in ticks for the activation process.
     */
    protected abstract int getActivationDelay();

    /**
     * Delay in ticks for the deactivation process.
     */
    protected abstract int getDeactivationDelay();

    /**
     * Called when activation begins.
     */
    protected abstract void onActivate(ServerPlayer player, Level level);

    /**
     * Called when deactivation begins.
     */
    protected abstract void onDeactivate();

    /**
     * Called when activation completes (transitions to ACTIVE).
     */
    protected abstract void onActivationComplete(ServerLevel level);

    /**
     * Called when deactivation completes (transitions to READY).
     */
    protected abstract void onDeactivationComplete(ServerLevel level);

    /**
     * Called each tick while in ACTIVE state.
     */
    protected abstract void tickActive(ServerLevel level);

    /**
     * Saves mechanism-specific data to NBT.
     */
    protected abstract void saveExtraData(CompoundTag tag, HolderLookup.Provider registries);

    /**
     * Loads mechanism-specific data from NBT.
     */
    protected abstract void loadExtraData(CompoundTag tag, HolderLookup.Provider registries);

    /**
     * Command pattern interface for queued mechanism actions.
     */
    @FunctionalInterface
    public interface MechanismCommand {
        void execute(MechanismBlockEntity mechanism, ServerLevel level);
    }
}
