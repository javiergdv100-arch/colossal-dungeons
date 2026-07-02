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
 * Devouring Door Trap - Living door organism that bites, holds, and swallows.
 * 
 * Mechanics:
 * - Opens like a mouth, absorbs entities that pass through
 * - Bite phase: deals heavy damage and holds entity in place
 * - Swallow phase: pulls entity inside, applies digestion damage
 * - Spit phase: ejects entity after damage
 * - Flint and steel cauterizes PERMANENTLY (door dies)
 * - Shears cut jaw tendons (safe passage permanently)
 * - Eggs fed inside prevent bite for 30 seconds
 * 
 * Config values used:
 * - damage.amount: bite damage per tick
 * - damage.radius: grab range of the mouth
 * - timing.warningTicks: mouth opening warning
 * - timing.activeTicks: full digest cycle time
 */
public class DevouringDoorTrap extends AbstractTrap {

    private static final int BITE_PHASE_TICKS = 40;
    private static final int SWALLOW_PHASE_TICKS = 60;
    private static final int SPIT_PHASE_TICKS = 20;
    private static final int EGG_SATIATION_TICKS = 600; // 30 seconds
    private static final float DIGESTION_DAMAGE_MULTIPLIER = 0.5f;

    private enum DoorPhase { MOUTH_OPEN, BITE, SWALLOW, SPIT }

    private DoorPhase currentPhase;
    private boolean cauterized;
    private boolean jawsCut;
    private boolean fedEggs;
    private int eggTimer;

    public DevouringDoorTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.currentPhase = DoorPhase.MOUTH_OPEN;
        this.cauterized = false;
        this.jawsCut = false;
        this.fedEggs = false;
        this.eggTimer = 0;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (cauterized || jawsCut) return false;
        if (fedEggs) {
            eggTimer--;
            if (eggTimer <= 0) fedEggs = false;
            return false;
        }
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
        // Door organism stirs - subtle breathing visible
        // Edges of door pulse slightly
        currentPhase = DoorPhase.MOUTH_OPEN;
    }

    @Override
    protected void arm(ServerLevel level) {
        // Mouth fully opens - teeth/tendrils visible
        // Organic sounds (wet, pulsing)
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Entity passes through - mouth snaps shut
        currentPhase = DoorPhase.BITE;
    }

    @Override
    protected void damage(ServerLevel level) {
        if (cauterized || jawsCut) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        List<LivingEntity> targets = getTargetsInRange(level, config.damage().radius());

        switch (currentPhase) {
            case BITE -> {
                // Heavy bite damage + hold in place
                DamageSource biteSource = level.damageSources().mobAttack(null);
                for (LivingEntity target : targets) {
                    target.hurt(biteSource, config.damage().amount());
                    // Hold entity in place (slow + no jump)
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4));
                    target.setDeltaMovement(0, 0, 0);
                }
                if (tickCounter > BITE_PHASE_TICKS) {
                    currentPhase = DoorPhase.SWALLOW;
                }
            }
            case SWALLOW -> {
                // Pull entity inside, digestion damage
                DamageSource digestSource = level.damageSources().magic();
                for (LivingEntity target : targets) {
                    target.hurt(digestSource, config.damage().amount() * DIGESTION_DAMAGE_MULTIPLIER);
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                }
                if (tickCounter > BITE_PHASE_TICKS + SWALLOW_PHASE_TICKS) {
                    currentPhase = DoorPhase.SPIT;
                }
            }
            case SPIT -> {
                // Eject entity forcefully
                for (LivingEntity target : targets) {
                    target.push(0, 0.5, -2.0);
                }
                if (tickCounter > BITE_PHASE_TICKS + SWALLOW_PHASE_TICKS + SPIT_PHASE_TICKS) {
                    state = TrapState.COOLDOWN;
                    tickCounter = 0;
                }
            }
            default -> {}
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        currentPhase = DoorPhase.MOUTH_OPEN;
    }

    /**
     * Called when flint and steel is used on the door.
     * Permanently cauterizes the organism - it dies.
     */
    public void cauterize() {
        this.cauterized = true;
        state = TrapState.INACTIVE;
    }

    /**
     * Called when shears are used on the jaw tendons.
     * Permanently prevents biting - safe passage.
     */
    public void cutJawTendons() {
        this.jawsCut = true;
        state = TrapState.INACTIVE;
    }

    /**
     * Called when eggs are fed to the door.
     * Prevents bite for 30 seconds.
     */
    public void feedEggs() {
        this.fedEggs = true;
        this.eggTimer = EGG_SATIATION_TICKS;
    }

    public boolean isCauterized() {
        return cauterized;
    }

    public boolean areJawsCut() {
        return jawsCut;
    }
}
