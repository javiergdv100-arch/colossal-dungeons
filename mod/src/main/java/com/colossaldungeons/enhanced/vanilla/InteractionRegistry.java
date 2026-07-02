package com.colossaldungeons.enhanced.vanilla;

import com.colossaldungeons.enhanced.core.registry.CDEBlocks;
import com.colossaldungeons.enhanced.vanilla.interactions.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Static registry mapping item+block pairs to their dungeon interaction handlers.
 * Initialized during mod setup to register all concrete vanilla interactions.
 */
public class InteractionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionRegistry.class);

    /** Map from (Item, Block) pair to list of possible interactions for that combination. */
    private static final Map<InteractionKey, List<IVanillaInteraction>> INTERACTIONS = new HashMap<>();

    /** Interactions that can apply to any block (checked when no specific block match). */
    private static final Map<Item, List<IVanillaInteraction>> GLOBAL_ITEM_INTERACTIONS = new HashMap<>();

    private InteractionRegistry() {
        // Static utility class
    }

    /**
     * Initializes all vanilla interactions. Called during mod common setup.
     */
    public static void initialize() {
        INTERACTIONS.clear();
        GLOBAL_ITEM_INTERACTIONS.clear();

        // Bone Meal interactions
        register(Items.BONE_MEAL, CDEBlocks.OIL_SURFACE.get(), new BoneMealOilInteraction());

        // Water Bucket interactions
        register(Items.WATER_BUCKET, CDEBlocks.OIL_SURFACE.get(), new WaterBucketInteraction());

        // Honey Bottle interactions
        register(Items.HONEY_BOTTLE, CDEBlocks.ICE_PLATE.get(), new HoneyBottleInteraction());

        // Fishing Rod + Mirror interactions
        register(Items.FISHING_ROD, CDEBlocks.MIRROR_PANEL.get(), new FishingRodMirrorInteraction());

        // Powder Snow Bucket interactions
        registerGlobal(Items.POWDER_SNOW_BUCKET, new PowderSnowPlatformInteraction());

        // Milk Bucket - global (clears all mod effects)
        registerGlobal(Items.MILK_BUCKET, new MilkCleanseInteraction());

        // Torch - global reveal mechanic
        registerGlobal(Items.TORCH, new TorchRevealInteraction());

        // TNT interactions
        register(Items.TNT, CDEBlocks.RESONANCE_CRYSTAL.get(), new TNTExplosionInteraction());
        registerGlobal(Items.TNT, new TNTExplosionInteraction());

        LOGGER.info("Registered {} interaction pairs and {} global interactions",
            INTERACTIONS.size(), GLOBAL_ITEM_INTERACTIONS.size());
    }

    /**
     * Registers a specific item+block interaction.
     */
    public static void register(Item item, Block block, IVanillaInteraction interaction) {
        InteractionKey key = new InteractionKey(item, block);
        INTERACTIONS.computeIfAbsent(key, k -> new ArrayList<>()).add(interaction);
    }

    /**
     * Registers a global item interaction (applies regardless of target block).
     */
    public static void registerGlobal(Item item, IVanillaInteraction interaction) {
        GLOBAL_ITEM_INTERACTIONS.computeIfAbsent(item, k -> new ArrayList<>()).add(interaction);
    }

    /**
     * Finds the first applicable interaction for the given context.
     *
     * @param context the interaction context
     * @return an Optional containing the applicable interaction, or empty
     */
    public static Optional<IVanillaInteraction> findInteraction(InteractionContext context) {
        Item item = context.stack().getItem();
        Block block = context.blockState().getBlock();

        // Check specific item+block pair first
        InteractionKey key = new InteractionKey(item, block);
        List<IVanillaInteraction> specific = INTERACTIONS.get(key);
        if (specific != null) {
            for (IVanillaInteraction interaction : specific) {
                if (interaction.canApply(context)) {
                    return Optional.of(interaction);
                }
            }
        }

        // Check global item interactions
        List<IVanillaInteraction> global = GLOBAL_ITEM_INTERACTIONS.get(item);
        if (global != null) {
            for (IVanillaInteraction interaction : global) {
                if (interaction.canApply(context)) {
                    return Optional.of(interaction);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Key for the interaction map, combining Item and Block.
     */
    private record InteractionKey(Item item, Block block) {
    }
}
