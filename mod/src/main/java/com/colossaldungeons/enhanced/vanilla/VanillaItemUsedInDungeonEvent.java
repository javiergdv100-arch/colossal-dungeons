package com.colossaldungeons.enhanced.vanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Fired when a vanilla item is successfully used with a dungeon interaction.
 * Other systems can listen to this to react (e.g., puzzle engines, trap resets).
 */
public class VanillaItemUsedInDungeonEvent extends Event {

    private final ServerPlayer player;
    private final ItemStack item;
    private final BlockPos pos;
    private final InteractionResult result;

    public VanillaItemUsedInDungeonEvent(ServerPlayer player, ItemStack item, BlockPos pos, InteractionResult result) {
        this.player = player;
        this.item = item;
        this.pos = pos;
        this.result = result;
    }

    public ServerPlayer getPlayer() { return player; }
    public ItemStack getItem() { return item; }
    public BlockPos getPos() { return pos; }
    public InteractionResult getResult() { return result; }
}
