package com.colossaldungeons.enhanced.data.dungeon;

import com.colossaldungeons.enhanced.dungeon.DungeonDefinition;
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
 * Loads dungeon definitions from data packs.
 * Reads JSON files from: data/[namespace]/dungeons/[name].json
 * 
 * Dungeon definitions describe the complete structure of a dungeon including
 * rooms, connections, rules, and reset behavior.
 */
public class DungeonConfigLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<ResourceLocation, DungeonDefinition> LOADED_DUNGEONS = new HashMap<>();

    public DungeonConfigLoader() {
        super(GSON, "dungeons");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOADED_DUNGEONS.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                DungeonDefinition definition = DungeonDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Error parsing dungeon config {}: {}", id, error))
                    .orElse(null);

                if (definition != null) {
                    LOADED_DUNGEONS.put(id, definition);
                    loaded++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load dungeon config: {}", id, e);
            }
        }

        LOGGER.info("Loaded {} dungeon definitions", loaded);
    }

    /**
     * Gets a loaded dungeon definition by its resource location.
     */
    public static DungeonDefinition getDefinition(ResourceLocation id) {
        return LOADED_DUNGEONS.get(id);
    }

    /**
     * Gets all loaded dungeon definitions.
     */
    public static Map<ResourceLocation, DungeonDefinition> getAllDefinitions() {
        return Map.copyOf(LOADED_DUNGEONS);
    }

    /**
     * Checks if a dungeon definition is loaded.
     */
    public static boolean hasDefinition(ResourceLocation id) {
        return LOADED_DUNGEONS.containsKey(id);
    }

    /**
     * Gets the total number of loaded dungeons.
     */
    public static int getLoadedCount() {
        return LOADED_DUNGEONS.size();
    }
}
