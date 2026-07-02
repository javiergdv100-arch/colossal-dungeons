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

    // ===== Worldbearer Dungeon Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> WALKING_FRAGMENT =
        ENTITIES.register("walking_fragment", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.2f, 1.4f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":walking_fragment"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> BALLAST_PILGRIM =
        ENTITIES.register("ballast_pilgrim", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.0f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":ballast_pilgrim"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> COUNTERWEIGHT_GUARDIAN =
        ENTITIES.register("counterweight_guardian", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.4f, 2.8f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":counterweight_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> LITHIC_SWARM =
        ENTITIES.register("lithic_swarm", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.0f, 1.0f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":lithic_swarm"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> LESSER_ATLAS =
        ENTITIES.register("lesser_atlas", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.0f, 3.5f)
            .clientTrackingRange(14)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":lesser_atlas"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FOUNDATION_WYRM =
        ENTITIES.register("foundation_wyrm", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.0f, 1.5f)
            .clientTrackingRange(14)
            .build(ColossalDungeons.MOD_ID + ":foundation_wyrm"));

    // ===== Worldbearer Bosses =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> LIVING_VERTEBRA =
        ENTITIES.register("living_vertebra", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.5f, 2.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":living_vertebra"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> CORE_CUSTODIAN =
        ENTITIES.register("core_custodian", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.8f, 3.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":core_custodian"));

    // ===== Hollow Leviathan Dungeon Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> CLINGING_PARASITE =
        ENTITIES.register("clinging_parasite", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 0.4f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":clinging_parasite"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> ANTIBODY =
        ENTITIES.register("antibody", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 0.6f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":antibody"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> DEVOURING_LARVA =
        ENTITIES.register("devouring_larva", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 0.4f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":devouring_larva"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> WALL_MAW =
        ENTITIES.register("wall_maw", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.2f, 1.2f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":wall_maw"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SPITTING_POLYP =
        ENTITIES.register("spitting_polyp", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 0.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":spitting_polyp"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> MEMBRANE_WEAVER =
        ENTITIES.register("membrane_weaver", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.4f, 1.8f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":membrane_weaver"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FALSE_HEART =
        ENTITIES.register("false_heart", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.0f, 2.0f)
            .clientTrackingRange(14)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":false_heart"));

    // ===== Hollow Leviathan Bosses =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> NERVE_CLUSTER =
        ENTITIES.register("nerve_cluster", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.0f, 2.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":nerve_cluster"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SOVEREIGN_PARASITE =
        ENTITIES.register("sovereign_parasite", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(3.0f, 4.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":sovereign_parasite"));

    // ===== Veiled Peak Dungeon Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> VEILED_SOUL =
        ENTITIES.register("veiled_soul", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":veiled_soul"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> PETRIFIED_CLIMBER =
        ENTITIES.register("petrified_climber", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":petrified_climber"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> ZEPHYR_WISP =
        ENTITIES.register("zephyr_wisp", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.5f, 0.5f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":zephyr_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FOG_BELLRINGER =
        ENTITIES.register("fog_bellringer", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 2.0f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":fog_bellringer"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SPECTRAL_YAK =
        ENTITIES.register("spectral_yak", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.4f, 1.6f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":spectral_yak"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> VEIL_WARDEN =
        ENTITIES.register("veil_warden", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.4f)
            .clientTrackingRange(14)
            .build(ColossalDungeons.MOD_ID + ":veil_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FROZEN_ORACLE =
        ENTITIES.register("frozen_oracle", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.7f, 2.2f)
            .clientTrackingRange(14)
            .build(ColossalDungeons.MOD_ID + ":frozen_oracle"));

    // ===== Veiled Peak Bosses =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SUMMIT_KEEPER =
        ENTITIES.register("summit_keeper", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.0f, 3.5f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":summit_keeper"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> CROWNED_TEMPEST =
        ENTITIES.register("crowned_tempest", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(2.5f, 4.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":crowned_tempest"));

    // ===== Descent into Madness Dungeon Entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> THE_WHISPERER =
        ENTITIES.register("the_whisperer", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":the_whisperer"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> FACELESS_PILGRIM =
        ENTITIES.register("faceless_pilgrim", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":faceless_pilgrim"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> THE_CROWD =
        ENTITIES.register("the_crowd", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 0.4f)
            .clientTrackingRange(8)
            .build(ColossalDungeons.MOD_ID + ":the_crowd"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> ROTTEN_MEMORY =
        ENTITIES.register("rotten_memory", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .build(ColossalDungeons.MOD_ID + ":rotten_memory"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> HOLLOW_FENCER =
        ENTITIES.register("hollow_fencer", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.6f, 1.9f)
            .clientTrackingRange(12)
            .build(ColossalDungeons.MOD_ID + ":hollow_fencer"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> CHOIR_OF_CRIES =
        ENTITIES.register("choir_of_cries", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.2f, 1.4f)
            .clientTrackingRange(14)
            .build(ColossalDungeons.MOD_ID + ":choir_of_cries"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> THE_ONE_WHO_REPEATS =
        ENTITIES.register("the_one_who_repeats", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.2f)
            .clientTrackingRange(14)
            .build(ColossalDungeons.MOD_ID + ":the_one_who_repeats"));

    // ===== Descent into Madness Bosses =====

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> SEMBLANCE =
        ENTITIES.register("semblance", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(0.8f, 2.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":semblance"));

    public static final DeferredHolder<EntityType<?>, EntityType<PathfinderMob>> BROKEN_SANITY =
        ENTITIES.register("broken_sanity", () -> EntityType.Builder.of(PathfinderMob::new, MobCategory.MONSTER)
            .sized(1.5f, 3.0f)
            .clientTrackingRange(16)
            .fireImmune()
            .build(ColossalDungeons.MOD_ID + ":broken_sanity"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
