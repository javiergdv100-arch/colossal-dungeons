package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Template Method pattern for dungeon traps.
 * Subclasses implement the abstract hooks; the tick() method drives the state machine.
 * 
 * State machine: INACTIVE -> PREPARED -> ARMED -> TRIGGERED -> COOLDOWN -> (INACTIVE or ARMED)
 */
public abstract class AbstractTrap {

    /**
     * Trap lifecycle states.
     */
    public enum TrapState {
        /** Trap is completely inactive and will not process. */
        INACTIVE,
        /** Trap is loaded and preparing (warming up). */
        PREPARED,
        /** Trap is armed and waiting for a trigger condition. */
        ARMED,
        /** Trap has been triggered and is dealing effects. */
        TRIGGERED,
        /** Trap is on cooldown before it can re-arm. */
        COOLDOWN
    }

    protected TrapState state = TrapState.INACTIVE;
    protected BlockPos position;
    protected TrapConfig config;
    protected int tickCounter;
    protected TrapActivator activator;

    protected AbstractTrap(BlockPos position, TrapConfig config) {
        this.position = position;
        this.config = config;
        this.tickCounter = 0;
        this.activator = TrapActivator.fromConfig(config.activator());
    }

    /**
     * Main tick method - final to enforce the Template Method pattern.
     * Processes the trap state machine each game tick.
     */
    public final void tick(ServerLevel level) {
        tickCounter++;

        switch (state) {
            case INACTIVE -> {
                if (shouldPrepare(level)) {
                    prepare(level);
                    state = TrapState.PREPARED;
                    tickCounter = 0;
                }
            }
            case PREPARED -> {
                if (shouldArm(level)) {
                    arm(level);
                    state = TrapState.ARMED;
                    tickCounter = 0;
                }
            }
            case ARMED -> {
                if (activator.shouldTrigger(level, position)) {
                    trigger(level);
                    state = TrapState.TRIGGERED;
                    tickCounter = 0;
                }
            }
            case TRIGGERED -> {
                damage(level);
                if (tickCounter >= config.timing().activeTicks()) {
                    state = TrapState.COOLDOWN;
                    tickCounter = 0;
                }
            }
            case COOLDOWN -> {
                if (tickCounter >= config.cooldownTicks()) {
                    reset(level);
                    if (config.repeatable()) {
                        state = TrapState.ARMED;
                    } else {
                        state = TrapState.INACTIVE;
                    }
                    tickCounter = 0;
                }
            }
        }
    }

    // ========== Abstract Methods (Template Method hooks) ==========

    /**
     * Determines if the trap should begin preparing (transition from INACTIVE to PREPARED).
     * Typically checks if players are in the same room.
     */
    protected abstract boolean shouldPrepare(ServerLevel level);

    /**
     * Determines if the trap should arm (transition from PREPARED to ARMED).
     * Typically checks if preparation is complete.
     */
    protected abstract boolean shouldArm(ServerLevel level);

    /**
     * Called when the trap transitions to PREPARED state.
     * Setup warning effects, sounds, etc.
     */
    protected abstract void prepare(ServerLevel level);

    /**
     * Called when the trap transitions to ARMED state.
     * Final setup before the trap can trigger.
     */
    protected abstract void arm(ServerLevel level);

    /**
     * Called when the trap triggers.
     * Start the damage/effect sequence.
     */
    protected abstract void trigger(ServerLevel level);

    /**
     * Called each tick while in TRIGGERED state.
     * Apply damage and effects to entities in range.
     */
    protected abstract void damage(ServerLevel level);

    /**
     * Called when the trap resets after cooldown.
     * Clean up effects, retract physical elements.
     */
    protected abstract void reset(ServerLevel level);

    // ========== Helper Methods ==========

    /**
     * Gets all living entities within the configured damage radius of this trap.
     */
    protected List<LivingEntity> getTargetsInRange(ServerLevel level) {
        double radius = config.damage().radius();
        AABB area = new AABB(position).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area);
    }

    /**
     * Gets all living entities within a custom radius of this trap.
     */
    protected List<LivingEntity> getTargetsInRange(ServerLevel level, double radius) {
        AABB area = new AABB(position).inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area);
    }

    // ========== Getters ==========

    public TrapState getState() { return state; }
    public BlockPos getPosition() { return position; }
    public TrapConfig getConfig() { return config; }
    public int getTickCounter() { return tickCounter; }

    /**
     * Force-set the trap state (used for loading saved state).
     */
    public void setState(TrapState state) {
        this.state = state;
    }
}
