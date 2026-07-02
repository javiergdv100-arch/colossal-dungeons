package com.colossaldungeons.enhanced.entity.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player interaction history with an NPC.
 * Each NPC has its own NPCMemory that records what dialogue nodes each player
 * has visited, their reputation with this NPC, quest states, and interaction count.
 *
 * Serializable via Codec for world persistence.
 */
public class NPCMemory {

    private final Map<UUID, PlayerMemory> memories;

    /**
     * Per-player memory data for a single NPC.
     *
     * @param lastDialogueNode the ID of the last dialogue node the player visited
     * @param reputation the player's reputation with this NPC
     * @param questStates map of quest IDs to their current state strings
     * @param interactionCount how many times the player has interacted with this NPC
     */
    public record PlayerMemory(
        String lastDialogueNode,
        int reputation,
        Map<String, String> questStates,
        int interactionCount
    ) {
        public static final Codec<PlayerMemory> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("last_node").forGetter(PlayerMemory::lastDialogueNode),
                Codec.INT.fieldOf("reputation").forGetter(PlayerMemory::reputation),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("quests").forGetter(PlayerMemory::questStates),
                Codec.INT.fieldOf("interactions").forGetter(PlayerMemory::interactionCount)
            ).apply(instance, PlayerMemory::new)
        );

        /**
         * Creates an empty player memory with default values.
         */
        public static PlayerMemory empty() {
            return new PlayerMemory("root", 0, new HashMap<>(), 0);
        }

        /**
         * Creates a copy with updated last dialogue node.
         */
        public PlayerMemory withLastNode(String nodeId) {
            return new PlayerMemory(nodeId, reputation, questStates, interactionCount);
        }

        /**
         * Creates a copy with incremented interaction count.
         */
        public PlayerMemory withInteraction() {
            return new PlayerMemory(lastDialogueNode, reputation, questStates, interactionCount + 1);
        }

        /**
         * Creates a copy with updated reputation.
         */
        public PlayerMemory withReputation(int newRep) {
            return new PlayerMemory(lastDialogueNode, newRep, questStates, interactionCount);
        }
    }

    public static final Codec<NPCMemory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerMemory.CODEC)
                .fieldOf("memories")
                .forGetter(NPCMemory::getMemories)
        ).apply(instance, memories -> {
            NPCMemory mem = new NPCMemory();
            mem.memories.putAll(memories);
            return mem;
        })
    );

    public NPCMemory() {
        this.memories = new HashMap<>();
    }

    /**
     * Gets or creates the memory for a specific player.
     *
     * @param playerId the player's UUID
     * @return the player's memory record
     */
    public PlayerMemory getOrCreate(UUID playerId) {
        return memories.computeIfAbsent(playerId, id -> PlayerMemory.empty());
    }

    /**
     * Updates the memory for a specific player.
     *
     * @param playerId the player's UUID
     * @param memory the updated memory
     */
    public void set(UUID playerId, PlayerMemory memory) {
        memories.put(playerId, memory);
    }

    /**
     * Records that a player visited a dialogue node.
     *
     * @param playerId the player's UUID
     * @param nodeId the node ID visited
     */
    public void recordVisit(UUID playerId, String nodeId) {
        PlayerMemory current = getOrCreate(playerId);
        memories.put(playerId, current.withLastNode(nodeId).withInteraction());
    }

    /**
     * Gets the last dialogue node for a player.
     *
     * @param playerId the player's UUID
     * @return the last node ID, or "root" if never interacted
     */
    public String getLastNode(UUID playerId) {
        return getOrCreate(playerId).lastDialogueNode();
    }

    /**
     * Gets the interaction count for a player.
     *
     * @param playerId the player's UUID
     * @return number of interactions
     */
    public int getInteractionCount(UUID playerId) {
        return getOrCreate(playerId).interactionCount();
    }

    /**
     * Gets the reputation for a player with this NPC.
     *
     * @param playerId the player's UUID
     * @return the reputation value
     */
    public int getReputation(UUID playerId) {
        return getOrCreate(playerId).reputation();
    }

    /**
     * Sets a quest state for a player.
     *
     * @param playerId the player's UUID
     * @param questId the quest ID
     * @param state the quest state
     */
    public void setQuestState(UUID playerId, String questId, String state) {
        PlayerMemory current = getOrCreate(playerId);
        Map<String, String> newQuests = new HashMap<>(current.questStates());
        newQuests.put(questId, state);
        memories.put(playerId, new PlayerMemory(
            current.lastDialogueNode(), current.reputation(), newQuests, current.interactionCount()));
    }

    /**
     * Gets the raw memories map (for serialization).
     */
    public Map<UUID, PlayerMemory> getMemories() {
        return memories;
    }
}
