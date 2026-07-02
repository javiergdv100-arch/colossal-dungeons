package com.colossaldungeons.enhanced.core.registry;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.dungeon.DungeonProgress;
import com.colossaldungeons.enhanced.dungeon.room.RoomStateData;
import com.colossaldungeons.enhanced.vanilla.InteractionCooldowns;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CDEAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ColossalDungeons.MOD_ID);

    // Player dungeon progress - persists on death
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DungeonProgress>> DUNGEON_PROGRESS =
        ATTACHMENTS.register("dungeon_progress",
            () -> AttachmentType.builder(DungeonProgress::new)
                .serialize(DungeonProgress.CODEC).copyOnDeath().build());

    // Room state data (attached to Level)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RoomStateData>> ROOM_STATE =
        ATTACHMENTS.register("room_state",
            () -> AttachmentType.builder(RoomStateData::new)
                .serialize(RoomStateData.CODEC).build());

    // Oil status level (0=clean, 1-3=oiled)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> OIL_STATUS =
        ATTACHMENTS.register("oil_status",
            () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT).build());

    // Madness level (0-100)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MADNESS_LEVEL =
        ATTACHMENTS.register("madness_level",
            () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT).copyOnDeath().build());

    // Vanilla interaction cooldowns
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InteractionCooldowns>> VANILLA_INTERACTION_COOLDOWN =
        ATTACHMENTS.register("vanilla_interaction_cooldown",
            () -> AttachmentType.builder(InteractionCooldowns::new)
                .serialize(InteractionCooldowns.CODEC).build());

    // Resonance charge level (0-10)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> RESONANCE_LEVEL =
        ATTACHMENTS.register("resonance_level",
            () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT).build());

    // Companion data (stored as string for now, full implementation later)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> COMPANION_DATA =
        ATTACHMENTS.register("companion_data",
            () -> AttachmentType.builder(() -> "")
                .serialize(Codec.STRING).copyOnDeath().build());

    // Player reputation (stored as integer sum)
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_REPUTATION =
        ATTACHMENTS.register("player_reputation",
            () -> AttachmentType.builder(() -> 0)
                .serialize(Codec.INT).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
