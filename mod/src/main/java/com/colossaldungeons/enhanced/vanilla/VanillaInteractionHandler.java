package com.colossaldungeons.enhanced.vanilla;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import com.colossaldungeons.enhanced.dungeon.DungeonInstance;
import com.colossaldungeons.enhanced.dungeon.DungeonManager;
import com.colossaldungeons.enhanced.dungeon.room.RoomInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Event handler for vanilla item interactions within dungeons.
 * Listens to PlayerInteractEvent.RightClickBlock and dispatches to the InteractionRegistry.
 * 
 * Follows section 7.7 pattern exactly:
 * 1. Check isClientSide (skip client)
 * 2. Cast to ServerPlayer
 * 3. Check isPlayerInDungeon
 * 4. Check cooldown
 * 5. Create InteractionContext
 * 6. Find interaction from registry
 * 7. Apply it
 * 8. Handle consume/cooldown/event firing
 */
@EventBusSubscriber(modid = ColossalDungeons.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class VanillaInteractionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaInteractionHandler.class);

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Step 1: Skip client side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Step 2: Cast to ServerPlayer
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) event.getLevel();

        // Step 3: Check if player is in a dungeon
        DungeonManager manager = DungeonManager.get(serverLevel);
        DungeonInstance dungeon = manager.getPlayerDungeon(serverPlayer);
        if (dungeon == null) {
            return; // Not in a dungeon, vanilla behavior applies
        }

        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) {
            return;
        }

        // Step 4: Check cooldown
        InteractionCooldowns cooldowns = serverPlayer.getData(CDEAttachments.VANILLA_INTERACTION_COOLDOWN.get());
        if (cooldowns.isOnCooldown(heldItem.getItem())) {
            return;
        }

        // Step 5: Create InteractionContext
        RoomInstance currentRoom = dungeon.getRoomForPlayer(serverPlayer);
        InteractionContext context = new InteractionContext(
            serverPlayer,
            serverLevel,
            event.getPos(),
            serverLevel.getBlockState(event.getPos()),
            heldItem,
            currentRoom
        );

        // Step 6: Find matching interaction
        Optional<IVanillaInteraction> interaction = InteractionRegistry.findInteraction(context);
        if (interaction.isEmpty()) {
            return;
        }

        // Step 7: Apply the interaction
        IVanillaInteraction handler = interaction.get();
        InteractionResult result = handler.apply(context);

        // Step 8: Handle consume, cooldown, event firing
        if (result == InteractionResult.SUCCESS || result == InteractionResult.PARTIAL) {
            // Consume item if required
            if (handler.consumesItem()) {
                heldItem.shrink(1);
            }

            // Set cooldown
            int cooldownTicks = handler.getCooldown();
            if (cooldownTicks > 0) {
                cooldowns.setCooldown(heldItem.getItem(), cooldownTicks);
            }

            // Fire event for other systems to react
            NeoForge.EVENT_BUS.post(new VanillaItemUsedInDungeonEvent(
                serverPlayer, heldItem, event.getPos(), result
            ));

            // Cancel the vanilla event to prevent normal block interaction
            event.setCanceled(true);
        }
    }
}
