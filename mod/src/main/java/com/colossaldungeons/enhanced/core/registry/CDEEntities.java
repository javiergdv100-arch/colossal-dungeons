package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDEEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> GELATINOUS_CUBE =
        ENTITIES.register("gelatinous_cube", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.8f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":gelatinous_cube"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> MIMIC =
        ENTITIES.register("mimic", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.0f, 1.0f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":mimic"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> DYING_ATLAS =
        ENTITIES.register("dying_atlas", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.5f, 4.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":dying_atlas"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> GLASS_HARLEQUIN =
        ENTITIES.register("glass_harlequin", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.0f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":glass_harlequin"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> VALDRIS =
        ENTITIES.register("valdris", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":valdris"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
