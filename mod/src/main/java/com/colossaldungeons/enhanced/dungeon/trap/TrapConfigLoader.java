package com.colossaldungeons.enhanced.dungeon.trap;

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
 * Loads trap configurations from data packs.
 * Reads JSON files from: data/[namespace]/traps/[name].json
 */
public class TrapConfigLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrapConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<ResourceLocation, TrapConfig> LOADED_CONFIGS = new HashMap<>();

    public TrapConfigLoader() {
        super(GSON, "traps");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOADED_CONFIGS.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                TrapConfig config = TrapConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Error parsing trap config {}: {}", id, error))
                    .orElse(null);

                if (config != null) {
                    LOADED_CONFIGS.put(id, config);
                    loaded++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load trap config: {}", id, e);
            }
        }

        LOGGER.info("Loaded {} trap configurations", loaded);
    }

    /**
     * Gets a loaded trap config by its resource location.
     */
    public static TrapConfig getConfig(ResourceLocation id) {
        return LOADED_CONFIGS.get(id);
    }

    /**
     * Gets all loaded trap configurations.
     */
    public static Map<ResourceLocation, TrapConfig> getAllConfigs() {
        return Map.copyOf(LOADED_CONFIGS);
    }

    /**
     * Checks if a trap config is loaded.
     */
    public static boolean hasConfig(ResourceLocation id) {
        return LOADED_CONFIGS.containsKey(id);
    }
}
