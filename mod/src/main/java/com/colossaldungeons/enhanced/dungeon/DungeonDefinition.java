package com.colossaldungeons.enhanced.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven definition of a dungeon loaded from JSON.
 * Contains all static configuration for a dungeon type.
 */
public record DungeonDefinition(
    ResourceLocation id,
    List<RoomDefinition> rooms,
    List<ConnectionDef> connections,
    DungeonRules rules,
    List<String> vanillaItems,
    Optional<ResourceLocation> bossId,
    ResetConfig resetConfig
) {
    public static final Codec<DungeonDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(DungeonDefinition::id),
            Codec.list(RoomDefinition.CODEC).fieldOf("rooms").forGetter(DungeonDefinition::rooms),
            Codec.list(ConnectionDef.CODEC).fieldOf("connections").forGetter(DungeonDefinition::connections),
            DungeonRules.CODEC.fieldOf("rules").forGetter(DungeonDefinition::rules),
            Codec.list(Codec.STRING).fieldOf("vanilla_items").forGetter(DungeonDefinition::vanillaItems),
            ResourceLocation.CODEC.optionalFieldOf("boss_id").forGetter(DungeonDefinition::bossId),
            ResetConfig.CODEC.fieldOf("reset_config").forGetter(DungeonDefinition::resetConfig)
        ).apply(instance, DungeonDefinition::new)
    );

    /**
     * Definition of a single room within a dungeon.
     */
    public record RoomDefinition(
        ResourceLocation id,
        String name,
        String type,
        int sizeX,
        int sizeY,
        int sizeZ
    ) {
        public static final Codec<RoomDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(RoomDefinition::id),
                Codec.STRING.fieldOf("name").forGetter(RoomDefinition::name),
                Codec.STRING.fieldOf("type").forGetter(RoomDefinition::type),
                Codec.INT.fieldOf("size_x").forGetter(RoomDefinition::sizeX),
                Codec.INT.fieldOf("size_y").forGetter(RoomDefinition::sizeY),
                Codec.INT.fieldOf("size_z").forGetter(RoomDefinition::sizeZ)
            ).apply(instance, RoomDefinition::new)
        );
    }

    /**
     * Defines a connection (doorway/passage) between two rooms.
     */
    public record ConnectionDef(
        ResourceLocation fromRoom,
        ResourceLocation toRoom,
        String type,
        boolean bidirectional
    ) {
        public static final Codec<ConnectionDef> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("from_room").forGetter(ConnectionDef::fromRoom),
                ResourceLocation.CODEC.fieldOf("to_room").forGetter(ConnectionDef::toRoom),
                Codec.STRING.fieldOf("type").forGetter(ConnectionDef::type),
                Codec.BOOL.fieldOf("bidirectional").forGetter(ConnectionDef::bidirectional)
            ).apply(instance, ConnectionDef::new)
        );
    }

    /**
     * Rules governing dungeon behavior.
     */
    public record DungeonRules(
        int maxPlayers,
        boolean allowRespawn,
        int timeLimitTicks,
        boolean pvpEnabled,
        boolean keepInventoryOnDeath
    ) {
        public static final Codec<DungeonRules> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("max_players").forGetter(DungeonRules::maxPlayers),
                Codec.BOOL.fieldOf("allow_respawn").forGetter(DungeonRules::allowRespawn),
                Codec.INT.fieldOf("time_limit_ticks").forGetter(DungeonRules::timeLimitTicks),
                Codec.BOOL.fieldOf("pvp_enabled").forGetter(DungeonRules::pvpEnabled),
                Codec.BOOL.fieldOf("keep_inventory_on_death").forGetter(DungeonRules::keepInventoryOnDeath)
            ).apply(instance, DungeonRules::new)
        );
    }

    /**
     * Configuration for how the dungeon resets after completion.
     */
    public record ResetConfig(
        int resetDelayTicks,
        boolean resetTraps,
        boolean resetMobs,
        boolean resetLoot,
        boolean resetPuzzles
    ) {
        public static final Codec<ResetConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("reset_delay_ticks").forGetter(ResetConfig::resetDelayTicks),
                Codec.BOOL.fieldOf("reset_traps").forGetter(ResetConfig::resetTraps),
                Codec.BOOL.fieldOf("reset_mobs").forGetter(ResetConfig::resetMobs),
                Codec.BOOL.fieldOf("reset_loot").forGetter(ResetConfig::resetLoot),
                Codec.BOOL.fieldOf("reset_puzzles").forGetter(ResetConfig::resetPuzzles)
            ).apply(instance, ResetConfig::new)
        );
    }
}
