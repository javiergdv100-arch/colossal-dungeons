package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDEBlocks {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(BuiltInRegistries.BLOCK, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<Block, Block> OIL_SURFACE =
        BLOCKS.register("oil_surface", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .noCollission().replaceable()
                .sound(SoundType.SLIME_BLOCK)
                .ignitedByLava()
        ));

    public static final DeferredHolder<Block, Block> TRAP_SPIKE_PLATE =
        BLOCKS.register("trap_spike_plate", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(50.0f, 1200.0f)
                .requiresCorrectToolForDrops().noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> RESONANCE_CRYSTAL =
        BLOCKS.register("resonance_crystal", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(20.0f, 300.0f)
                .lightLevel(state -> 9)
                .sound(SoundType.AMETHYST).noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> PUSHABLE_BLOCK =
        BLOCKS.register("pushable_block", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.STONE)
        ));

    public static final DeferredHolder<Block, Block> HIDDEN_BUTTON =
        BLOCKS.register("hidden_button", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5f, 6.0f)
                .noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> HYDRAULIC_ACTIVATOR =
        BLOCKS.register("hydraulic_activator", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(25.0f, 600.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.COPPER)
        ));

    public static final DeferredHolder<Block, Block> MIRROR_PANEL =
        BLOCKS.register("mirror_panel", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .strength(5.0f, 10.0f)
                .noOcclusion()
                .sound(SoundType.GLASS)
        ));

    public static final DeferredHolder<Block, Block> BRAZIER_ACTIVABLE =
        BLOCKS.register("brazier_activable", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(10.0f, 30.0f)
                .lightLevel(state -> 0)
                .sound(SoundType.LANTERN).noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> WALL_MAW_BLOCK =
        BLOCKS.register("wall_maw_block", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(50.0f, 1200.0f)
                .requiresCorrectToolForDrops().noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> MEMBRANE_BLOCK =
        BLOCKS.register("membrane_block", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(2.0f, 3.0f)
                .noOcclusion()
                .sound(SoundType.SLIME_BLOCK)
        ));

    public static final DeferredHolder<Block, Block> ICE_PLATE =
        BLOCKS.register("ice_plate", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .strength(3.0f, 5.0f)
                .friction(0.98f)
                .sound(SoundType.GLASS).noOcclusion()
        ));

    public static final DeferredHolder<Block, Block> GOLDEN_MECHANISM =
        BLOCKS.register("golden_mechanism", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(15.0f, 100.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.COPPER)
        ));

    public static final DeferredHolder<Block, Block> POWDER_SNOW_PLATFORM =
        BLOCKS.register("powder_snow_platform", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.SNOW)
                .strength(1.0f, 2.0f)
                .noOcclusion()
                .sound(SoundType.SNOW)
        ));

    public static final DeferredHolder<Block, Block> NERVE_FIBER =
        BLOCKS.register("nerve_fiber", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(5.0f, 10.0f)
                .noOcclusion()
                .sound(SoundType.SCULK)
        ));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
