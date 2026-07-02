package com.colossaldungeons.enhanced.entity.boss;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.colossaldungeons.enhanced.network.CDENetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages phase transitions for boss entities.
 * Monitors the boss health each tick and triggers phase transitions
 * when health thresholds are crossed. Sends BossPhasePayload network
 * packets to tracking players on transition.
 *
 * @param <T> the boss entity type, must be both a Mob and implement IBossPhase
 */
public class BossPhaseManager<T extends Mob & IBossPhase> {

    private final T boss;
    private final List<PhaseThreshold> thresholds;
    private int currentPhase;
    private boolean transitioning;

    /**
     * Represents a health threshold that triggers a phase transition.
     */
    public record PhaseThreshold(int phaseOrdinal, float healthFraction) {}

    /**
     * Creates a new BossPhaseManager for the specified boss.
     *
     * @param boss the boss entity to manage
     */
    public BossPhaseManager(T boss) {
        this.boss = boss;
        this.thresholds = new ArrayList<>();
        this.currentPhase = 0;
        this.transitioning = false;
    }

    /**
     * Adds a health threshold that triggers a phase transition.
     * Thresholds should be added in decreasing order (e.g., 0.75, 0.5, 0.25).
     *
     * @param phaseOrdinal the phase to transition to
     * @param healthFraction the health fraction (0.0-1.0) that triggers this phase
     * @return this manager for chaining
     */
    public BossPhaseManager<T> addThreshold(int phaseOrdinal, float healthFraction) {
        this.thresholds.add(new PhaseThreshold(phaseOrdinal, healthFraction));
        return this;
    }

    /**
     * Should be called every tick to check for phase transitions.
     * If the boss health drops below a threshold, triggers the transition.
     */
    public void tick() {
        if (transitioning || boss.isDeadOrDying()) {
            return;
        }

        float healthFraction = boss.getHealth() / boss.getMaxHealth();

        for (PhaseThreshold threshold : thresholds) {
            if (threshold.phaseOrdinal() > currentPhase && healthFraction <= threshold.healthFraction()) {
                triggerTransition(threshold.phaseOrdinal());
                break;
            }
        }
    }

    /**
     * Triggers a phase transition, updating the boss and notifying clients.
     *
     * @param newPhase the phase ordinal to transition to
     */
    private void triggerTransition(int newPhase) {
        transitioning = true;
        int oldPhase = currentPhase;
        currentPhase = newPhase;

        // Notify the boss entity
        boss.transitionToPhase(newPhase);

        // Send network sync to all tracking players
        if (boss.level() instanceof ServerLevel serverLevel) {
            CDENetworking.BossPhasePayload payload =
                new CDENetworking.BossPhasePayload(boss.getId(), newPhase);

            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceTo(boss) < 64.0) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }

        transitioning = false;
    }

    /**
     * Gets the current phase ordinal.
     *
     * @return the current phase
     */
    public int getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Forces a phase transition regardless of health thresholds.
     * Useful for scripted events.
     *
     * @param phase the target phase
     */
    public void forcePhase(int phase) {
        if (phase != currentPhase) {
            triggerTransition(phase);
        }
    }

    /**
     * Resets the phase manager to initial state.
     */
    public void reset() {
        this.currentPhase = 0;
        this.transitioning = false;
    }
}
