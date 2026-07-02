package com.colossaldungeons.enhanced.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Interface for room effects that can be applied to dungeon rooms.
 * Room effects modify the environment or apply conditions to entities within a room.
 */
public interface IRoomEffect extends IDungeonContent {

    /**
     * Gets the unique identifier for this room effect.
     */
    @Override
    ResourceLocation getId();

    /**
     * Applies the effect to the room. Called when the effect becomes active.
     *
     * @param roomId the identifier of the room to apply the effect to
     */
    void apply(ResourceLocation roomId);

    /**
     * Removes the effect from the room. Called when the effect is deactivated.
     *
     * @param roomId the identifier of the room
     */
    void remove(ResourceLocation roomId);

    /**
     * Ticks the effect each server tick while active.
     */
    void tick();

    /**
     * Whether this effect is currently active.
     *
     * @return true if the effect is applied
     */
    boolean isActive();

    @Override
    default String getType() {
        return "room_effect";
    }
}
