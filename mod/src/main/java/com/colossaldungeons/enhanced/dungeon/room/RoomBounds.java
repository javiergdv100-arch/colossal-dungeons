package com.colossaldungeons.enhanced.dungeon.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Defines the axis-aligned bounding box for a dungeon room.
 * Provides spatial queries for entity containment and intersection.
 */
public record RoomBounds(BlockPos min, BlockPos max) {

    public static final Codec<RoomBounds> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(RoomBounds::min),
            BlockPos.CODEC.fieldOf("max").forGetter(RoomBounds::max)
        ).apply(instance, RoomBounds::new)
    );

    /**
     * Checks if a block position is within this room's bounds (inclusive).
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Checks if an entity's position is within this room's bounds.
     */
    public boolean contains(Entity entity) {
        BlockPos entityPos = entity.blockPosition();
        return contains(entityPos);
    }

    /**
     * Returns the center position of this room.
     */
    public BlockPos getCenter() {
        return new BlockPos(
            (min.getX() + max.getX()) / 2,
            (min.getY() + max.getY()) / 2,
            (min.getZ() + max.getZ()) / 2
        );
    }

    /**
     * Returns a new RoomBounds inflated by the given amount in all directions.
     */
    public RoomBounds inflate(int amount) {
        return new RoomBounds(
            min.offset(-amount, -amount, -amount),
            max.offset(amount, amount, amount)
        );
    }

    /**
     * Converts this room bounds to a Minecraft AABB for entity queries.
     */
    public AABB toAABB() {
        return new AABB(
            min.getX(), min.getY(), min.getZ(),
            max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0
        );
    }

    /**
     * Returns the center as a Vec3 (useful for distance calculations).
     */
    public Vec3 getCenterVec() {
        return new Vec3(
            (min.getX() + max.getX() + 1.0) / 2.0,
            (min.getY() + max.getY() + 1.0) / 2.0,
            (min.getZ() + max.getZ() + 1.0) / 2.0
        );
    }
}
