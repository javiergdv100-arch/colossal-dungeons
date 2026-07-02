package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Mirror Room Trap - Rotating mirror panels reorganizing reflections periodically.
 * 
 * Mechanics:
 * - Room has mirror panels that rotate, changing reflection angles
 * - Creates disorientation and false paths
 * - Snowballs break panels (reduces room complexity)
 * - Spyglass reveals real exit through reflections
 * - Water (empana) reveals real panels vs illusory ones
 * - Players take confusion damage the longer they stay
 * 
 * Config values used:
 * - damage.amount: confusion/disorientation damage over time
 * - damage.radius: room bounds
 * - timing.warningTicks: time between mirror rotations
 * - timing.activeTicks: rotation duration
 */
public class MirrorRoomTrap extends AbstractTrap {

    private static final int ROTATION_INTERVAL_TICKS = 200;
    private static final int MAX_PANELS = 8;
    private static final int CONFUSION_BUILDUP_INTERVAL = 100;

    private int activePanels;
    private int rotationCycle;
    private boolean realExitRevealed;

    public MirrorRoomTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.activePanels = MAX_PANELS;
        this.rotationCycle = 0;
        this.realExitRevealed = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (activePanels <= 0) return false;
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
        // Mirror panels begin to glow faintly
        // Reflections start to shift
        rotationCycle = 0;
    }

    @Override
    protected void arm(ServerLevel level) {
        // Panels locked into initial configuration
        // Room fully active
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Mirrors begin rotating - reflections scramble
        // Disorienting visual effects begin
    }

    @Override
    protected void damage(ServerLevel level) {
        if (activePanels <= 0 || realExitRevealed) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // Rotate panels periodically
        if (tickCounter % ROTATION_INTERVAL_TICKS == 0) {
            rotationCycle++;
        }

        // Apply disorientation to players in the mirror room
        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());

        if (tickCounter % CONFUSION_BUILDUP_INTERVAL == 0) {
            for (LivingEntity target : targets) {
                // Confusion intensifies over time
                int intensity = Math.min(rotationCycle, 3);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, intensity));

                // Minor psychic damage from disorientation
                DamageSource confusionSource = level.damageSources().magic();
                target.hurt(confusionSource, config.damage().amount());
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        rotationCycle = 0;
        realExitRevealed = false;
    }

    /**
     * Called when a snowball breaks a mirror panel.
     * Reduces room complexity, making navigation easier.
     */
    public void breakPanel() {
        this.activePanels = Math.max(0, this.activePanels - 1);
    }

    /**
     * Called when spyglass is used to observe reflections.
     * Reveals the real exit.
     */
    public void revealExitWithSpyglass() {
        this.realExitRevealed = true;
    }

    /**
     * Called when water reveals real vs illusory panels.
     */
    public void revealWithWater() {
        this.activePanels = Math.max(0, this.activePanels - 3);
    }

    public int getActivePanels() {
        return activePanels;
    }

    public int getRotationCycle() {
        return rotationCycle;
    }
}
