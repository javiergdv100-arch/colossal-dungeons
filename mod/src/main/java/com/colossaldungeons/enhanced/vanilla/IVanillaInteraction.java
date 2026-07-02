package com.colossaldungeons.enhanced.vanilla;

/**
 * Interface for vanilla item interactions that have special behavior within dungeons.
 * Each implementation defines how a specific item+block combination behaves in dungeon context.
 */
public interface IVanillaInteraction {

    /**
     * Checks if this interaction can be applied in the given context.
     *
     * @param context the interaction context
     * @return true if the interaction is applicable
     */
    boolean canApply(InteractionContext context);

    /**
     * Applies the interaction effect.
     *
     * @param context the interaction context
     * @return the result of the interaction
     */
    InteractionResult apply(InteractionContext context);

    /**
     * Whether this interaction consumes the item used.
     *
     * @return true if the item should be consumed
     */
    boolean consumesItem();

    /**
     * Gets the cooldown in ticks before this interaction can be used again.
     *
     * @return cooldown in ticks (0 for no cooldown)
     */
    int getCooldown();
}
