package com.colossaldungeons.enhanced.dungeon;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists dungeon state data to the world save.
 * Stores which dungeons exist, their current state, and completion info.
 */
public class DungeonSaveData extends SavedData {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonSaveData.class);
    private static final String DATA_NAME = ColossalDungeons.MOD_ID + "_dungeons";

    private final Map<ResourceLocation, DungeonEntry> dungeons;

    public DungeonSaveData() {
        this.dungeons = new HashMap<>();
    }

    public DungeonSaveData(Map<ResourceLocation, DungeonEntry> dungeons) {
        this.dungeons = new HashMap<>(dungeons);
    }

    /**
     * Codec for individual dungeon save entries.
     */
    public record DungeonEntry(
        DungeonState state,
        int completionCount,
        long lastActiveTime
    ) {
        public static final Codec<DungeonEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                DungeonState.CODEC.fieldOf("state").forGetter(DungeonEntry::state),
                Codec.INT.fieldOf("completion_count").forGetter(DungeonEntry::completionCount),
                Codec.LONG.fieldOf("last_active_time").forGetter(DungeonEntry::lastActiveTime)
            ).apply(instance, DungeonEntry::new)
        );
    }

    private static final Codec<Map<ResourceLocation, DungeonEntry>> MAP_CODEC =
        Codec.unboundedMap(ResourceLocation.CODEC, DungeonEntry.CODEC);

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        MAP_CODEC.encodeStart(NbtOps.INSTANCE, dungeons)
            .resultOrPartial(error -> LOGGER.error("Failed to save dungeon data: {}", error))
            .ifPresent(encoded -> tag.put("dungeons", encoded));
        return tag;
    }

    /**
     * Creates DungeonSaveData from a saved CompoundTag.
     */
    public static DungeonSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("dungeons")) {
            Map<ResourceLocation, DungeonEntry> loaded = MAP_CODEC.parse(NbtOps.INSTANCE, tag.get("dungeons"))
                .resultOrPartial(error -> LOGGER.error("Failed to load dungeon data: {}", error))
                .orElse(new HashMap<>());
            return new DungeonSaveData(loaded);
        }
        return new DungeonSaveData();
    }

    /**
     * Gets or creates the DungeonSaveData for the given level.
     */
    public static DungeonSaveData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(DungeonSaveData::new, DungeonSaveData::load),
            DATA_NAME
        );
    }

    // --- Data Access ---

    public void setDungeonState(ResourceLocation id, DungeonState state, long gameTime) {
        DungeonEntry existing = dungeons.get(id);
        int completions = existing != null ? existing.completionCount() : 0;
        if (state == DungeonState.COMPLETED && existing != null) {
            completions = existing.completionCount() + 1;
        }
        dungeons.put(id, new DungeonEntry(state, completions, gameTime));
        setDirty();
    }

    public DungeonState getDungeonState(ResourceLocation id) {
        DungeonEntry entry = dungeons.get(id);
        return entry != null ? entry.state() : DungeonState.UNDISCOVERED;
    }

    public int getCompletionCount(ResourceLocation id) {
        DungeonEntry entry = dungeons.get(id);
        return entry != null ? entry.completionCount() : 0;
    }

    public Map<ResourceLocation, DungeonEntry> getAllDungeons() {
        return Map.copyOf(dungeons);
    }
}
