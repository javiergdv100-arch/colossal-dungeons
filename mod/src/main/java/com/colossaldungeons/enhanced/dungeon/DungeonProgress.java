package com.colossaldungeons.enhanced.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks a player's overall dungeon progress, including cleared rooms,
 * boss kills, and discovered secrets. Persists on death via attachment.
 */
public class DungeonProgress {

    public static final Codec<DungeonProgress> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.list(ResourceLocation.CODEC).fieldOf("cleared_rooms").forGetter(DungeonProgress::getClearedRooms),
            Codec.INT.fieldOf("total_boss_kills").forGetter(DungeonProgress::getTotalBossKills),
            Codec.INT.fieldOf("secrets_found").forGetter(DungeonProgress::getSecretsFound),
            Codec.INT.fieldOf("deaths_in_dungeon").forGetter(DungeonProgress::getDeathsInDungeon),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).fieldOf("reputation").forGetter(DungeonProgress::getReputation)
        ).apply(instance, DungeonProgress::new)
    );

    private final List<ResourceLocation> clearedRooms;
    private int totalBossKills;
    private int secretsFound;
    private int deathsInDungeon;
    private final Map<ResourceLocation, Integer> reputation;

    public DungeonProgress() {
        this(new ArrayList<>(), 0, 0, 0, new HashMap<>());
    }

    public DungeonProgress(List<ResourceLocation> clearedRooms, int totalBossKills,
                           int secretsFound, int deathsInDungeon,
                           Map<ResourceLocation, Integer> reputation) {
        this.clearedRooms = new ArrayList<>(clearedRooms);
        this.totalBossKills = totalBossKills;
        this.secretsFound = secretsFound;
        this.deathsInDungeon = deathsInDungeon;
        this.reputation = new HashMap<>(reputation);
    }

    public List<ResourceLocation> getClearedRooms() { return clearedRooms; }
    public int getTotalBossKills() { return totalBossKills; }
    public int getSecretsFound() { return secretsFound; }
    public int getDeathsInDungeon() { return deathsInDungeon; }
    public Map<ResourceLocation, Integer> getReputation() { return reputation; }

    public void addClearedRoom(ResourceLocation roomId) {
        if (!clearedRooms.contains(roomId)) {
            clearedRooms.add(roomId);
        }
    }

    public void incrementBossKills() { totalBossKills++; }
    public void incrementSecrets() { secretsFound++; }
    public void incrementDeaths() { deathsInDungeon++; }

    public void addReputation(ResourceLocation factionId, int amount) {
        reputation.merge(factionId, amount, Integer::sum);
    }

    public int getReputationFor(ResourceLocation factionId) {
        return reputation.getOrDefault(factionId, 0);
    }
}
