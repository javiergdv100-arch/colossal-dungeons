package com.colossaldungeons.enhanced.data.dungeon;

import com.colossaldungeons.enhanced.dungeon.puzzle.PuzzleConfig;
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
 * Loads puzzle configurations from data packs.
 * Reads JSON files from: data/[namespace]/puzzles/[name].json
 * 
 * Puzzle configs define puzzle type, solution, constraints, rewards, and hints.
 */
public class PuzzleConfigLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PuzzleConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<ResourceLocation, PuzzleConfig> LOADED_PUZZLES = new HashMap<>();

    public PuzzleConfigLoader() {
        super(GSON, "puzzles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOADED_PUZZLES.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                PuzzleConfig config = PuzzleConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Error parsing puzzle config {}: {}", id, error))
                    .orElse(null);

                if (config != null) {
                    LOADED_PUZZLES.put(id, config);
                    loaded++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load puzzle config: {}", id, e);
            }
        }

        LOGGER.info("Loaded {} puzzle configurations", loaded);
    }

    /**
     * Gets a loaded puzzle config by its resource location.
     */
    public static PuzzleConfig getConfig(ResourceLocation id) {
        return LOADED_PUZZLES.get(id);
    }

    /**
     * Gets all loaded puzzle configurations.
     */
    public static Map<ResourceLocation, PuzzleConfig> getAllConfigs() {
        return Map.copyOf(LOADED_PUZZLES);
    }

    /**
     * Checks if a puzzle config is loaded.
     */
    public static boolean hasConfig(ResourceLocation id) {
        return LOADED_PUZZLES.containsKey(id);
    }

    /**
     * Gets the total number of loaded puzzle configs.
     */
    public static int getLoadedCount() {
        return LOADED_PUZZLES.size();
    }
}
