package com.colossaldungeons.enhanced.api;

import com.colossaldungeons.enhanced.entity.npc.CompanionEntity;
import com.colossaldungeons.enhanced.entity.npc.CompanionOrder;
import com.colossaldungeons.enhanced.entity.npc.CompanionState;

/**
 * Public API interface for companion entity behavior logic.
 * Defines the contract for how companions respond to orders and tick their AI.
 *
 * Addon mods can implement this to create custom companion behavior modes.
 */
public interface ICompanionBehavior {

    /**
     * Called when the companion receives a new order from its owner.
     *
     * @param order the order to process
     */
    void onOrderReceived(CompanionOrder order);

    /**
     * Called every tick to update companion behavior based on current state.
     *
     * @param companion the companion entity being ticked
     */
    void tick(CompanionEntity companion);

    /**
     * Gets the current behavioral state of the companion.
     *
     * @return the current companion state
     */
    CompanionState getState();
}
