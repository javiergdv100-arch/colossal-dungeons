package com.colossaldungeons.enhanced.entity.ai;

import com.colossaldungeons.enhanced.entity.boss.DyingAtlasEntity;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

import java.util.List;

/**
 * Brain provider for the Dying Atlas boss, following section 7.10 of the technical plan.
 *
 * Sensors:
 * - NearbyPlayersSensor: detects players in range
 * - CDEVibrationSensor: detects vibrations/sounds nearby
 * - CDEPhaseHealthSensor: monitors health for phase transitions
 *
 * Core tasks:
 * - LookAtTarget: always look toward attack target
 * - MoveToWalkTarget: pathfind toward target
 *
 * Fight tasks:
 * - Phase-conditional behaviors wrapping different attacks per phase:
 *   Phase 1: Melee slam attacks
 *   Phase 2: Sweeping area attacks
 *   Phase 3: Frenzied rapid attacks + area attacks
 */
public class DyingAtlasBrainProvider extends CDEBrainProvider<DyingAtlasEntity> {

    public DyingAtlasBrainProvider(DyingAtlasEntity entity) {
        super(entity);
    }

    @Override
    public List<? extends ExtendedSensor<? extends DyingAtlasEntity>> getSensors() {
        return List.of(
            new NearbyPlayersSensor<>(),
            new CDEVibrationSensor<>(),
            new CDEPhaseHealthSensor<>()
        );
    }

    @Override
    public BrainActivityGroup<? extends DyingAtlasEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
            new LookAtTarget<>(),
            new MoveToWalkTarget<>()
        );
    }

    @Override
    public BrainActivityGroup<? extends DyingAtlasEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(
            new FirstApplicableBehaviour<DyingAtlasEntity>(
                // Phase 3: Frenzied attacks (highest priority when in phase 3)
                new CDEPhaseConditionalBehavior<>(
                    DyingAtlasEntity.BossPhase.PHASE_3.ordinal(),
                    new CDETelegraphedAreaAttack<>(5.0f, 20, 15.0f)
                ),
                // Phase 2: Sweeping attacks
                new CDEPhaseConditionalBehavior<>(
                    DyingAtlasEntity.BossPhase.PHASE_2.ordinal(),
                    new CDETelegraphedAreaAttack<>(4.0f, 30, 12.0f)
                ),
                // Phase 1: Basic melee (default)
                new CDEPhaseConditionalBehavior<>(
                    DyingAtlasEntity.BossPhase.PHASE_1.ordinal(),
                    new AnimatableMeleeAttack<>(20)
                )
            )
        );
    }

    @Override
    public BrainActivityGroup<? extends DyingAtlasEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
            new LookAtTarget<>()
        );
    }
}
