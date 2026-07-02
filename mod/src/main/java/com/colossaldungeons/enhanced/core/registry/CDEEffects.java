package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
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
        EFFECTS.register("oiled", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0x1A1A1A));

    public static final DeferredHolder<MobEffect, MobEffect> OIL_PROTECTION =
        EFFECTS.register("oil_protection", () -> new CDEMobEffect(MobEffectCategory.BENEFICIAL, 0x4A90D9));

    public static final DeferredHolder<MobEffect, MobEffect> MADNESS =
        EFFECTS.register("madness", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0x8B008B));

    public static final DeferredHolder<MobEffect, MobEffect> RESONANCE_CHARGE =
        EFFECTS.register("resonance_charge", () -> new CDEMobEffect(MobEffectCategory.NEUTRAL, 0xA020F0));

    public static final DeferredHolder<MobEffect, MobEffect> HEMORRHAGE =
        EFFECTS.register("hemorrhage", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0xCC0000));

    public static final DeferredHolder<MobEffect, MobEffect> FROST_SLOW =
        EFFECTS.register("frost_slow", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0xADD8E6));

    public static final DeferredHolder<MobEffect, MobEffect> SOLAR_BURN =
        EFFECTS.register("solar_burn", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0xFFD700));

    public static final DeferredHolder<MobEffect, MobEffect> PARASITIC_POISON =
        EFFECTS.register("parasitic_poison", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0x228B22));

    public static final DeferredHolder<MobEffect, MobEffect> SANITY_DRAIN =
        EFFECTS.register("sanity_drain", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0x4B0082));

    public static final DeferredHolder<MobEffect, MobEffect> WEIGHT_ENCUMBRANCE =
        EFFECTS.register("weight_encumbrance", () -> new CDEMobEffect(MobEffectCategory.HARMFUL, 0x808080));

    public static final DeferredHolder<MobEffect, MobEffect> ELEMENTAL_AFFINITY =
        EFFECTS.register("elemental_affinity", () -> new CDEMobEffect(MobEffectCategory.BENEFICIAL, 0x00CED1));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    /**
     * Simple MobEffect implementation used for all CDE effects.
     * Full effect logic will be added in later features.
     */
    private static class CDEMobEffect extends MobEffect {
        protected CDEMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
