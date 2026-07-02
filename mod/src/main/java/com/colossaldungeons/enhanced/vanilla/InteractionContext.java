package com.colossaldungeons.enhanced.vanilla;

import com.colossaldungeons.enhanced.dungeon.room.RoomInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Context for a vanilla item interaction within a dungeon.
 * Contains all relevant information about the interaction event.
 */
public record InteractionContext(
    Player player,
    Level level,
    BlockPos pos,
    BlockState blockState,
    ItemStack stack,
    RoomInstance currentRoom
) {
}
