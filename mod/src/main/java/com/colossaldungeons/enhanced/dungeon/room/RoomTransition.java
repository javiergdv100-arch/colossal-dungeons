package com.colossaldungeons.enhanced.dungeon.room;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import com.colossaldungeons.enhanced.dungeon.RoomActivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates and executes transitions between room states.
 * Fires appropriate events when transitions occur.
 */
public class RoomTransition {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomTransition.class);

    /**
     * Result of a transition attempt.
     */
    public record TransitionResult(boolean success, RoomState fromState, RoomState toState, String reason) {
        public static TransitionResult success(RoomState from, RoomState to) {
            return new TransitionResult(true, from, to, "");
        }

        public static TransitionResult failure(RoomState from, RoomState to, String reason) {
            return new TransitionResult(false, from, to, reason);
        }
    }

    /**
     * Attempts to transition a room to a new state with full validation and event firing.
     *
     * @param room The room instance to transition
     * @param targetState The desired new state
     * @param trigger The player that triggered the transition (may be null)
     * @return The result of the transition attempt
     */
    public static TransitionResult execute(RoomInstance room, RoomState targetState, ServerPlayer trigger) {
        RoomState currentState = room.getState();

        // Validate the transition
        if (!currentState.canTransitionTo(targetState)) {
            LOGGER.warn("Invalid room transition attempted: {} -> {} for room {}",
                currentState, targetState, room.getId());
            return TransitionResult.failure(currentState, targetState,
                "Invalid transition from " + currentState + " to " + targetState);
        }

        // Execute the transition
        boolean success = room.transitionTo(targetState);
        if (!success) {
            return TransitionResult.failure(currentState, targetState, "Transition execution failed");
        }

        // Fire appropriate events
        fireTransitionEvents(room, currentState, targetState, trigger);

        LOGGER.debug("Room {} transitioned: {} -> {}", room.getId(), currentState, targetState);
        return TransitionResult.success(currentState, targetState);
    }

    /**
     * Fires events based on the type of transition that occurred.
     */
    private static void fireTransitionEvents(RoomInstance room, RoomState from, RoomState to, ServerPlayer trigger) {
        if (to == RoomState.ACTIVE && trigger != null) {
            NeoForge.EVENT_BUS.post(new RoomActivatedEvent(room, trigger));
        }
    }

    /**
     * Checks if a transition is valid without executing it.
     */
    public static boolean isValid(RoomState from, RoomState to) {
        return from.canTransitionTo(to);
    }
}
