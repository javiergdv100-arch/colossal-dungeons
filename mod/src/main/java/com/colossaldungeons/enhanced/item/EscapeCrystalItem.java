package com.colossaldungeons.enhanced.item;

import com.colossaldungeons.enhanced.dungeon.DungeonInstance;
import com.colossaldungeons.enhanced.dungeon.DungeonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Escape Crystal - single use item that teleports player to dungeon entrance.
 * 
 * Behavior:
 * - On use: teleports player to their current dungeon's entrance position
 * - Gets entrance pos from DungeonManager
 * - Consumes the item on successful use
 * - Fails gracefully if player is not in a dungeon
 */
public class EscapeCrystalItem extends Item {

    public EscapeCrystalItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.serverLevel();
            DungeonManager manager = DungeonManager.get(serverLevel);
            DungeonInstance dungeon = manager.getPlayerDungeon(serverPlayer);

            if (dungeon != null) {
                // Get the dungeon entrance position (first room's position as fallback)
                BlockPos entrancePos = getDungeonEntrance(dungeon, serverPlayer);

                if (entrancePos != null) {
                    // Teleport player
                    serverPlayer.teleportTo(
                        entrancePos.getX() + 0.5,
                        entrancePos.getY(),
                        entrancePos.getZ() + 0.5
                    );

                    // Effects
                    level.playSound(null, entrancePos, SoundEvents.CHORUS_FRUIT_TELEPORT,
                        SoundSource.PLAYERS, 1.0f, 1.0f);

                    // Remove player from dungeon tracking
                    manager.onPlayerLeaveDungeon(serverPlayer);

                    // Consume the item
                    if (!serverPlayer.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    return InteractionResultHolder.success(stack);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Gets the entrance position for the dungeon.
     * Falls back to the world spawn if no entrance data is available.
     */
    private BlockPos getDungeonEntrance(DungeonInstance dungeon, ServerPlayer player) {
        // Get the first room's center position as the entrance
        if (!dungeon.getRooms().isEmpty()) {
            return dungeon.getRooms().get(0).getBounds().getCenter();
        }

        // Fallback: world spawn
        return player.serverLevel().getSharedSpawnPos();
    }
}
