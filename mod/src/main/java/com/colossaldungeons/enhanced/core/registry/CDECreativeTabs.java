package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDECreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CDE_BLOCKS_TAB =
        TABS.register("cde_blocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ColossalDungeons.MOD_ID + ".blocks"))
            .icon(() -> new ItemStack(CDEItems.RESONANCE_CRYSTAL_ITEM.get()))
            .displayItems((params, output) -> {
                output.accept(CDEItems.OIL_SURFACE_ITEM.get());
                output.accept(CDEItems.TRAP_SPIKE_PLATE_ITEM.get());
                output.accept(CDEItems.RESONANCE_CRYSTAL_ITEM.get());
                output.accept(CDEItems.PUSHABLE_BLOCK_ITEM.get());
                output.accept(CDEItems.HIDDEN_BUTTON_ITEM.get());
                output.accept(CDEItems.HYDRAULIC_ACTIVATOR_ITEM.get());
                output.accept(CDEItems.MIRROR_PANEL_ITEM.get());
                output.accept(CDEItems.BRAZIER_ACTIVABLE_ITEM.get());
                output.accept(CDEItems.WALL_MAW_BLOCK_ITEM.get());
                output.accept(CDEItems.MEMBRANE_BLOCK_ITEM.get());
                output.accept(CDEItems.ICE_PLATE_ITEM.get());
                output.accept(CDEItems.GOLDEN_MECHANISM_ITEM.get());
                output.accept(CDEItems.POWDER_SNOW_PLATFORM_ITEM.get());
                output.accept(CDEItems.NERVE_FIBER_ITEM.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CDE_ITEMS_TAB =
        TABS.register("cde_items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ColossalDungeons.MOD_ID + ".items"))
            .icon(() -> new ItemStack(CDEItems.ORACLE_RELIC.get()))
            .displayItems((params, output) -> {
                output.accept(CDEItems.MIRROR_SHARD.get());
                output.accept(CDEItems.ESCAPE_CRYSTAL.get());
                output.accept(CDEItems.SANITY_CANDLE.get());
                output.accept(CDEItems.WINDCUTTER_CLOAK.get());
                output.accept(CDEItems.ORACLE_RELIC.get());
                output.accept(CDEItems.FROST_CROWN.get());
                output.accept(CDEItems.TEMPEST_CROWN.get());
                output.accept(CDEItems.SHATTER_CROWN.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CDE_TOOLS_TAB =
        TABS.register("cde_tools", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ColossalDungeons.MOD_ID + ".tools"))
            .icon(() -> new ItemStack(CDEItems.COUNTERWEIGHT_HAMMER.get()))
            .displayItems((params, output) -> {
                output.accept(CDEItems.COUNTERWEIGHT_HAMMER.get());
                output.accept(CDEItems.SURGEON_BLADE.get());
                output.accept(CDEItems.MADNESS_RAPIER.get());
                output.accept(CDEItems.CYCLE_SCEPTER.get());
                output.accept(CDEItems.NOON_LANCE.get());
                output.accept(CDEItems.BLIZZARD_BOW.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
