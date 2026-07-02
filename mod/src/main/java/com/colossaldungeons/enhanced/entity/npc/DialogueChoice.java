package com.colossaldungeons.enhanced.entity.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Represents a single choice available to the player in a dialogue node.
 *
 * @param text the displayed text for this choice
 * @param nextNodeId the ID of the node to navigate to when this choice is selected
 * @param condition optional condition that must be met for this choice to be visible (null = always visible)
 * @param action optional action executed when this choice is selected (null = no action)
 */
public record DialogueChoice(
    String text,
    String nextNodeId,
    DialogueCondition condition,
    DialogueAction action
) {

    public static final Codec<DialogueChoice> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("text").forGetter(DialogueChoice::text),
            Codec.STRING.fieldOf("next_node_id").forGetter(DialogueChoice::nextNodeId),
            Codec.STRING.optionalFieldOf("condition", "").forGetter(c ->
                c.condition() != null ? c.condition().getConditionId() : ""),
            Codec.STRING.optionalFieldOf("action", "").forGetter(c ->
                c.action() != null ? c.action().getActionId() : "")
        ).apply(instance, (text, nextId, condId, actId) ->
            new DialogueChoice(text, nextId,
                condId.isEmpty() ? null : new DialogueCondition.AlwaysTrueCondition(),
                actId.isEmpty() ? null : null))
    );

    /**
     * Creates a simple choice with no condition or action.
     */
    public DialogueChoice(String text, String nextNodeId) {
        this(text, nextNodeId, null, null);
    }

    /**
     * Checks if this choice is available to the specified player.
     * If no condition is set, the choice is always available.
     *
     * @param player the player to check against
     * @return true if the choice should be displayed
     */
    public boolean isAvailable(net.minecraft.server.level.ServerPlayer player) {
        if (condition == null) return true;
        return condition.test(player);
    }
}
