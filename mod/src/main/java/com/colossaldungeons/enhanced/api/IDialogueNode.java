package com.colossaldungeons.enhanced.api;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Interface for dialogue nodes in the NPC dialogue system.
 * Each node represents a point in a conversation with choices the player can make.
 */
public interface IDialogueNode extends IDungeonContent {

    /**
     * Gets the unique identifier for this dialogue node.
     */
    @Override
    ResourceLocation getId();

    /**
     * Gets the text displayed to the player at this dialogue node.
     *
     * @return the dialogue text
     */
    String getText();

    /**
     * Gets the available choices the player can make at this node.
     * Each choice leads to another dialogue node or ends the conversation.
     *
     * @return list of dialogue choices
     */
    List<DialogueChoice> getChoices();

    /**
     * Whether this node ends the conversation (no further choices).
     *
     * @return true if this is a terminal node
     */
    default boolean isTerminal() {
        return getChoices().isEmpty();
    }

    @Override
    default String getType() {
        return "dialogue";
    }

    /**
     * Represents a single choice in a dialogue node.
     *
     * @param text the choice text displayed to the player
     * @param nextNodeId the ID of the next dialogue node (null if ends conversation)
     * @param condition optional condition that must be met for this choice to appear
     */
    record DialogueChoice(String text, ResourceLocation nextNodeId, String condition) {
        public DialogueChoice(String text, ResourceLocation nextNodeId) {
            this(text, nextNodeId, null);
        }
    }
}
