package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Infinite Corridor Trap - Spatial loop where player advances but returns to same section.
 * 
 * Mechanics:
 * - Player walks forward but is teleported back imperceptibly
 * - Magic lamp in ceiling maintains the spatial loop
 * - Snowball or crossbow bolt at the lamp breaks the illusion
 * - Loop has subtle tells (slight shimmer, deja vu particles)
 * - Drains hunger and applies confusion while trapped
 * 
 * Config values used:
 * - damage.amount: hunger drain per cycle
 * - damage.radius: corridor length before loop teleport
 * - timing.warningTicks: time before loop activates
 * - timing.activeTicks: maximum time in loop before auto-release
 */
public class InfiniteCorridorTrap extends AbstractTrap {

    private static final int LOOP_TELEPORT_DISTANCE = 12;
    private static final int CONFUSION_DURATION = 60;
    private static final int HUNGER_DRAIN_INTERVAL = 40;

    private boolean lampBroken;
    private BlockPos lampPosition;
    private int loopCycles;

    public InfiniteCorridorTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.lampBroken = false;
        this.lampPosition = position.above(4); // Magic lamp 4 blocks above
        this.loopCycles = 0;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (lampBroken) return false;
        List<Player> players = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.activator().range()));
        return !players.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Corridor begins to shimmer subtly
        // Ambient sounds become slightly distorted
    }

    @Override
    protected void arm(ServerLevel level) {
        // Spatial loop is fully established
        // Magic lamp glows brighter
        loopCycles = 0;
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Loop activates - player enters infinite corridor
        // Reality warps around them
    }

    @Override
    protected void damage(ServerLevel level) {
        if (lampBroken) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        List<Player> trapped = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.damage().radius()));

        for (Player player : trapped) {
            // Apply confusion effect
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CONFUSION_DURATION, 0));

            // Drain hunger periodically
            if (tickCounter % HUNGER_DRAIN_INTERVAL == 0) {
                player.getFoodData().setFoodLevel(
                    Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }

            // Teleport player back to start of corridor (spatial loop)
            if (player.position().distanceTo(Vec3.atCenterOf(position)) > LOOP_TELEPORT_DISTANCE) {
                player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                loopCycles++;
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        // Spatial loop dissolves
        // Reality stabilizes
        lampBroken = false;
        loopCycles = 0;
    }

    /**
     * Called when a projectile (snowball/crossbow) hits the magic lamp.
     * Breaks the spatial loop, freeing trapped players.
     */
    public void breakLamp() {
        this.lampBroken = true;
    }

    public BlockPos getLampPosition() {
        return lampPosition;
    }

    public int getLoopCycles() {
        return loopCycles;
    }

    public boolean isLampBroken() {
        return lampBroken;
    }
}
