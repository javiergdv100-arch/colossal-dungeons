package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDEItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(BuiltInRegistries.ITEM, ColossalDungeons.MOD_ID);

    // ========== Block Items ==========
    public static final DeferredHolder<Item, BlockItem> OIL_SURFACE_ITEM =
        ITEMS.register("oil_surface", () -> new BlockItem(CDEBlocks.OIL_SURFACE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> TRAP_SPIKE_PLATE_ITEM =
        ITEMS.register("trap_spike_plate", () -> new BlockItem(CDEBlocks.TRAP_SPIKE_PLATE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> RESONANCE_CRYSTAL_ITEM =
        ITEMS.register("resonance_crystal", () -> new BlockItem(CDEBlocks.RESONANCE_CRYSTAL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> PUSHABLE_BLOCK_ITEM =
        ITEMS.register("pushable_block", () -> new BlockItem(CDEBlocks.PUSHABLE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> HIDDEN_BUTTON_ITEM =
        ITEMS.register("hidden_button", () -> new BlockItem(CDEBlocks.HIDDEN_BUTTON.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> HYDRAULIC_ACTIVATOR_ITEM =
        ITEMS.register("hydraulic_activator", () -> new BlockItem(CDEBlocks.HYDRAULIC_ACTIVATOR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> MIRROR_PANEL_ITEM =
        ITEMS.register("mirror_panel", () -> new BlockItem(CDEBlocks.MIRROR_PANEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> BRAZIER_ACTIVABLE_ITEM =
        ITEMS.register("brazier_activable", () -> new BlockItem(CDEBlocks.BRAZIER_ACTIVABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WALL_MAW_BLOCK_ITEM =
        ITEMS.register("wall_maw_block", () -> new BlockItem(CDEBlocks.WALL_MAW_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> MEMBRANE_BLOCK_ITEM =
        ITEMS.register("membrane_block", () -> new BlockItem(CDEBlocks.MEMBRANE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ICE_PLATE_ITEM =
        ITEMS.register("ice_plate", () -> new BlockItem(CDEBlocks.ICE_PLATE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> GOLDEN_MECHANISM_ITEM =
        ITEMS.register("golden_mechanism", () -> new BlockItem(CDEBlocks.GOLDEN_MECHANISM.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> POWDER_SNOW_PLATFORM_ITEM =
        ITEMS.register("powder_snow_platform", () -> new BlockItem(CDEBlocks.POWDER_SNOW_PLATFORM.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> NERVE_FIBER_ITEM =
        ITEMS.register("nerve_fiber", () -> new BlockItem(CDEBlocks.NERVE_FIBER.get(), new Item.Properties()));

    // Weapons
    public static final DeferredHolder<Item, Item> COUNTERWEIGHT_HAMMER =
        ITEMS.register("counterweight_hammer", () -> new SwordItem(
            Tiers.NETHERITE, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 7, -3.0f))
        ));

    public static final DeferredHolder<Item, Item> SURGEON_BLADE =
        ITEMS.register("surgeon_blade", () -> new SwordItem(
            Tiers.DIAMOND, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -1.8f))
        ));

    public static final DeferredHolder<Item, Item> MADNESS_RAPIER =
        ITEMS.register("madness_rapier", () -> new SwordItem(
            Tiers.DIAMOND, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 5, -2.0f))
        ));

    public static final DeferredHolder<Item, Item> CYCLE_SCEPTER =
        ITEMS.register("cycle_scepter", () -> new SwordItem(
            Tiers.NETHERITE, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.6f))
        ));

    public static final DeferredHolder<Item, Item> NOON_LANCE =
        ITEMS.register("noon_lance", () -> new SwordItem(
            Tiers.NETHERITE, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8, -3.2f))
        ));

    public static final DeferredHolder<Item, Item> BLIZZARD_BOW =
        ITEMS.register("blizzard_bow", () -> new Item(new Item.Properties().durability(512)));

    // Utility / Tools
    public static final DeferredHolder<Item, Item> MIRROR_SHARD =
        ITEMS.register("mirror_shard", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> ESCAPE_CRYSTAL =
        ITEMS.register("escape_crystal", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> SANITY_CANDLE =
        ITEMS.register("sanity_candle", () -> new Item(new Item.Properties().durability(64)));

    public static final DeferredHolder<Item, Item> WINDCUTTER_CLOAK =
        ITEMS.register("windcutter_cloak", () -> new Item(new Item.Properties().stacksTo(1).durability(256)));

    // Relics / Crowns
    public static final DeferredHolder<Item, Item> ORACLE_RELIC =
        ITEMS.register("oracle_relic", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> FROST_CROWN =
        ITEMS.register("frost_crown", () -> new Item(new Item.Properties().stacksTo(1).durability(1024)));

    public static final DeferredHolder<Item, Item> TEMPEST_CROWN =
        ITEMS.register("tempest_crown", () -> new Item(new Item.Properties().stacksTo(1).durability(1024)));

    public static final DeferredHolder<Item, Item> SHATTER_CROWN =
        ITEMS.register("shatter_crown", () -> new Item(new Item.Properties().stacksTo(1).durability(1024)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
