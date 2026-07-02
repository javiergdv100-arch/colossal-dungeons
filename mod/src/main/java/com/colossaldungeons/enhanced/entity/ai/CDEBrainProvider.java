package com.colossaldungeons.enhanced.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

import java.util.List;

/**
 * Abstract base class implementing SmartBrainOwner for CDE entities.
 * Provides the template method pattern for AI definition using SmartBrainLib.
 *
 * Subclasses define their sensors, core tasks, fight tasks, and idle tasks
 * to create complete AI behavior trees.
 *
 * @param <T> the entity type this brain provider manages
 */
public abstract class CDEBrainProvider<T extends PathfinderMob> implements SmartBrainOwner<T> {

    protected final T entity;

    protected CDEBrainProvider(T entity) {
        this.entity = entity;
    }

    /**
     * Gets the list of sensors for this entity's brain.
     * Sensors detect environmental conditions and populate memory modules.
     *
     * @return list of extended sensors
     */
    @Override
    public abstract List<? extends ExtendedSensor<? extends T>> getSensors();

    /**
     * Gets the core tasks (always-running behaviors like looking and moving).
     *
     * @return the core task activity group
     */
    @Override
    public abstract BrainActivityGroup<? extends T> getCoreTasks();

    /**
     * Gets the fight tasks (combat behaviors triggered by hostility).
     *
     * @return the fight task activity group
     */
    @Override
    public abstract BrainActivityGroup<? extends T> getFightTasks();

    /**
     * Gets the idle tasks (default behaviors when not engaged in combat).
     *
     * @return the idle task activity group
     */
    @Override
    public abstract BrainActivityGroup<? extends T> getIdleTasks();
}
