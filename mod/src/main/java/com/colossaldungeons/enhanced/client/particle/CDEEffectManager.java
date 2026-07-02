package com.colossaldungeons.enhanced.client.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Client-side effect manager for CDE visual effects.
 * Manages a pool of active effect instances with a maximum of 64 concurrent effects.
 * 
 * Provides methods for spawning effects at positions, on entity bones,
 * and for vanilla feedback effects (bone meal cleanse, water extinguish, etc.)
 */
@OnlyIn(Dist.CLIENT)
public class CDEEffectManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDEEffectManager.class);

    /** Maximum concurrent active effects */
    public static final int MAX_ACTIVE_EFFECTS = 64;

    /** Default effect lifetime in ticks */
    private static final int DEFAULT_LIFETIME = 60;

    /** Cache of effect resource locations by ID string */
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    /** Active effect instances */
    private static final List<CDEEffectInstance> ACTIVE = new ArrayList<>();

    private CDEEffectManager() {
        // Static utility class
    }

    /**
     * Spawns an effect at a world position.
     *
     * @param id the effect identifier
     * @param pos the world position
     * @param scale the effect scale
     */
    public static void spawnAtPosition(String id, Vec3 pos, float scale) {
        if (ACTIVE.size() >= MAX_ACTIVE_EFFECTS) {
            // Remove oldest effect to make room
            removeOldest();
        }

        CDEEffectInstance instance = new CDEEffectInstance(id, pos, scale, DEFAULT_LIFETIME);
        ACTIVE.add(instance);

        // Cache the resource location
        CACHE.computeIfAbsent(id, k -> ResourceLocation.fromNamespaceAndPath(
            "colossal_dungeons_enhanced", "effects/" + k));
    }

    /**
     * Spawns an effect attached to an entity bone.
     *
     * @param id the effect identifier
     * @param entity the entity to attach to
     * @param bone the bone name for attachment
     * @param scale the effect scale
     */
    public static void spawnOnBone(String id, Entity entity, String bone, float scale) {
        if (ACTIVE.size() >= MAX_ACTIVE_EFFECTS) {
            removeOldest();
        }

        Vec3 pos = entity.position();
        CDEEffectInstance instance = new CDEEffectInstance(id, pos, scale, DEFAULT_LIFETIME * 2);
        instance.attachToBone(entity, bone);
        ACTIVE.add(instance);
    }

    /**
     * Spawns a vanilla feedback effect at a block position.
     * Used for dungeon-specific vanilla item interactions.
     *
     * @param type the feedback type: "bone_meal_cleanse", "water_extinguish",
     *             "honey_coat", "powder_snow_platform"
     * @param pos the block position
     */
    public static void spawnVanillaFeedback(String type, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);

        switch (type) {
            case "bone_meal_cleanse" -> {
                spawnAtPosition("vanilla_cleanse", center, 1.0f);
            }
            case "water_extinguish" -> {
                spawnAtPosition("vanilla_extinguish", center, 1.2f);
            }
            case "honey_coat" -> {
                spawnAtPosition("vanilla_honey", center, 0.8f);
            }
            case "powder_snow_platform" -> {
                spawnAtPosition("vanilla_snow", center, 1.5f);
            }
            default -> LOGGER.warn("Unknown vanilla feedback type: {}", type);
        }
    }

    /**
     * Spawns a trap-specific visual effect at a block position.
     *
     * @param pos the block position of the trap
     * @param trapType the trap type identifier
     * @param intensity the effect intensity (1-10)
     */
    public static void spawnTrapEffect(BlockPos pos, String trapType, int intensity) {
        Vec3 center = Vec3.atCenterOf(pos);
        float scale = 0.5f + (intensity * 0.15f);
        String effectId = "trap_" + trapType;

        spawnAtPosition(effectId, center, scale);
    }

    /**
     * Ticks all active effects, removing finished ones.
     * Called each client tick.
     */
    public static void tick() {
        Iterator<CDEEffectInstance> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            CDEEffectInstance instance = iterator.next();
            instance.tick();
            if (instance.isFinished()) {
                iterator.remove();
            }
        }
    }

    /**
     * Removes the oldest active effect to make room for new ones.
     */
    private static void removeOldest() {
        if (!ACTIVE.isEmpty()) {
            ACTIVE.get(0).stop();
            ACTIVE.remove(0);
        }
    }

    /**
     * Stops all active effects immediately.
     */
    public static void stopAll() {
        for (CDEEffectInstance instance : ACTIVE) {
            instance.stop();
        }
        ACTIVE.clear();
    }

    /**
     * Gets the number of currently active effects.
     */
    public static int getActiveCount() {
        return ACTIVE.size();
    }

    /**
     * Gets an unmodifiable view of all active effect instances.
     */
    public static List<CDEEffectInstance> getActiveEffects() {
        return Collections.unmodifiableList(ACTIVE);
    }
}
