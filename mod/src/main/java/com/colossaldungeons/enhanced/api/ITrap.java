package com.colossaldungeons.enhanced.api;

import com.colossaldungeons.enhanced.dungeon.trap.AbstractTrap;
import com.colossaldungeons.enhanced.dungeon.trap.TrapConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Public API interface for trap content.
 * Addon mods implement this to define new trap types that can be registered via CDEApi.
 */
public interface ITrap extends IDungeonContent {

    /**
     * Gets the unique identifier for this trap type.
     */
    @Override
    ResourceLocation getId();

    /**
     * Gets the default configuration for this trap type.
     *
     * @return the default TrapConfig
     */
    TrapConfig getConfig();

    /**
     * Creates a new trap instance at the given position with the given config.
     *
     * @param pos the block position for the trap
     * @param config the trap configuration (may differ from default)
     * @return a new AbstractTrap instance
     */
    AbstractTrap create(BlockPos pos, TrapConfig config);

    @Override
    default String getType() {
        return "trap";
    }
}
