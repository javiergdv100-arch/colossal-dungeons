package com.colossaldungeons.enhanced.entity.npc;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a complete dialogue tree for an NPC.
 * Contains all dialogue nodes and handles navigation between them based on player choices.
 *
 * The tree holds a root node and a map of all nodes by ID for direct lookup.
 * Dialogue flow: player interacts with NPC -> root node displayed -> player makes choice
 * -> advance to next node -> repeat until terminal node or exit.
 */
public class DialogueTree {

    private final DialogueNode rootNode;
    private final Map<String, DialogueNode> allNodes;

    /**
     * Creates a new dialogue tree with the specified root node.
     *
     * @param rootNode the starting node of the dialogue
     */
    public DialogueTree(DialogueNode rootNode) {
        this.rootNode = rootNode;
        this.allNodes = new HashMap<>();
        this.allNodes.put(rootNode.id(), rootNode);
    }

    /**
     * Adds a node to the dialogue tree.
     *
     * @param node the node to add
     * @return this tree for chaining
     */
    public DialogueTree addNode(DialogueNode node) {
        this.allNodes.put(node.id(), node);
        return this;
    }

    /**
     * Gets a node by its ID.
     *
     * @param id the node ID
     * @return the node, or null if not found
     */
    public DialogueNode getNode(String id) {
        return allNodes.get(id);
    }

    /**
     * Gets the root (starting) node of the dialogue.
     *
     * @return the root node
     */
    public DialogueNode getRootNode() {
        return rootNode;
    }

    /**
     * Advances the dialogue based on the player's choice at the given node.
     * Validates the choice, checks conditions, executes any action, and returns the next node.
     *
     * @param player the player making the choice
     * @param currentNodeId the current node the player is viewing
     * @param choiceIndex the index of the choice made (0-based)
     * @return the next dialogue node, or null if the choice is invalid or dialogue ends
     */
    public DialogueNode advance(ServerPlayer player, String currentNodeId, int choiceIndex) {
        DialogueNode currentNode = allNodes.get(currentNodeId);
        if (currentNode == null) return null;

        if (choiceIndex < 0 || choiceIndex >= currentNode.choices().size()) {
            return null;
        }

        DialogueChoice choice = currentNode.choices().get(choiceIndex);

        // Check if the choice's condition is met
        if (!choice.isAvailable(player)) {
            return null;
        }

        // Execute the choice's action (if any)
        if (choice.action() != null) {
            choice.action().execute(player);
        }

        // Navigate to the next node
        String nextNodeId = choice.nextNodeId();
        if (nextNodeId == null || nextNodeId.isEmpty()) {
            return null; // End of dialogue
        }

        DialogueNode nextNode = allNodes.get(nextNodeId);
        if (nextNode == null) return null;

        // Check if the next node's display condition is met
        if (nextNode.displayCondition() != null && !nextNode.displayCondition().test(player)) {
            return null;
        }

        return nextNode;
    }

    /**
     * Gets the total number of nodes in this tree.
     *
     * @return the node count
     */
    public int getNodeCount() {
        return allNodes.size();
    }

    /**
     * Checks if a node with the given ID exists in this tree.
     *
     * @param id the node ID to check
     * @return true if the node exists
     */
    public boolean hasNode(String id) {
        return allNodes.containsKey(id);
    }
}
