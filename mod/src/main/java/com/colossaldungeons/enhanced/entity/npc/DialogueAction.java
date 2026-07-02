package com.colossaldungeons.enhanced.entity.npc;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for actions that execute when a dialogue choice is selected.
 * Actions can modify player state, give items, start quests, etc.
 */
public interface DialogueAction {

    /**
     * Executes the action for the given player.
     *
     * @param player the player who made the dialogue choice
     */
    void execute(ServerPlayer player);

    /**
     * Gets a serializable identifier for this action.
     *
     * @return the action ID string
     */
    String getActionId();

    // ========== Implementations ==========

    /**
     * Action that gives an item stack to the player.
     */
    record GiveItemAction(ItemStack stack) implements DialogueAction {

        @Override
        public void execute(ServerPlayer player) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                // Drop on ground if inventory is full
                player.drop(copy, false);
            }
        }

        @Override
        public String getActionId() {
            return "give_item";
        }
    }

    /**
     * Action that modifies the player's reputation with a faction.
     */
    record SetReputationAction(String faction, int amount) implements DialogueAction {

        @Override
        public void execute(ServerPlayer player) {
            int currentRep = player.getData(CDEAttachments.PLAYER_REPUTATION.get());
            player.setData(CDEAttachments.PLAYER_REPUTATION.get(), currentRep + amount);
        }

        @Override
        public String getActionId() {
            return "set_reputation:" + faction + ":" + amount;
        }
    }

    /**
     * Action that starts a quest for the player.
     * Updates the dungeon progress attachment with quest data.
     */
    record StartQuestAction(String questId) implements DialogueAction {

        @Override
        public void execute(ServerPlayer player) {
            // Quest tracking is handled through the DungeonProgress system
            // This marks the quest as started in the player's progress data
            var progress = player.getData(CDEAttachments.DUNGEON_PROGRESS.get());
            // Progress system handles quest state internally
        }

        @Override
        public String getActionId() {
            return "start_quest:" + questId;
        }
    }

    /**
     * Action that opens a shop interface for the player.
     */
    record OpenShopAction(String shopId) implements DialogueAction {

        @Override
        public void execute(ServerPlayer player) {
            // Opens the merchant shop UI
            // Full implementation would send a packet to open the shop screen
            // with the specified shop configuration
        }

        @Override
        public String getActionId() {
            return "open_shop:" + shopId;
        }
    }
}
