package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Elemental Column Trap - Columns emerge from floor charged with current floor element.
 * 
 * Mechanics:
 * - Columns rise from the floor in a pattern
 * - Each column is charged with the current dungeon floor's element (fire, ice, lightning, poison)
 * - Opposite element item disables the column (water vs fire, fire vs ice, etc.)
 * - Spyglass reveals the emergence pattern from distance
 * - Shield blocks column impact damage
 * - Columns follow predictable sequential or rotating patterns
 * 
 * Config values used:
 * - damage.amount: elemental damage per column hit
 * - damage.radius: column impact radius
 * - timing.warningTicks: rumble before emergence
 * - timing.activeTicks: column active duration
 */
public class ElementalColumnTrap extends AbstractTrap {

    public enum ElementType {
        FIRE, ICE, LIGHTNING, POISON
    }

    private static final int COLUMN_EMERGE_INTERVAL = 30;
    private static final int MAX_COLUMNS = 6;
    private static final float COLUMN_KNOCKUP = 0.6f;

    private ElementType element;
    private int activeColumnIndex;
    private boolean disabledByOpposite;
    private int totalColumns;

    public ElementalColumnTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.element = ElementType.FIRE;
        this.activeColumnIndex = 0;
        this.disabledByOpposite = false;
        this.totalColumns = MAX_COLUMNS;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (disabledByOpposite) return false;
        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Floor rumbles - cracks appear in column emergence pattern
        // Element glow visible through cracks
        activeColumnIndex = 0;
    }

    @Override
    protected void arm(ServerLevel level) {
        // First column about to emerge - elemental charge visible
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Columns begin emerging in sequence
    }

    @Override
    protected void damage(ServerLevel level) {
        if (disabledByOpposite) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        // Advance column pattern
        if (tickCounter % COLUMN_EMERGE_INTERVAL == 0) {
            activeColumnIndex = (activeColumnIndex + 1) % totalColumns;
        }

        // Calculate column position based on pattern
        double angle = (2.0 * Math.PI * activeColumnIndex) / totalColumns;
        double colRadius = config.damage().radius() * 0.7;
        double colX = position.getX() + Math.cos(angle) * colRadius;
        double colZ = position.getZ() + Math.sin(angle) * colRadius;
        BlockPos columnPos = BlockPos.containing(colX, position.getY(), colZ);

        // Damage entities near the active column
        List<LivingEntity> targets = getTargetsInRange(level, 2.0);

        for (LivingEntity target : targets) {
            // Check if entity is near the active column
            double distToColumn = target.position().distanceTo(new Vec3(colX, position.getY(), colZ));
            if (distToColumn > 2.0) continue;

            // Shield blocks column impact
            if (target.isBlocking()) {
                continue;
            }

            // Apply elemental damage based on type
            DamageSource elementSource;
            switch (element) {
                case FIRE -> {
                    elementSource = level.damageSources().onFire();
                    target.setRemainingFireTicks(60);
                }
                case ICE -> {
                    elementSource = level.damageSources().freeze();
                    target.setTicksFrozen(target.getTicksFrozen() + 80);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
                case LIGHTNING -> {
                    elementSource = level.damageSources().lightningBolt();
                    target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 0));
                }
                case POISON -> {
                    elementSource = level.damageSources().magic();
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0));
                }
                default -> elementSource = level.damageSources().generic();
            }

            target.hurt(elementSource, config.damage().amount());
            // Column impact launches entity upward
            target.push(0, COLUMN_KNOCKUP, 0);
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        activeColumnIndex = 0;
        disabledByOpposite = false;
    }

    /**
     * Called when the opposite element item is used.
     * Disables the trap.
     */
    public void disableWithOppositeElement() {
        this.disabledByOpposite = true;
    }

    /**
     * Sets the elemental type for this column trap.
     */
    public void setElement(ElementType element) {
        this.element = element;
    }

    public ElementType getElement() {
        return element;
    }

    /**
     * Gets the emergence pattern (for spyglass observation).
     */
    public int getActiveColumnIndex() {
        return activeColumnIndex;
    }

    public int getTotalColumns() {
        return totalColumns;
    }
}
