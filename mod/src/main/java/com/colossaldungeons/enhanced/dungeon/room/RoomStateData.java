package com.colossaldungeons.enhanced.dungeon.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the persistent state for all rooms in a dungeon level.
 * Attached to the Level via AttachmentType.
 */
public class RoomStateData {

    public static final Codec<RoomStateData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, RoomEntry.CODEC).fieldOf("rooms").forGetter(RoomStateData::getRooms),
            Codec.INT.fieldOf("active_room_count").forGetter(RoomStateData::getActiveRoomCount)
        ).apply(instance, RoomStateData::new)
    );

    private final Map<ResourceLocation, RoomEntry> rooms;
    private int activeRoomCount;

    public RoomStateData() {
        this(new HashMap<>(), 0);
    }

    public RoomStateData(Map<ResourceLocation, RoomEntry> rooms, int activeRoomCount) {
        this.rooms = new HashMap<>(rooms);
        this.activeRoomCount = activeRoomCount;
    }

    public Map<ResourceLocation, RoomEntry> getRooms() { return rooms; }
    public int getActiveRoomCount() { return activeRoomCount; }

    public void setActiveRoomCount(int count) { this.activeRoomCount = count; }

    public void setRoomState(ResourceLocation roomId, RoomState state) {
        rooms.computeIfAbsent(roomId, k -> new RoomEntry(state, new ArrayList<>(), false));
        rooms.get(roomId).setState(state);
    }

    public RoomState getRoomState(ResourceLocation roomId) {
        RoomEntry entry = rooms.get(roomId);
        return entry != null ? entry.getState() : RoomState.UNLOADED;
    }

    /**
     * Represents the persistent state of a single room.
     */
    public static class RoomEntry {
        public static final Codec<RoomEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                RoomState.CODEC.fieldOf("state").forGetter(RoomEntry::getState),
                Codec.list(BlockPos.CODEC).fieldOf("activated_mechanisms").forGetter(RoomEntry::getActivatedMechanisms),
                Codec.BOOL.fieldOf("cleared").forGetter(RoomEntry::isCleared)
            ).apply(instance, RoomEntry::new)
        );

        private RoomState state;
        private final List<BlockPos> activatedMechanisms;
        private boolean cleared;

        public RoomEntry(RoomState state, List<BlockPos> activatedMechanisms, boolean cleared) {
            this.state = state;
            this.activatedMechanisms = new ArrayList<>(activatedMechanisms);
            this.cleared = cleared;
        }

        public RoomState getState() { return state; }
        public List<BlockPos> getActivatedMechanisms() { return activatedMechanisms; }
        public boolean isCleared() { return cleared; }

        public void setState(RoomState state) { this.state = state; }
        public void setCleared(boolean cleared) { this.cleared = cleared; }
        public void addActivatedMechanism(BlockPos pos) { activatedMechanisms.add(pos); }
    }

    /**
     * Room lifecycle states.
     */
    public enum RoomState {
        UNLOADED,
        DORMANT,
        PREPARED,
        ACTIVE,
        COMBAT,
        PUZZLE_ACTIVE,
        CLEARED,
        LOCKED;

        public static final Codec<RoomState> CODEC = Codec.STRING.xmap(
            RoomState::valueOf,
            RoomState::name
        );
    }
}
