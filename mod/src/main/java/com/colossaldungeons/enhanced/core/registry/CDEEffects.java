package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.effect.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDEEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> OILED =
        EFFECTS.register("oiled", OiledEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> OIL_PROTECTION =
        EFFECTS.register("oil_protection", OilProtectionEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> MADNESS =
        EFFECTS.register("madness", MadnessEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> RESONANCE_CHARGE =
        EFFECTS.register("resonance_charge", ResonanceChargeEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> HEMORRHAGE =
        EFFECTS.register("hemorrhage", HemorrhageEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FROST_SLOW =
        EFFECTS.register("frost_slow", FrostSlowEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SOLAR_BURN =
        EFFECTS.register("solar_burn", SolarBurnEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> PARASITIC_POISON =
        EFFECTS.register("parasitic_poison", ParasiticPoisonEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> SANITY_DRAIN =
        EFFECTS.register("sanity_drain", SanityDrainEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> WEIGHT_ENCUMBRANCE =
        EFFECTS.register("weight_encumbrance", () -> new SimpleCDEEffect(MobEffectCategory.HARMFUL, 0x808080));

    public static final DeferredHolder<MobEffect, MobEffect> ELEMENTAL_AFFINITY =
        EFFECTS.register("elemental_affinity", () -> new SimpleCDEEffect(MobEffectCategory.BENEFICIAL, 0x00CED1));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    /**
     * Simple MobEffect implementation for effects without custom tick logic yet.
     */
    public static class SimpleCDEEffect extends MobEffect {
        public SimpleCDEEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
