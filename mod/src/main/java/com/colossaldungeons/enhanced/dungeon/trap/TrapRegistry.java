package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Registry for trap types. Maps ResourceLocation identifiers to trap factory functions.
 * Addons can register their own trap types here.
 */
public class TrapRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrapRegistry.class);

    private static final Map<ResourceLocation, BiFunction<BlockPos, TrapConfig, AbstractTrap>> REGISTRY = new HashMap<>();

    /**
     * Initializes the registry with built-in trap types.
     */
    public static void initialize() {
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "spike_plate"),
            SpikePlateTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "oil_surface"),
            OilSurfaceTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "oil_lamp"),
            OilLampTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "crushing_wall"),
            CrushingWallTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "flame_pillar"),
            FlamePillarTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "collapse"),
            CollapseTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "skeever_cage"),
            SkeeverCageTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "rolling_boulder"),
            RollingBoulderTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "infinite_corridor"),
            InfiniteCorridorTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "swinging_blade"),
            SwingingBladeTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "wall_blades"),
            WallBladesTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "tension_chain"),
            TensionChainTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "deadweight_chest"),
            DeadweightChestTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "devouring_door"),
            DevouringDoorTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "living"),
            LivingTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "mirror_room"),
            MirrorRoomTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "hostile_reflection"),
            HostileReflectionTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "blizzard"),
            BlizzardTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "ice_slide"),
            IceSlideTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "wind_gust"),
            WindGustTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "disappearing_floor"),
            DisappearingFloorTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "false_voice"),
            FalseVoiceTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "elemental_column"),
            ElementalColumnTrap::new);
        register(ResourceLocation.fromNamespaceAndPath("colossal_dungeons_enhanced", "solar_ray"),
            SolarRayTrap::new);

        LOGGER.info("TrapRegistry initialized with {} trap types", REGISTRY.size());
    }

    /**
     * Registers a trap factory under the given ID.
     *
     * @param id The unique identifier for this trap type
     * @param factory A function that creates a trap given position and config
     */
    public static void register(ResourceLocation id, BiFunction<BlockPos, TrapConfig, AbstractTrap> factory) {
        if (REGISTRY.containsKey(id)) {
            LOGGER.warn("Overwriting existing trap registration: {}", id);
        }
        REGISTRY.put(id, factory);
    }

    /**
     * Creates a trap instance for the given type at the specified position.
     *
     * @param id The trap type identifier
     * @param pos The world position for the trap
     * @param config The trap configuration
     * @return A new trap instance, or null if the type is not registered
     */
    public static AbstractTrap create(ResourceLocation id, BlockPos pos, TrapConfig config) {
        BiFunction<BlockPos, TrapConfig, AbstractTrap> factory = REGISTRY.get(id);
        if (factory == null) {
            LOGGER.error("Unknown trap type: {}", id);
            return null;
        }
        return factory.apply(pos, config);
    }

    /**
     * Checks if a trap type is registered.
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * Returns the number of registered trap types.
     */
    public static int getRegisteredCount() {
        return REGISTRY.size();
    }
}
