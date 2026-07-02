package com.colossaldungeons.enhanced.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Record describing the content provided by an addon mod.
 * Used for discovery, display, and dependency tracking.
 *
 * @param modId the mod ID of the addon
 * @param displayName human-readable name of the addon
 * @param version the version string of the addon
 * @param traps list of trap IDs registered by this addon
 * @param puzzles list of puzzle IDs registered by this addon
 * @param mechanisms list of mechanism IDs registered by this addon
 * @param entities list of entity IDs registered by this addon
 * @param interactions list of interaction IDs registered by this addon
 */
public record AddonContentPack(
    String modId,
    String displayName,
    String version,
    List<ResourceLocation> traps,
    List<ResourceLocation> puzzles,
    List<ResourceLocation> mechanisms,
    List<ResourceLocation> entities,
    List<ResourceLocation> interactions
) {
    /**
     * Creates a minimal content pack with just identification info.
     */
    public static AddonContentPack empty(String modId, String displayName, String version) {
        return new AddonContentPack(modId, displayName, version,
            List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Gets the total number of content items registered by this addon.
     */
    public int totalContentCount() {
        return traps.size() + puzzles.size() + mechanisms.size()
            + entities.size() + interactions.size();
    }
}
