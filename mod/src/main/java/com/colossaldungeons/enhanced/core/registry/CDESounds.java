package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CDESounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ColossalDungeons.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TRAP_ACTIVATE =
        SOUNDS.register("trap_activate", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "trap_activate")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ROOM_ENTER =
        SOUNDS.register("room_enter", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "room_enter")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_PHASE_CHANGE =
        SOUNDS.register("boss_phase_change", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "boss_phase_change")));

    public static final DeferredHolder<SoundEvent, SoundEvent> RESONANCE_CHARGE_UP =
        SOUNDS.register("resonance_charge_up", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "resonance_charge_up")));

    public static final DeferredHolder<SoundEvent, SoundEvent> OIL_IGNITE =
        SOUNDS.register("oil_ignite", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "oil_ignite")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_MEAL_CLEANSE =
        SOUNDS.register("bone_meal_cleanse", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "bone_meal_cleanse")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_SHATTER =
        SOUNDS.register("mirror_shatter", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "mirror_shatter")));

    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT_LOOP =
        SOUNDS.register("heartbeat_loop", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "heartbeat_loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PUZZLE_COMPLETE =
        SOUNDS.register("puzzle_complete", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "puzzle_complete")));

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
