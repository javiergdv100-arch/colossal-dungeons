package com.colossaldungeons.enhanced.network;

import com.colossaldungeons.enhanced.ColossalDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles all network payload registration and message handling for CDE.
 * Uses record-based payloads with StreamCodec (NeoForge 1.21.1 pattern).
 */
public class CDENetworking {

    // ========== S2C (Server to Client) Payloads ==========

    /**
     * Sent when a trap activates - triggers client-side VFX/SFX.
     */
    public record TrapActivatePayload(BlockPos trapPos, String trapType, int intensity, float damageRadius)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<TrapActivatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "trap_activate"));

        public static final StreamCodec<FriendlyByteBuf, TrapActivatePayload> STREAM_CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, TrapActivatePayload::trapPos,
                ByteBufCodecs.STRING_UTF8, TrapActivatePayload::trapType,
                ByteBufCodecs.VAR_INT, TrapActivatePayload::intensity,
                ByteBufCodecs.FLOAT, TrapActivatePayload::damageRadius,
                TrapActivatePayload::new);

        @Override
        public CustomPacketPayload.Type<TrapActivatePayload> type() { return TYPE; }
    }

    /**
     * Sent when a player enters/exits a room - syncs room state to client.
     */
    public record RoomSyncPayload(ResourceLocation roomId, int stateOrdinal)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RoomSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "room_sync"));

        public static final StreamCodec<FriendlyByteBuf, RoomSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, RoomSyncPayload::roomId,
                ByteBufCodecs.VAR_INT, RoomSyncPayload::stateOrdinal,
                RoomSyncPayload::new);

        @Override
        public CustomPacketPayload.Type<RoomSyncPayload> type() { return TYPE; }
    }

    /**
     * Sent when a boss changes phase - triggers transition animation.
     */
    public record BossPhasePayload(int entityId, int phaseOrdinal)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<BossPhasePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "boss_phase"));

        public static final StreamCodec<FriendlyByteBuf, BossPhasePayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, BossPhasePayload::entityId,
                ByteBufCodecs.VAR_INT, BossPhasePayload::phaseOrdinal,
                BossPhasePayload::new);

        @Override
        public CustomPacketPayload.Type<BossPhasePayload> type() { return TYPE; }
    }

    /**
     * Sent to spawn visual effects at a position.
     */
    public record VFXSpawnPayload(BlockPos pos, String vfxType, float scale, int color)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<VFXSpawnPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "vfx_spawn"));

        public static final StreamCodec<FriendlyByteBuf, VFXSpawnPayload> STREAM_CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, VFXSpawnPayload::pos,
                ByteBufCodecs.STRING_UTF8, VFXSpawnPayload::vfxType,
                ByteBufCodecs.FLOAT, VFXSpawnPayload::scale,
                ByteBufCodecs.VAR_INT, VFXSpawnPayload::color,
                VFXSpawnPayload::new);

        @Override
        public CustomPacketPayload.Type<VFXSpawnPayload> type() { return TYPE; }
    }

    /**
     * Sent when a puzzle state changes - syncs puzzle progress to client.
     */
    public record PuzzleStatePayload(ResourceLocation puzzleId, int progress, int maxProgress, boolean completed)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PuzzleStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "puzzle_state"));

        public static final StreamCodec<FriendlyByteBuf, PuzzleStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, PuzzleStatePayload::puzzleId,
                ByteBufCodecs.VAR_INT, PuzzleStatePayload::progress,
                ByteBufCodecs.VAR_INT, PuzzleStatePayload::maxProgress,
                ByteBufCodecs.BOOL, PuzzleStatePayload::completed,
                PuzzleStatePayload::new);

        @Override
        public CustomPacketPayload.Type<PuzzleStatePayload> type() { return TYPE; }
    }

    /**
     * Sent when resonance charge level changes.
     */
    public record ResonanceUpdatePayload(int entityId, int chargeLevel, boolean discharging)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ResonanceUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "resonance_update"));

        public static final StreamCodec<FriendlyByteBuf, ResonanceUpdatePayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ResonanceUpdatePayload::entityId,
                ByteBufCodecs.VAR_INT, ResonanceUpdatePayload::chargeLevel,
                ByteBufCodecs.BOOL, ResonanceUpdatePayload::discharging,
                ResonanceUpdatePayload::new);

        @Override
        public CustomPacketPayload.Type<ResonanceUpdatePayload> type() { return TYPE; }
    }

    /**
     * Sent for general dungeon events (door open, loot spawn, etc.)
     */
    public record DungeonEventPayload(ResourceLocation eventType, BlockPos pos, String data)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DungeonEventPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "dungeon_event"));

        public static final StreamCodec<FriendlyByteBuf, DungeonEventPayload> STREAM_CODEC =
            StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, DungeonEventPayload::eventType,
                BlockPos.STREAM_CODEC, DungeonEventPayload::pos,
                ByteBufCodecs.STRING_UTF8, DungeonEventPayload::data,
                DungeonEventPayload::new);

        @Override
        public CustomPacketPayload.Type<DungeonEventPayload> type() { return TYPE; }
    }

    // ========== C2S (Client to Server) Payloads ==========

    /**
     * Sent when a player activates a mechanism (lever, button, etc.)
     */
    public record MechanismActivatePayload(BlockPos mechanismPos, String action)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<MechanismActivatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "mechanism_activate"));

        public static final StreamCodec<FriendlyByteBuf, MechanismActivatePayload> STREAM_CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, MechanismActivatePayload::mechanismPos,
                ByteBufCodecs.STRING_UTF8, MechanismActivatePayload::action,
                MechanismActivatePayload::new);

        @Override
        public CustomPacketPayload.Type<MechanismActivatePayload> type() { return TYPE; }
    }

    /**
     * Sent when a player makes a dialogue choice with an NPC.
     */
    public record DialogueChoicePayload(int npcEntityId, int choiceIndex, String dialogueId)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DialogueChoicePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "dialogue_choice"));

        public static final StreamCodec<FriendlyByteBuf, DialogueChoicePayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DialogueChoicePayload::npcEntityId,
                ByteBufCodecs.VAR_INT, DialogueChoicePayload::choiceIndex,
                ByteBufCodecs.STRING_UTF8, DialogueChoicePayload::dialogueId,
                DialogueChoicePayload::new);

        @Override
        public CustomPacketPayload.Type<DialogueChoicePayload> type() { return TYPE; }
    }

    /**
     * Sent when a player gives an order to their companion.
     */
    public record CompanionOrderPayload(int companionEntityId, String orderType, BlockPos targetPos)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<CompanionOrderPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "companion_order"));

        public static final StreamCodec<FriendlyByteBuf, CompanionOrderPayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CompanionOrderPayload::companionEntityId,
                ByteBufCodecs.STRING_UTF8, CompanionOrderPayload::orderType,
                BlockPos.STREAM_CODEC, CompanionOrderPayload::targetPos,
                CompanionOrderPayload::new);

        @Override
        public CustomPacketPayload.Type<CompanionOrderPayload> type() { return TYPE; }
    }

    /**
     * Sent when the player requests interaction with a puzzle element.
     */
    public record PuzzleInteractPayload(BlockPos puzzlePos, int interactionType)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PuzzleInteractPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColossalDungeons.MOD_ID, "puzzle_interact"));

        public static final StreamCodec<FriendlyByteBuf, PuzzleInteractPayload> STREAM_CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, PuzzleInteractPayload::puzzlePos,
                ByteBufCodecs.VAR_INT, PuzzleInteractPayload::interactionType,
                PuzzleInteractPayload::new);

        @Override
        public CustomPacketPayload.Type<PuzzleInteractPayload> type() { return TYPE; }
    }

    // ========== Registration ==========

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1.0.0");

        // S2C payloads
        reg.playToClient(TrapActivatePayload.TYPE, TrapActivatePayload.STREAM_CODEC, CDENetworking::handleTrapClient);
        reg.playToClient(RoomSyncPayload.TYPE, RoomSyncPayload.STREAM_CODEC, CDENetworking::handleRoomClient);
        reg.playToClient(BossPhasePayload.TYPE, BossPhasePayload.STREAM_CODEC, CDENetworking::handleBossClient);
        reg.playToClient(VFXSpawnPayload.TYPE, VFXSpawnPayload.STREAM_CODEC, CDENetworking::handleVFXClient);
        reg.playToClient(PuzzleStatePayload.TYPE, PuzzleStatePayload.STREAM_CODEC, CDENetworking::handlePuzzleStateClient);
        reg.playToClient(ResonanceUpdatePayload.TYPE, ResonanceUpdatePayload.STREAM_CODEC, CDENetworking::handleResonanceClient);
        reg.playToClient(DungeonEventPayload.TYPE, DungeonEventPayload.STREAM_CODEC, CDENetworking::handleDungeonEventClient);

        // C2S payloads
        reg.playToServer(MechanismActivatePayload.TYPE, MechanismActivatePayload.STREAM_CODEC, CDENetworking::handleMechServer);
        reg.playToServer(DialogueChoicePayload.TYPE, DialogueChoicePayload.STREAM_CODEC, CDENetworking::handleDialogueServer);
        reg.playToServer(CompanionOrderPayload.TYPE, CompanionOrderPayload.STREAM_CODEC, CDENetworking::handleOrderServer);
        reg.playToServer(PuzzleInteractPayload.TYPE, PuzzleInteractPayload.STREAM_CODEC, CDENetworking::handlePuzzleInteractServer);
    }

    // ========== Handlers (placeholder implementations) ==========

    private static void handleTrapClient(TrapActivatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: spawn VFX/SFX for trap activation
        });
    }

    private static void handleRoomClient(RoomSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: update room state display, HUD elements
        });
    }

    private static void handleBossClient(BossPhasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: trigger boss phase transition animation
        });
    }

    private static void handleVFXClient(VFXSpawnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: spawn visual effect at position
        });
    }

    private static void handlePuzzleStateClient(PuzzleStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: update puzzle progress UI
        });
    }

    private static void handleResonanceClient(ResonanceUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: update resonance visual indicators
        });
    }

    private static void handleDungeonEventClient(DungeonEventPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: handle general dungeon event (door animation, loot sparkle, etc.)
        });
    }

    private static void handleMechServer(MechanismActivatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Server-side: validate and process mechanism activation
        });
    }

    private static void handleDialogueServer(DialogueChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Server-side: process dialogue choice, advance conversation
        });
    }

    private static void handleOrderServer(CompanionOrderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Server-side: validate and execute companion order
        });
    }

    private static void handlePuzzleInteractServer(PuzzleInteractPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Server-side: validate and process puzzle interaction
        });
    }
}
