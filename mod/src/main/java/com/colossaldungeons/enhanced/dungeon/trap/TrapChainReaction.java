package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Observer pattern implementation for chain reactions between traps.
 * When a trap triggers, it can notify connected traps to also trigger.
 */
public class TrapChainReaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrapChainReaction.class);

    private final AbstractTrap sourceTrap;
    private final List<AbstractTrap> connectedTraps;
    private int delayTicks;
    private int currentDelay;
    private boolean pending;

    public TrapChainReaction(AbstractTrap sourceTrap) {
        this.sourceTrap = sourceTrap;
        this.connectedTraps = new ArrayList<>();
        this.delayTicks = 5; // Default chain delay
        this.currentDelay = 0;
        this.pending = false;
    }

    /**
     * Adds a trap to the chain reaction list.
     */
    public void addConnectedTrap(AbstractTrap trap) {
        if (trap != sourceTrap) {
            connectedTraps.add(trap);
        }
    }

    /**
     * Removes a trap from the chain reaction list.
     */
    public void removeConnectedTrap(AbstractTrap trap) {
        connectedTraps.remove(trap);
    }

    /**
     * Called when the source trap triggers. Starts the chain reaction countdown.
     */
    public void onSourceTriggered() {
        if (!connectedTraps.isEmpty()) {
            pending = true;
            currentDelay = 0;
            LOGGER.debug("Chain reaction initiated from {}, notifying {} connected traps",
                sourceTrap.getPosition(), connectedTraps.size());
        }
    }

    /**
     * Tick the chain reaction. After the delay, triggers all connected traps.
     */
    public void tick(ServerLevel level) {
        if (!pending) return;

        currentDelay++;
        if (currentDelay >= delayTicks) {
            triggerChain(level);
            pending = false;
            currentDelay = 0;
        }
    }

    /**
     * Triggers all connected traps in the chain.
     */
    private void triggerChain(ServerLevel level) {
        for (AbstractTrap connectedTrap : connectedTraps) {
            if (connectedTrap.getState() == AbstractTrap.TrapState.ARMED) {
                // Force trigger the connected trap
                connectedTrap.setState(AbstractTrap.TrapState.TRIGGERED);
                LOGGER.debug("Chain reaction triggered trap at {}", connectedTrap.getPosition());
            }
        }
    }

    /**
     * Sets the delay in ticks before the chain reaction triggers.
     */
    public void setDelayTicks(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public AbstractTrap getSourceTrap() { return sourceTrap; }
    public List<AbstractTrap> getConnectedTraps() { return connectedTraps; }
    public boolean isPending() { return pending; }
}
