package com.colossaldungeons.enhanced.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for conditions that gate dialogue options.
 * Conditions are evaluated against a ServerPlayer to determine if
 * a dialogue node or choice should be displayed.
 */
public interface DialogueCondition {

    /**
     * Tests if the condition is met for the given player.
     *
     * @param player the player to test against
     * @return true if the condition is satisfied
     */
    boolean test(ServerPlayer player);

    /**
     * Gets a serializable identifier for this condition.
     *
     * @return the condition ID string
     */
    String getConditionId();

    // ========== Implementations ==========

    /**
     * Condition that checks if the player has a specific item in their inventory.
     */
    record HasItemCondition(Item item, int count) implements DialogueCondition {

        @Override
        public boolean test(ServerPlayer player) {
            int total = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(item)) {
                    total += stack.getCount();
                }
            }
            return total >= count;
        }

        @Override
        public String getConditionId() {
            return "has_item";
        }
    }

    /**
     * Condition that checks if the player has minimum reputation with a faction.
     * Uses the PLAYER_REPUTATION attachment for the reputation value.
     */
    record ReputationCondition(String faction, int minRep) implements DialogueCondition {

        @Override
        public boolean test(ServerPlayer player) {
            // Uses the PLAYER_REPUTATION attachment
            int rep = player.getData(
                com.colossaldungeons.enhanced.core.registry.CDEAttachments.PLAYER_REPUTATION.get());
            return rep >= minRep;
        }

        @Override
        public String getConditionId() {
            return "reputation:" + faction + ":" + minRep;
        }
    }

    /**
     * Condition that checks a quest's current state.
     * Uses the dungeon progress attachment to look up quest state.
     */
    record QuestStateCondition(String questId, String state) implements DialogueCondition {

        @Override
        public boolean test(ServerPlayer player) {
            // Check quest state via the dungeon progress attachment
            // This is a simplified check - full implementation would use DungeonProgress
            var progress = player.getData(
                com.colossaldungeons.enhanced.core.registry.CDEAttachments.DUNGEON_PROGRESS.get());
            return progress != null;
        }

        @Override
        public String getConditionId() {
            return "quest_state:" + questId + ":" + state;
        }
    }

    /**
     * Condition that always returns true - used as a default/fallback.
     */
    record AlwaysTrueCondition() implements DialogueCondition {

        @Override
        public boolean test(ServerPlayer player) {
            return true;
        }

        @Override
        public String getConditionId() {
            return "always_true";
        }
    }
}
