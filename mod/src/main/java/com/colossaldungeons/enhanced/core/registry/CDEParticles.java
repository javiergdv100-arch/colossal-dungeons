package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDEParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OIL_DRIP =
        PARTICLES.register("oil_drip", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MERCURY_SPLASH =
        PARTICLES.register("mercury_splash", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOLAR_FLARE =
        PARTICLES.register("solar_flare", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_SHARD =
        PARTICLES.register("frost_shard", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BONE_MEAL_CLEANSE_PARTICLE =
        PARTICLES.register("bone_meal_cleanse_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESONANCE_WAVE =
        PARTICLES.register("resonance_wave", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACID_BUBBLE =
        PARTICLES.register("acid_bubble", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPECTRAL_WISP =
        PARTICLES.register("spectral_wisp", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
