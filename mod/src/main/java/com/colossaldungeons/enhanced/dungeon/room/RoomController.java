package com.colossaldungeons.enhanced.dungeon.room;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls which room subsystems are active based on the room's current state.
 * Implements the system activation table:
 * 
 * State       | AI  | BlockEntity | Traps | Puzzles | VFX | Sound
 * ------------|-----|-------------|-------|---------|-----|------
 * UNLOADED    | OFF | OFF         | OFF   | OFF     | OFF | OFF
 * PREPARED    | OFF | OFF         | OFF   | OFF     | ON  | ON
 * ACTIVE      | ON  | ON          | ON    | ON      | ON  | ON
 * COMPLETED   | OFF | ON          | OFF   | OFF     | ON  | ON
 * DORMANT     | OFF | OFF         | OFF   | OFF     | OFF | OFF
 * SUSPENDED   | OFF | OFF         | OFF   | OFF     | OFF | OFF
 */
public class RoomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomController.class);

    private final RoomInstance room;

    // System activation flags
    private boolean aiEnabled;
    private boolean blockEntityTickEnabled;
    private boolean trapsEnabled;
    private boolean puzzlesEnabled;
    private boolean vfxEnabled;
    private boolean soundEnabled;

    public RoomController(RoomInstance room) {
        this.room = room;
        applyStateConfig(room.getState());
    }

    /**
     * Called when the room transitions to a new state.
     * Updates all system activation flags per the activation table.
     */
    public void onStateChanged(RoomState newState) {
        applyStateConfig(newState);
        LOGGER.debug("Room {} state changed to {}: AI={}, Traps={}, Puzzles={}",
            room.getId(), newState, aiEnabled, trapsEnabled, puzzlesEnabled);
    }

    /**
     * Applies system configuration for the given room state.
     */
    private void applyStateConfig(RoomState state) {
        switch (state) {
            case UNLOADED -> {
                aiEnabled = false;
                blockEntityTickEnabled = false;
                trapsEnabled = false;
                puzzlesEnabled = false;
                vfxEnabled = false;
                soundEnabled = false;
            }
            case PREPARED -> {
                aiEnabled = false;
                blockEntityTickEnabled = false;
                trapsEnabled = false;
                puzzlesEnabled = false;
                vfxEnabled = true;
                soundEnabled = true;
            }
            case ACTIVE -> {
                aiEnabled = true;
                blockEntityTickEnabled = true;
                trapsEnabled = true;
                puzzlesEnabled = true;
                vfxEnabled = true;
                soundEnabled = true;
            }
            case COMPLETED -> {
                aiEnabled = false;
                blockEntityTickEnabled = true;
                trapsEnabled = false;
                puzzlesEnabled = false;
                vfxEnabled = true;
                soundEnabled = true;
            }
            case DORMANT, SUSPENDED -> {
                aiEnabled = false;
                blockEntityTickEnabled = false;
                trapsEnabled = false;
                puzzlesEnabled = false;
                vfxEnabled = false;
                soundEnabled = false;
            }
        }
    }

    /**
     * Ticks the room controller, processing only enabled systems.
     */
    public void tick(ServerLevel level) {
        // Only tick systems that are enabled for the current state
        if (trapsEnabled) {
            room.getTraps().forEach(trap -> trap.tick(level));
        }
    }

    // --- System state queries ---

    public boolean isAiEnabled() { return aiEnabled; }
    public boolean isBlockEntityTickEnabled() { return blockEntityTickEnabled; }
    public boolean isTrapsEnabled() { return trapsEnabled; }
    public boolean isPuzzlesEnabled() { return puzzlesEnabled; }
    public boolean isVfxEnabled() { return vfxEnabled; }
    public boolean isSoundEnabled() { return soundEnabled; }

    public RoomInstance getRoom() { return room; }
}
