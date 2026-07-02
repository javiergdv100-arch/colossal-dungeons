package com.colossaldungeons.enhanced.vanilla;

/**
 * Result of a vanilla item interaction attempt within a dungeon.
 */
public enum InteractionResult {
    /** Interaction was successful and fully applied. */
    SUCCESS,
    /** Interaction failed (conditions not met). */
    FAIL,
    /** Interaction partially applied (e.g., some effect but not full). */
    PARTIAL,
    /** Interaction blocked by cooldown. */
    COOLDOWN
}
