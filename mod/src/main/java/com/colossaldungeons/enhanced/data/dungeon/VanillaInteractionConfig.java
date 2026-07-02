package com.colossaldungeons.enhanced.data.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Data-driven vanilla interaction configuration loaded from JSON.
 * Defines how a vanilla item interacts with a dungeon element.
 *
 * @param item the vanilla item identifier (e.g. "minecraft:bone_meal")
 * @param targetBlock the target block or "any" for global
 * @param effect the effect type applied
 * @param description human-readable description
 * @param consumesItem whether the item is consumed
 * @param cooldownTicks cooldown in ticks
 * @param particleEffect particle effect to spawn
 * @param soundEvent sound to play on interaction
 * @param conditions list of condition strings that must be met
 */
public record VanillaInteractionConfig(
    String item,
    String targetBlock,
    String effect,
    String description,
    boolean consumesItem,
    int cooldownTicks,
    String particleEffect,
    String soundEvent,
    List<String> conditions
) {
    public static final Codec<VanillaInteractionConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("item").forGetter(VanillaInteractionConfig::item),
            Codec.STRING.fieldOf("target_block").forGetter(VanillaInteractionConfig::targetBlock),
            Codec.STRING.fieldOf("effect").forGetter(VanillaInteractionConfig::effect),
            Codec.STRING.optionalFieldOf("description", "").forGetter(VanillaInteractionConfig::description),
            Codec.BOOL.optionalFieldOf("consumes_item", true).forGetter(VanillaInteractionConfig::consumesItem),
            Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(VanillaInteractionConfig::cooldownTicks),
            Codec.STRING.optionalFieldOf("particle_effect", "").forGetter(VanillaInteractionConfig::particleEffect),
            Codec.STRING.optionalFieldOf("sound_event", "").forGetter(VanillaInteractionConfig::soundEvent),
            Codec.STRING.listOf().optionalFieldOf("conditions", List.of()).forGetter(VanillaInteractionConfig::conditions)
        ).apply(instance, VanillaInteractionConfig::new)
    );
}
