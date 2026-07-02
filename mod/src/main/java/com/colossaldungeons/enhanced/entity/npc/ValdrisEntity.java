package com.colossaldungeons.enhanced.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Valdris - the merchant NPC with unique dialogue tree.
 * Offers shops, quests, and reputation-gated dialogue branches.
 *
 * Dialogue structure:
 * - Greeting (varies based on reputation)
 * - Shop access (always available)
 * - Quest offers (reputation-gated)
 * - Farewell
 *
 * Has custom animations for idle, talking, and a unique gesture animation
 * used during important dialogue moments.
 */
public class ValdrisEntity extends NPCEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.valdris.idle");
    private static final RawAnimation TALKING = RawAnimation.begin().thenLoop("animation.valdris.talking");
    private static final RawAnimation GESTURE = RawAnimation.begin().thenPlay("animation.valdris.gesture");

    private boolean isGesturing;

    public ValdrisEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.isGesturing = false;
        initializeDialogueTree();
    }

    /**
     * Creates attribute supplier for Valdris.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Initializes Valdris's complete dialogue tree with reputation-gated branches.
     */
    private void initializeDialogueTree() {
        // Root greeting node
        DialogueNode greeting = new DialogueNode(
            "root", "Valdris",
            "Welcome, traveler. The dungeons hold many secrets... and many dangers. How may I assist you?",
            List.of(
                new DialogueChoice("Show me your wares.", "shop", null, null),
                new DialogueChoice("Do you have any work for me?", "quest_offer",
                    new DialogueCondition.ReputationCondition("valdris", 5), null),
                new DialogueChoice("Tell me about this place.", "lore", null, null),
                new DialogueChoice("Farewell.", "farewell", null, null)
            )
        );

        // High reputation greeting
        DialogueNode greetingHighRep = new DialogueNode(
            "greeting_high_rep", "Valdris",
            "Ah, my trusted friend returns! The dungeons have been particularly active of late. I have special stock for one such as yourself.",
            List.of(
                new DialogueChoice("Show me the special merchandise.", "shop_special", null, null),
                new DialogueChoice("What's been happening?", "quest_urgent", null, null),
                new DialogueChoice("Just browsing today.", "shop", null, null),
                new DialogueChoice("Until next time, Valdris.", "farewell_friend", null, null)
            ),
            new DialogueCondition.ReputationCondition("valdris", 20)
        );

        // Shop node
        DialogueNode shop = new DialogueNode(
            "shop", "Valdris",
            "Take a look at what I have. Everything here has been tested in the depths below.",
            List.of(
                new DialogueChoice("[Browse Shop]", "root", null,
                    new DialogueAction.OpenShopAction("valdris_general")),
                new DialogueChoice("Actually, never mind.", "root", null, null)
            )
        );

        // Special shop (reputation-gated)
        DialogueNode shopSpecial = new DialogueNode(
            "shop_special", "Valdris",
            "For someone of your standing, I keep the truly rare items in the back. Enchanted beyond what most can afford.",
            List.of(
                new DialogueChoice("[Browse Special Stock]", "root", null,
                    new DialogueAction.OpenShopAction("valdris_special")),
                new DialogueChoice("Perhaps later.", "root", null, null)
            )
        );

        // Quest offer node (requires minimum reputation)
        DialogueNode questOffer = new DialogueNode(
            "quest_offer", "Valdris",
            "Indeed I do. There are whispers of an ancient artifact deep in the ruins below. " +
            "If you could retrieve it, I would make it well worth your trouble.",
            List.of(
                new DialogueChoice("Tell me more about this artifact.", "quest_details", null, null),
                new DialogueChoice("What's the pay?", "quest_reward", null, null),
                new DialogueChoice("Not interested right now.", "root", null, null)
            )
        );

        // Quest details
        DialogueNode questDetails = new DialogueNode(
            "quest_details", "Valdris",
            "The Atlas Shard - a fragment of a being that once held the world together. " +
            "It lies in the chamber of the Dying Atlas. Be warned: the guardian still stirs.",
            List.of(
                new DialogueChoice("I accept. I'll retrieve the shard.", "quest_accepted", null,
                    new DialogueAction.StartQuestAction("retrieve_atlas_shard")),
                new DialogueChoice("That sounds too dangerous.", "root", null, null)
            )
        );

        // Quest accepted
        DialogueNode questAccepted = new DialogueNode(
            "quest_accepted", "Valdris",
            "Excellent! Be careful down there. The Atlas is weakened, but still deadly. " +
            "Return to me when you have the shard.",
            List.of(
                new DialogueChoice("I'll be back.", "root", null,
                    new DialogueAction.SetReputationAction("valdris", 2))
            )
        );

        // Quest reward discussion
        DialogueNode questReward = new DialogueNode(
            "quest_reward", "Valdris",
            "For the Atlas Shard? I would offer enchanted armor, rare potions, " +
            "and a significant sum of emeralds. A fair trade for the risk involved.",
            List.of(
                new DialogueChoice("Sounds fair. I'll do it.", "quest_accepted", null,
                    new DialogueAction.StartQuestAction("retrieve_atlas_shard")),
                new DialogueChoice("I'll think about it.", "root", null, null)
            )
        );

        // Urgent quest (high rep)
        DialogueNode questUrgent = new DialogueNode(
            "quest_urgent", "Valdris",
            "Something stirs in the deepest chambers. Tremors that should not be. " +
            "I need someone I trust to investigate. Will you go?",
            List.of(
                new DialogueChoice("Count me in.", "quest_accepted_urgent", null,
                    new DialogueAction.StartQuestAction("investigate_tremors")),
                new DialogueChoice("What can you tell me first?", "quest_urgent_details", null, null)
            )
        );

        // Urgent quest details
        DialogueNode questUrgentDetails = new DialogueNode(
            "quest_urgent_details", "Valdris",
            "The resonance crystals are behaving erratically. Some have shattered entirely. " +
            "Whatever is causing this, it is growing stronger.",
            List.of(
                new DialogueChoice("I'll look into it immediately.", "quest_accepted_urgent", null,
                    new DialogueAction.StartQuestAction("investigate_tremors")),
                new DialogueChoice("I need to prepare first.", "root", null, null)
            )
        );

        // Urgent quest accepted
        DialogueNode questAcceptedUrgent = new DialogueNode(
            "quest_accepted_urgent", "Valdris",
            "Thank you, my friend. Take this - it may help you in the depths.",
            List.of(
                new DialogueChoice("Thank you, Valdris.", "root", null,
                    new DialogueAction.SetReputationAction("valdris", 5))
            )
        );

        // Lore node
        DialogueNode lore = new DialogueNode(
            "lore", "Valdris",
            "These ruins were once a great civilization. They fell when the Atlas - a being " +
            "of immense power - began to die. Its decay corrupted everything around it.",
            List.of(
                new DialogueChoice("What is the Atlas?", "lore_atlas", null, null),
                new DialogueChoice("Why do you stay here?", "lore_valdris", null, null),
                new DialogueChoice("Thank you for the information.", "root", null, null)
            )
        );

        // Lore: Atlas
        DialogueNode loreAtlas = new DialogueNode(
            "lore_atlas", "Valdris",
            "A titan of living stone, once tasked with holding the world's foundations together. " +
            "Now it decays in the depths, its corruption spreading outward. A pitiable creature.",
            List.of(
                new DialogueChoice("Can it be saved?", "lore_atlas_save", null, null),
                new DialogueChoice("I've heard enough.", "root", null, null)
            )
        );

        // Lore: Save Atlas
        DialogueNode loreAtlasSave = new DialogueNode(
            "lore_atlas_save", "Valdris",
            "Some say it could be, if one gathered the scattered shards of its power. " +
            "But such an endeavor would be... extremely dangerous.",
            List.of(
                new DialogueChoice("Interesting. I'll keep that in mind.", "root", null, null)
            )
        );

        // Lore: Valdris himself
        DialogueNode loreValdris = new DialogueNode(
            "lore_valdris", "Valdris",
            "Someone must help the brave souls who venture into the depths. " +
            "I provide supplies, knowledge, and occasionally... hope.",
            List.of(
                new DialogueChoice("Noble of you.", "root", null,
                    new DialogueAction.SetReputationAction("valdris", 1)),
                new DialogueChoice("Back to business.", "root", null, null)
            )
        );

        // Farewell nodes
        DialogueNode farewell = new DialogueNode(
            "farewell", "Valdris",
            "Safe travels, adventurer. The dungeons are unforgiving.",
            List.of()
        );

        DialogueNode farewellFriend = new DialogueNode(
            "farewell_friend", "Valdris",
            "Until next time, my friend. May your blade stay sharp and your torch stay lit.",
            List.of()
        );

        // Build the tree
        DialogueTree tree = new DialogueTree(greeting);
        tree.addNode(greetingHighRep);
        tree.addNode(shop);
        tree.addNode(shopSpecial);
        tree.addNode(questOffer);
        tree.addNode(questDetails);
        tree.addNode(questAccepted);
        tree.addNode(questReward);
        tree.addNode(questUrgent);
        tree.addNode(questUrgentDetails);
        tree.addNode(questAcceptedUrgent);
        tree.addNode(lore);
        tree.addNode(loreAtlas);
        tree.addNode(loreAtlasSave);
        tree.addNode(loreValdris);
        tree.addNode(farewell);
        tree.addNode(farewellFriend);

        this.setDialogueTree(tree);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isGesturing) {
                isGesturing = false;
                return state.setAndContinue(GESTURE);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "talk", 5, state -> {
            // Check if any nearby player is in conversation
            if (!this.level().isClientSide()) {
                var nearestPlayer = this.level().getNearestPlayer(this, 4.0);
                if (nearestPlayer != null) {
                    return state.setAndContinue(TALKING);
                }
            }
            return state.setAndContinue(IDLE);
        }));
    }

    /**
     * Triggers the gesture animation (used during important dialogue moments).
     */
    public void triggerGesture() {
        this.isGesturing = true;
    }
}
