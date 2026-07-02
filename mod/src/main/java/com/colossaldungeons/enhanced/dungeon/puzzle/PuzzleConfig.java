package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Data-driven puzzle configuration loaded from JSON.
 * Defines the puzzle type, solution, constraints, rewards, and hints.
 */
public record PuzzleConfig(
    String type,
    List<String> solution,
    int maxAttempts,
    int timeLimitTicks,
    List<ResourceLocation> rewards,
    List<String> hints
) {
    public static final Codec<PuzzleConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("type").forGetter(PuzzleConfig::type),
            Codec.STRING.listOf().fieldOf("solution").forGetter(PuzzleConfig::solution),
            Codec.INT.optionalFieldOf("max_attempts", 3).forGetter(PuzzleConfig::maxAttempts),
            Codec.INT.optionalFieldOf("time_limit_ticks", 6000).forGetter(PuzzleConfig::timeLimitTicks),
            ResourceLocation.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(PuzzleConfig::rewards),
            Codec.STRING.listOf().optionalFieldOf("hints", List.of()).forGetter(PuzzleConfig::hints)
        ).apply(instance, PuzzleConfig::new)
    );
}
