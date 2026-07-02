package com.colossaldungeons.enhanced.dungeon.mechanism;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Interface for dungeon mechanisms - interactive blocks that can be activated
 * by players or other systems to trigger events within the dungeon.
 */
public interface IMechanism {

    /**
     * Gets the unique identifier for this mechanism.
     */
    ResourceLocation getId();

    /**
     * Activates the mechanism.
     *
     * @param player the player activating the mechanism (may be null for automated activation)
     * @param level the level
     */
    void activate(ServerPlayer player, Level level);

    /**
     * Deactivates the mechanism.
     */
    void deactivate();

    /**
     * Gets the current state of this mechanism.
     */
    MechanismState getState();

    /**
     * Checks if a player can activate this mechanism.
     *
     * @param player the player attempting activation
     * @return true if activation is allowed
     */
    boolean canActivate(ServerPlayer player);

    /**
     * Ticks the mechanism each server tick.
     */
    void tick(ServerLevel level);
}
