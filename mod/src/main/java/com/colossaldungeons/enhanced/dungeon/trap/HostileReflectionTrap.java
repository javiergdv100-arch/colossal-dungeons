package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Hostile Reflection Trap - Player reflection emerges from mirror to attack.
 * 
 * Mechanics:
 * - Player's reflection steps out of a mirror as a hostile copy
 * - Reflection has same equipment and attacks with it
 * - Water (empana) applied to mirror PREVENTS emergence
 * - Snowball breaks mirror to kill active reflection
 * - Shield blocks reflection's attacks
 * - Reflection deals player's melee damage
 * 
 * Config values used:
 * - damage.amount: base reflection attack damage
 * - damage.radius: mirror detection range
 * - timing.warningTicks: mirror ripple before emergence
 * - timing.activeTicks: reflection lifetime
 */
public class HostileReflectionTrap extends AbstractTrap {

    private static final float REFLECTION_DAMAGE_SCALE = 0.8f;
    private static final int ATTACK_INTERVAL_TICKS = 30;

    private boolean reflectionActive;
    private boolean mirrorBroken;
    private boolean mirrorWaterProtected;
    private float reflectionHealth;

    public HostileReflectionTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.reflectionActive = false;
        this.mirrorBroken = false;
        this.mirrorWaterProtected = false;
        this.reflectionHealth = 20.0f;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (mirrorBroken || mirrorWaterProtected) return false;
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
        // Mirror surface begins to ripple
        // Player's reflection moves independently
    }

    @Override
    protected void arm(ServerLevel level) {
        // Reflection reaches toward the glass from inside
        // Mirror bulges outward
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Reflection steps out of the mirror
        reflectionActive = true;
        reflectionHealth = 20.0f;
    }

    @Override
    protected void damage(ServerLevel level) {
        if (mirrorBroken || !reflectionActive || reflectionHealth <= 0) {
            reflectionActive = false;
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // Reflection attacks nearby players at intervals
        if (tickCounter % ATTACK_INTERVAL_TICKS != 0) return;

        List<Player> targets = level.getEntitiesOfClass(Player.class,
            new AABB(position).inflate(config.damage().radius()));
        DamageSource reflectionSource = level.damageSources().generic();

        for (Player target : targets) {
            // Shield blocks reflection attacks
            if (target.isBlocking()) {
                continue;
            }

            float damage = config.damage().amount() * REFLECTION_DAMAGE_SCALE;
            target.hurt(reflectionSource, damage);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        reflectionActive = false;
        reflectionHealth = 20.0f;
        mirrorWaterProtected = false;
    }

    /**
     * Called when a snowball breaks the mirror.
     * Kills any active reflection.
     */
    public void breakMirror() {
        this.mirrorBroken = true;
        this.reflectionActive = false;
    }

    /**
     * Called when water is applied to the mirror surface.
     * Prevents reflection from emerging.
     */
    public void applyWaterProtection() {
        this.mirrorWaterProtected = true;
    }

    /**
     * Damage the reflection entity.
     */
    public void damageReflection(float amount) {
        this.reflectionHealth -= amount;
    }

    public boolean isReflectionActive() {
        return reflectionActive;
    }

    public boolean isMirrorBroken() {
        return mirrorBroken;
    }
}
