package com.colossaldungeons.enhanced.data.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads vanilla interaction definitions from data packs.
 * Reads JSON files from: data/[namespace]/vanilla_interactions/[name].json
 * 
 * Interaction configs define how vanilla items interact with dungeon elements.
 */
public class VanillaInteractionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaInteractionLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<ResourceLocation, VanillaInteractionConfig> LOADED_INTERACTIONS = new HashMap<>();

    public VanillaInteractionLoader() {
        super(GSON, "vanilla_interactions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOADED_INTERACTIONS.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                VanillaInteractionConfig config = VanillaInteractionConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Error parsing interaction config {}: {}", id, error))
                    .orElse(null);

                if (config != null) {
                    LOADED_INTERACTIONS.put(id, config);
                    loaded++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load interaction config: {}", id, e);
            }
        }

        LOGGER.info("Loaded {} vanilla interaction configurations", loaded);
    }

    /**
     * Gets a loaded interaction config by its resource location.
     */
    public static VanillaInteractionConfig getConfig(ResourceLocation id) {
        return LOADED_INTERACTIONS.get(id);
    }

    /**
     * Gets all loaded interaction configurations.
     */
    public static Map<ResourceLocation, VanillaInteractionConfig> getAllConfigs() {
        return Map.copyOf(LOADED_INTERACTIONS);
    }

    /**
     * Checks if an interaction config is loaded.
     */
    public static boolean hasConfig(ResourceLocation id) {
        return LOADED_INTERACTIONS.containsKey(id);
    }

    /**
     * Gets the total number of loaded interactions.
     */
    public static int getLoadedCount() {
        return LOADED_INTERACTIONS.size();
    }
}
