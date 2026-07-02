package com.colossaldungeons.enhanced.entity.npc;

import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Base NPC entity with dialogue support and GeckoLib animations.
 * NPCs are non-attackable friendly creatures that offer dialogue interactions.
 *
 * Features:
 * - Dialogue tree for conversations
 * - Per-player memory tracking
 * - Idle and talking animations
 * - Invulnerable by default (cannot be killed by players)
 */
public class NPCEntity extends CDEGeoEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.npc.idle");
    private static final RawAnimation TALKING = RawAnimation.begin().thenLoop("animation.npc.talking");

    protected DialogueTree dialogueTree;
    protected final NPCMemory memory;
    private boolean isTalking;

    public NPCEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.memory = new NPCMemory();
        this.dialogueTree = null;
        this.isTalking = false;
        this.setInvulnerable(true);
    }

    /**
     * Creates attribute supplier for NPCs.
     * NPCs have moderate health but no damage (non-combatant).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    /**
     * Sets the dialogue tree for this NPC.
     *
     * @param tree the dialogue tree to use
     */
    public void setDialogueTree(DialogueTree tree) {
        this.dialogueTree = tree;
    }

    /**
     * Gets the dialogue tree for this NPC.
     *
     * @return the dialogue tree, or null if not set
     */
    public DialogueTree getDialogueTree() {
        return dialogueTree;
    }

    /**
     * Gets the NPC memory tracker.
     *
     * @return the memory instance
     */
    public NPCMemory getMemory() {
        return memory;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (dialogueTree != null) {
                startDialogue(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    /**
     * Starts a dialogue session with the specified player.
     * Retrieves the player's last node from memory and presents the dialogue.
     *
     * @param player the server player to interact with
     */
    protected void startDialogue(ServerPlayer player) {
        if (dialogueTree == null) return;

        isTalking = true;

        // Get the player's last position in this dialogue, or start from root
        String lastNode = memory.getLastNode(player.getUUID());
        DialogueNode startNode = dialogueTree.getNode(lastNode);

        // If the last node no longer exists or fails condition, start from root
        if (startNode == null) {
            startNode = dialogueTree.getRootNode();
        }
        if (startNode.displayCondition() != null && !startNode.displayCondition().test(player)) {
            startNode = dialogueTree.getRootNode();
        }

        // Record the interaction
        memory.recordVisit(player.getUUID(), startNode.id());

        // In a full implementation, this would send a dialogue packet to the client
        // to open the dialogue UI with the current node's text and choices
    }

    /**
     * Handles a player making a dialogue choice.
     *
     * @param player the player
     * @param choiceIndex the choice index
     */
    public void handleDialogueChoice(ServerPlayer player, int choiceIndex) {
        if (dialogueTree == null) return;

        String currentNodeId = memory.getLastNode(player.getUUID());
        DialogueNode nextNode = dialogueTree.advance(player, currentNodeId, choiceIndex);

        if (nextNode != null) {
            memory.recordVisit(player.getUUID(), nextNode.id());
            // Send next dialogue state to client
        } else {
            // Dialogue ended
            isTalking = false;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // NPCs are invulnerable to player attacks
        if (source.getEntity() instanceof Player) {
            return false;
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // NPCMemory serialization would go here via Codec
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // NPCMemory deserialization would go here via Codec
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isTalking) {
                return state.setAndContinue(TALKING);
            }
            return state.setAndContinue(IDLE);
        }));
    }
}
