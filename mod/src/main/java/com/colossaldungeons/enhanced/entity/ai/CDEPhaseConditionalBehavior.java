package com.colossaldungeons.enhanced.entity.ai;

import com.colossaldungeons.enhanced.api.IBossPhase;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.List;

/**
 * Wraps another behavior and only allows execution when the boss is in the specified phase.
 * This enables phase-gated attack patterns in the brain-based AI system.
 *
 * Usage example:
 * <pre>
 * new CDEPhaseConditionalBehavior&lt;&gt;(
 *     DyingAtlasEntity.BossPhase.PHASE_2.ordinal(),
 *     new AnimatableMeleeAttack&lt;&gt;(20)
 * )
 * </pre>
 *
 * @param <E> the entity type (must implement IBossPhase)
 */
public class CDEPhaseConditionalBehavior<E extends Mob & IBossPhase> extends ExtendedBehaviour<E> {

    private final int requiredPhase;
    private final ExtendedBehaviour<? super E> wrappedBehavior;

    /**
     * Creates a phase-conditional behavior wrapper.
     *
     * @param requiredPhase the phase ordinal required for this behavior to execute
     * @param wrappedBehavior the behavior to execute when the phase matches
     */
    public CDEPhaseConditionalBehavior(int requiredPhase, ExtendedBehaviour<? super E> wrappedBehavior) {
        this.requiredPhase = requiredPhase;
        this.wrappedBehavior = wrappedBehavior;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return entity.getCurrentPhase() == requiredPhase;
    }

    @Override
    protected void start(ServerLevel level, E entity, long gameTime) {
        if (entity.getCurrentPhase() == requiredPhase) {
            wrappedBehavior.tryStart(level, entity, gameTime);
        }
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        if (entity.getCurrentPhase() != requiredPhase) {
            doStop(level, entity, gameTime);
            return;
        }
        wrappedBehavior.tickOrStop(level, entity, gameTime);
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        wrappedBehavior.doStop(level, entity, gameTime);
    }
}
