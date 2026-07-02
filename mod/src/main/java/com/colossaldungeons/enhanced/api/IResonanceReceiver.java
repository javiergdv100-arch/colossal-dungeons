package com.colossaldungeons.enhanced.api;

/**
 * Public API interface for blocks/entities that can receive resonance pulses.
 * Implement this on BlockEntities or entities that should respond to resonance signals
 * from Sculk Sensors, Note Blocks, Bells, or other resonance sources.
 * 
 * Addon mods can implement this interface to create custom resonance-responsive blocks.
 */
public interface IResonanceReceiver {

    /**
     * Called when a resonance pulse is received.
     *
     * @param power the strength of the resonance pulse (typically 1-100)
     */
    void onResonancePulse(int power);

    /**
     * Gets the minimum resonance threshold needed to affect this receiver.
     * Pulses with power below this value are ignored.
     *
     * @return the minimum power level that triggers a response
     */
    int getResonanceThreshold();
}
