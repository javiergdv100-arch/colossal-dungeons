package com.colossaldungeons.enhanced.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Base interface for all registrable dungeon content.
 * All content that can be registered through the addon API must implement this.
 */
public interface IDungeonContent {

    /**
     * Gets the unique identifier for this content.
     *
     * @return the resource location ID
     */
    ResourceLocation getId();

    /**
     * Gets the type category of this content.
     * Common values: "trap", "puzzle", "mechanism", "entity", "interaction"
     *
     * @return a string identifying the content type
     */
    String getType();
}
