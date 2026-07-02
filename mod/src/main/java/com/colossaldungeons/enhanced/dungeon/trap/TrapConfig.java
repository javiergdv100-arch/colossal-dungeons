package com.colossaldungeons.enhanced.dungeon.trap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven trap configuration loaded from JSON.
 * Contains all parameters defining a trap's behavior.
 */
public record TrapConfig(
    String type,
    ActivatorConfig activator,
    DamageConfig damage,
    TimingConfig timing,
    List<String> vanillaInteractions,
    List<String> chainReactions,
    Optional<String> vfxId,
    boolean repeatable,
    int cooldownTicks
) {
    public static final Codec<TrapConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("type").forGetter(TrapConfig::type),
            ActivatorConfig.CODEC.fieldOf("activator").forGetter(TrapConfig::activator),
            DamageConfig.CODEC.fieldOf("damage").forGetter(TrapConfig::damage),
            TimingConfig.CODEC.fieldOf("timing").forGetter(TrapConfig::timing),
            Codec.list(Codec.STRING).fieldOf("vanilla_interactions").forGetter(TrapConfig::vanillaInteractions),
            Codec.list(Codec.STRING).fieldOf("chain_reactions").forGetter(TrapConfig::chainReactions),
            Codec.STRING.optionalFieldOf("vfx_id").forGetter(TrapConfig::vfxId),
            Codec.BOOL.fieldOf("repeatable").forGetter(TrapConfig::repeatable),
            Codec.INT.fieldOf("cooldown_ticks").forGetter(TrapConfig::cooldownTicks)
        ).apply(instance, TrapConfig::new)
    );

    /**
     * Configuration for what triggers the trap.
     */
    public record ActivatorConfig(
        String type,
        double range,
        int delay
    ) {
        public static final Codec<ActivatorConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("type").forGetter(ActivatorConfig::type),
                Codec.DOUBLE.fieldOf("range").forGetter(ActivatorConfig::range),
                Codec.INT.fieldOf("delay").forGetter(ActivatorConfig::delay)
            ).apply(instance, ActivatorConfig::new)
        );
    }

    /**
     * Configuration for the damage dealt by the trap.
     */
    public record DamageConfig(
        float amount,
        String type,
        float radius,
        List<String> effects
    ) {
        public static final Codec<DamageConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.FLOAT.fieldOf("amount").forGetter(DamageConfig::amount),
                Codec.STRING.fieldOf("type").forGetter(DamageConfig::type),
                Codec.FLOAT.fieldOf("radius").forGetter(DamageConfig::radius),
                Codec.list(Codec.STRING).fieldOf("effects").forGetter(DamageConfig::effects)
            ).apply(instance, DamageConfig::new)
        );
    }

    /**
     * Timing configuration for trap state transitions.
     */
    public record TimingConfig(
        int warningTicks,
        int activeTicks,
        int retractTicks
    ) {
        public static final Codec<TimingConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("warning_ticks").forGetter(TimingConfig::warningTicks),
                Codec.INT.fieldOf("active_ticks").forGetter(TimingConfig::activeTicks),
                Codec.INT.fieldOf("retract_ticks").forGetter(TimingConfig::retractTicks)
            ).apply(instance, TimingConfig::new)
        );
    }
}
