package com.colossaldungeons.enhanced.vanilla;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tracks interaction cooldowns per item type for a player within dungeons.
 * Each item can be on cooldown for a set number of ticks.
 */
public class InteractionCooldowns {

    public static final Codec<InteractionCooldowns> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).fieldOf("cooldowns").forGetter(InteractionCooldowns::serializeCooldowns)
        ).apply(instance, InteractionCooldowns::fromSerialized)
    );

    private final Map<Item, Integer> cooldowns;

    public InteractionCooldowns() {
        this.cooldowns = new HashMap<>();
    }

    private InteractionCooldowns(Map<Item, Integer> cooldowns) {
        this.cooldowns = new HashMap<>(cooldowns);
    }

    public boolean isOnCooldown(Item item) {
        return cooldowns.getOrDefault(item, 0) > 0;
    }

    public void setCooldown(Item item, int ticks) {
        cooldowns.put(item, ticks);
    }

    public int getCooldown(Item item) {
        return cooldowns.getOrDefault(item, 0);
    }

    public void tick() {
        cooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    private Map<ResourceLocation, Integer> serializeCooldowns() {
        Map<ResourceLocation, Integer> serialized = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : cooldowns.entrySet()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(entry.getKey());
            if (key != null) {
                serialized.put(key, entry.getValue());
            }
        }
        return serialized;
    }

    private static InteractionCooldowns fromSerialized(Map<ResourceLocation, Integer> serialized) {
        Map<Item, Integer> cooldowns = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : serialized.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item != null) {
                cooldowns.put(item, entry.getValue());
            }
        }
        return new InteractionCooldowns(cooldowns);
    }
}
