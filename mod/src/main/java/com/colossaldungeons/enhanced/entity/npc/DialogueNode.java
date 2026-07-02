package com.colossaldungeons.enhanced.entity.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Represents a single node in a dialogue tree.
 * Contains the text spoken by the NPC and a list of player choices.
 *
 * @param id the unique identifier for this node within its dialogue tree
 * @param speakerName the display name of the speaker (NPC name)
 * @param text the dialogue text displayed to the player
 * @param choices available player responses/choices
 * @param displayCondition optional condition controlling whether this node displays (null = always)
 */
public record DialogueNode(
    String id,
    String speakerName,
    String text,
    List<DialogueChoice> choices,
    DialogueCondition displayCondition
) {

    public static final Codec<DialogueNode> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("id").forGetter(DialogueNode::id),
            Codec.STRING.fieldOf("speaker_name").forGetter(DialogueNode::speakerName),
            Codec.STRING.fieldOf("text").forGetter(DialogueNode::text),
            DialogueChoice.CODEC.listOf().fieldOf("choices").forGetter(DialogueNode::choices),
            Codec.STRING.optionalFieldOf("condition", "").forGetter(n ->
                n.displayCondition() != null ? n.displayCondition().getConditionId() : "")
        ).apply(instance, (id, speaker, text, choices, condId) ->
            new DialogueNode(id, speaker, text, choices, condId.isEmpty() ? null : new DialogueCondition.AlwaysTrueCondition()))
    );

    /**
     * Creates a simple dialogue node with no condition.
     */
    public DialogueNode(String id, String speakerName, String text, List<DialogueChoice> choices) {
        this(id, speakerName, text, choices, null);
    }

    /**
     * Gets the number of choices available to the player.
     *
     * @return the choice count
     */
    public int getChoiceCount() {
        return choices.size();
    }

    /**
     * Whether this is a terminal node (no choices lead elsewhere).
     *
     * @return true if this node has no outgoing choices
     */
    public boolean isTerminal() {
        return choices.isEmpty();
    }
}
