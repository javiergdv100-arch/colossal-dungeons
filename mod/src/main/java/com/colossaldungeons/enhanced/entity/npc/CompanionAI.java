package com.colossaldungeons.enhanced.entity.npc;

import com.colossaldungeons.enhanced.api.ICompanionBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * AI logic controller for companion entities.
 * Manages behavior trees for each CompanionState, handling pathfinding,
 * combat targeting, and idle behavior based on the current state.
 *
 * Behavior per state:
 * - FOLLOW: Follow owner at 3-6 block distance, speed up if too far
 * - WAIT: Stay still, look at owner
 * - ATTACK: Target nearest hostile mob, engage in melee
 * - EXPLORE: Wander randomly within the current room bounds
 */
public class CompanionAI implements ICompanionBehavior {

    private CompanionState currentState;
    private CompanionOrder lastOrder;
    private static final double FOLLOW_MIN_DISTANCE = 3.0;
    private static final double FOLLOW_MAX_DISTANCE = 6.0;
    private static final double TELEPORT_DISTANCE = 20.0;
    private static final double ATTACK_RANGE = 12.0;
    private static final double EXPLORE_RANGE = 16.0;

    public CompanionAI() {
        this.currentState = CompanionState.FOLLOW;
        this.lastOrder = CompanionOrder.FOLLOW;
    }

    @Override
    public void onOrderReceived(CompanionOrder order) {
        this.lastOrder = order;
        this.currentState = order.toState();
    }

    @Override
    public void tick(CompanionEntity companion) {
        switch (currentState) {
            case FOLLOW -> tickFollow(companion);
            case WAIT -> tickWait(companion);
            case ATTACK -> tickAttack(companion);
            case EXPLORE -> tickExplore(companion);
        }
    }

    @Override
    public CompanionState getState() {
        return currentState;
    }

    /**
     * FOLLOW behavior: pathfind to owner, maintain distance.
     */
    private void tickFollow(CompanionEntity companion) {
        Player owner = companion.getOwnerPlayer();
        if (owner == null) return;

        double distance = companion.distanceTo(owner);

        // Teleport if too far away
        if (distance > TELEPORT_DISTANCE) {
            companion.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0);
            return;
        }

        // Pathfind to owner if beyond follow distance
        if (distance > FOLLOW_MAX_DISTANCE) {
            companion.getNavigation().moveTo(owner, 1.2);
        } else if (distance > FOLLOW_MIN_DISTANCE) {
            companion.getNavigation().moveTo(owner, 1.0);
        } else {
            // Close enough - stop
            companion.getNavigation().stop();
        }
    }

    /**
     * WAIT behavior: stay still, look at owner.
     */
    private void tickWait(CompanionEntity companion) {
        companion.getNavigation().stop();

        Player owner = companion.getOwnerPlayer();
        if (owner != null) {
            companion.getLookControl().setLookAt(owner, 30.0f, 30.0f);
        }
    }

    /**
     * ATTACK behavior: find and engage nearest hostile mob.
     */
    private void tickAttack(CompanionEntity companion) {
        LivingEntity currentTarget = companion.getTarget();

        // Find a new target if current one is dead or gone
        if (currentTarget == null || !currentTarget.isAlive()
                || companion.distanceTo(currentTarget) > ATTACK_RANGE) {
            LivingEntity newTarget = findNearestHostile(companion);
            companion.setTarget(newTarget);
            currentTarget = newTarget;
        }

        // Engage target
        if (currentTarget != null && currentTarget.isAlive()) {
            double distToTarget = companion.distanceTo(currentTarget);
            if (distToTarget > 2.0) {
                companion.getNavigation().moveTo(currentTarget, 1.3);
            } else {
                companion.getNavigation().stop();
                companion.swing(companion.getUsedItemHand());
            }
        } else {
            // No targets - fall back to following owner
            tickFollow(companion);
        }
    }

    /**
     * EXPLORE behavior: wander around the area.
     */
    private void tickExplore(CompanionEntity companion) {
        if (!companion.getNavigation().isInProgress()) {
            // Pick a random position nearby
            double x = companion.getX() + (companion.getRandom().nextDouble() - 0.5) * EXPLORE_RANGE;
            double z = companion.getZ() + (companion.getRandom().nextDouble() - 0.5) * EXPLORE_RANGE;
            double y = companion.getY();

            companion.getNavigation().moveTo(x, y, z, 0.8);
        }
    }

    /**
     * Finds the nearest hostile mob within attack range.
     */
    private LivingEntity findNearestHostile(CompanionEntity companion) {
        AABB searchBox = companion.getBoundingBox().inflate(ATTACK_RANGE);
        List<Monster> hostiles = companion.level().getEntitiesOfClass(Monster.class, searchBox,
            e -> e.isAlive() && !e.isSpectator());

        Monster nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Monster hostile : hostiles) {
            double dist = companion.distanceTo(hostile);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = hostile;
            }
        }
        return nearest;
    }
}
