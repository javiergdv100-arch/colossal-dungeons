package com.colossaldungeons.enhanced.entity.npc;

import com.colossaldungeons.enhanced.entity.CDEGeoEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Optional;
import java.util.UUID;

/**
 * Companion entity that follows and assists the player in dungeons.
 *
 * Features:
 * - 4 behavioral states: FOLLOW, WAIT, ATTACK, EXPLORE
 * - Follows owner and pathfinds with them
 * - Attacks targets on command
 * - Has its own 9-slot inventory
 * - Can participate in puzzle interactions
 * - Responds to CompanionOrder commands
 * - GeckoLib animated
 */
public class CompanionEntity extends CDEGeoEntity implements GeoEntity {

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
        SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final CompanionAI companionAI;
    private final CompanionInventory inventory;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.companion.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.companion.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.companion.run");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.companion.attack");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.companion.sit");

    public CompanionEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.companionAI = new CompanionAI();
        this.inventory = new CompanionInventory();
    }

    /**
     * Creates attribute supplier for companion entities.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, CompanionState.FOLLOW.ordinal());
        builder.define(OWNER_UUID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        // Goals are handled by CompanionAI state machine
    }

    // ========== Owner Management ==========

    /**
     * Sets the owner of this companion.
     *
     * @param player the owner player
     */
    public void setOwner(Player player) {
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
    }

    /**
     * Gets the owner's UUID.
     *
     * @return optional containing the owner UUID
     */
    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    /**
     * Gets the owner player entity (may return null if owner is offline/not found).
     *
     * @return the owner player or null
     */
    public Player getOwnerPlayer() {
        Optional<UUID> ownerUUID = getOwnerUUID();
        if (ownerUUID.isPresent()) {
            return this.level().getPlayerByUUID(ownerUUID.get());
        }
        return null;
    }

    // ========== State Management ==========

    /**
     * Gets the current companion state.
     *
     * @return the current state
     */
    public CompanionState getCompanionState() {
        return CompanionState.values()[this.entityData.get(STATE)];
    }

    /**
     * Sets the companion state.
     *
     * @param state the new state
     */
    public void setCompanionState(CompanionState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    /**
     * Processes a companion order, updating state and AI.
     *
     * @param order the order to process
     */
    public void receiveOrder(CompanionOrder order) {
        companionAI.onOrderReceived(order);
        setCompanionState(companionAI.getState());
    }

    // ========== Inventory ==========

    /**
     * Gets the companion's inventory.
     *
     * @return the 9-slot inventory
     */
    public CompanionInventory getCompanionInventory() {
        return inventory;
    }

    // ========== Tick and Interaction ==========

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            companionAI.tick(this);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            Optional<UUID> owner = getOwnerUUID();
            if (owner.isEmpty()) {
                // Claim this companion
                setOwner(player);
                return InteractionResult.SUCCESS;
            }
            if (owner.get().equals(player.getUUID())) {
                // Owner interaction - could open inventory or toggle state
                // For now, cycle through states
                CompanionState current = getCompanionState();
                CompanionState next = CompanionState.values()[(current.ordinal() + 1) % CompanionState.values().length];
                receiveOrder(CompanionOrder.values()[next.ordinal()]);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // ========== Persistence ==========

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CompanionState", getCompanionState().ordinal());
        getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.put("CompanionInventory", inventory.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CompanionState")) {
            setCompanionState(CompanionState.values()[tag.getInt("CompanionState")]);
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        if (tag.contains("CompanionInventory")) {
            inventory.load(tag.getCompound("CompanionInventory"));
        }
    }

    // ========== GeckoLib Animation ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            CompanionState companionState = getCompanionState();

            if (companionState == CompanionState.WAIT) {
                return state.setAndContinue(SIT);
            }

            if (state.isMoving()) {
                // Run if far from owner, walk otherwise
                Player owner = getOwnerPlayer();
                if (owner != null && distanceTo(owner) > 10.0) {
                    return state.setAndContinue(RUN);
                }
                return state.setAndContinue(WALK);
            }

            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 5, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.getController().forceAnimationReset();
            return state.setAndContinue(IDLE);
        }));
    }
}
