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

    // ===== Shared Dungeon Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> NEOTHELID_ADULT =
        ENTITIES.register("neothelid_adult", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.5f, 4.0f)
            .clientTrackingRange(16)
            .build(ColossalDungeons.MOD_ID + ":neothelid_adult"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> NEOTHELID_BABY =
        ENTITIES.register("neothelid_baby", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.4f, 0.3f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":neothelid_baby"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> STITCHED_WATCHER =
        ENTITIES.register("stitched_watcher", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.0f, 1.0f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":stitched_watcher"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> ADVENTURER_WRAITH =
        ENTITIES.register("adventurer_wraith", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":adventurer_wraith"));

    // ===== Mirror Castle Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> REFLECTION_TWIN =
        ENTITIES.register("reflection_twin", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":reflection_twin"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> GLASS_SERVANT =
        ENTITIES.register("glass_servant", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":glass_servant"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> QUICKSILVER_MAIDEN =
        ENTITIES.register("quicksilver_maiden", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.6f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":quicksilver_maiden"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FRAME_SENTINEL =
        ENTITIES.register("frame_sentinel", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.2f, 2.0f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":frame_sentinel"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SPECULAR_ECHO =
        ENTITIES.register("specular_echo", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":specular_echo"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> LANTERN_BEARER =
        ENTITIES.register("lantern_bearer", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.6f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":lantern_bearer"));

    // ===== Mirror Castle Bosses =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> BROKEN_TWINS =
        ENTITIES.register("broken_twins", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.2f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":broken_twins"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> LADY_QUICKSILVER =
        ENTITIES.register("lady_quicksilver", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":lady_quicksilver"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SHATTERED_SOVEREIGN =
        ENTITIES.register("shattered_sovereign", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.2f, 2.8f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":shattered_sovereign"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
