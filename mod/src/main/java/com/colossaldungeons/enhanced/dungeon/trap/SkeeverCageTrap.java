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
 * Skeever Cage Trap - Heavy cage (500HP) falls from ceiling trapping player.
 * 
 * Mechanics:
 * - Cage falls when player steps into trigger zone
 * - After 3 minutes (3600 ticks), fills with poison gas
 * - Crossbow bolt or snowball at actuator mechanism frees player
 * - Ender pearl can escape if line of sight available
 * - Cage has 500HP and can be destroyed by raw damage
 * 
 * Config values used:
 * - damage.amount: poison gas damage per tick
 * - damage.radius: cage interior radius
 * - timing.warningTicks: cage drop warning (creak sound)
 * - timing.activeTicks: time before gas (3600 default)
 */
public class SkeeverCageTrap extends AbstractTrap {

    private static final float CAGE_MAX_HEALTH = 500.0f;
    private static final int GAS_FILL_TICKS = 3600; // 3 minutes
    private static final int POISON_DURATION = 100;

    private float cageHealth;
    private boolean cageFallen;
    private boolean gasActive;
    private boolean freedByProjectile;

    public SkeeverCageTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.cageHealth = CAGE_MAX_HEALTH;
        this.cageFallen = false;
        this.gasActive = false;
        this.freedByProjectile = false;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (cageFallen) return false;
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
        // Chain creaks above - subtle warning
    }

    @Override
    protected void arm(ServerLevel level) {
        // Cage is ready to drop
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Cage drops from ceiling, trapping player
        cageFallen = true;
        gasActive = false;
    }

    @Override
    protected void damage(ServerLevel level) {
        if (freedByProjectile || cageHealth <= 0) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // After 3 minutes, gas fills the cage
        if (tickCounter >= GAS_FILL_TICKS) {
            gasActive = true;
        }

        if (gasActive) {
            List<LivingEntity> trapped = getTargetsInRange(level, config.damage().radius());
            DamageSource poisonSource = level.damageSources().magic();

            for (LivingEntity target : trapped) {
                target.hurt(poisonSource, config.damage().amount());
                target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION, 1));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, POISON_DURATION, 0));
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        cageFallen = false;
        gasActive = false;
        freedByProjectile = false;
        cageHealth = CAGE_MAX_HEALTH;
    }

    /**
     * Called when a projectile hits the actuator mechanism.
     */
    public void onActuatorHit() {
        this.freedByProjectile = true;
    }

    /**
     * Apply damage to the cage itself.
     */
    public void damageCage(float amount) {
        this.cageHealth -= amount;
    }

    public float getCageHealth() {
        return cageHealth;
    }

    public boolean isGasActive() {
        return gasActive;
    }
}
