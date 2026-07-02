package com.colossaldungeons.enhanced.dungeon.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Living Trap - Organic family of traps: breathing walls, grabbing roots/tentacles,
 * hidden mouths, digestive floors, cocoons, organic eyes.
 * 
 * Mechanics:
 * - Multiple organic variants chosen at instantiation
 * - Shears cut organic tissue instantly (disables trap segment)
 * - Flint and steel cauterizes (prevents regeneration permanently)
 * - Bone meal causes overgrowth that weakens structure
 * - Water neutralizes digestive floors
 * 
 * Config values used:
 * - damage.amount: grab/digest/bite damage
 * - damage.radius: tentacle/root reach
 * - timing.warningTicks: organic pulsing warning
 * - timing.activeTicks: grab/digest duration
 */
public class LivingTrap extends AbstractTrap {

    public enum OrganicType {
        BREATHING_WALL,
        GRABBING_ROOTS,
        GRABBING_TENTACLES,
        HIDDEN_MOUTH,
        DIGESTIVE_FLOOR,
        COCOON,
        ORGANIC_EYES
    }

    private static final float GRAB_HOLD_STRENGTH = 5;
    private static final int DIGEST_DAMAGE_INTERVAL = 10;
    private static final int REGEN_INTERVAL_TICKS = 600;

    private OrganicType organicType;
    private boolean cauterized;
    private boolean cutByShears;
    private boolean overgrown;
    private boolean neutralizedByWater;
    private int regenTimer;

    public LivingTrap(BlockPos position, TrapConfig config) {
        super(position, config);
        this.organicType = OrganicType.GRABBING_ROOTS;
        this.cauterized = false;
        this.cutByShears = false;
        this.overgrown = false;
        this.neutralizedByWater = false;
        this.regenTimer = 0;
    }

    @Override
    protected boolean shouldPrepare(ServerLevel level) {
        if (cauterized) return false;

        // If cut by shears but not cauterized, regenerate over time
        if (cutByShears && !cauterized) {
            regenTimer++;
            if (regenTimer >= REGEN_INTERVAL_TICKS) {
                cutByShears = false;
                regenTimer = 0;
            }
            return false;
        }

        // Check for water neutralizing digestive floors
        if (organicType == OrganicType.DIGESTIVE_FLOOR) {
            neutralizedByWater = level.getBlockState(position).is(Blocks.WATER) ||
                level.getBlockState(position.above()).is(Blocks.WATER);
            if (neutralizedByWater) return false;
        }

        List<LivingEntity> nearby = getTargetsInRange(level, config.activator().range());
        return !nearby.isEmpty();
    }

    @Override
    protected boolean shouldArm(ServerLevel level) {
        return tickCounter >= config.timing().warningTicks();
    }

    @Override
    protected void prepare(ServerLevel level) {
        // Organic tissue pulses - breathing motion visible
        // Wet squelching sounds
    }

    @Override
    protected void arm(ServerLevel level) {
        // Organic elements extend/open, ready to grab
    }

    @Override
    protected void trigger(ServerLevel level) {
        // Organic trap activates based on type
    }

    @Override
    protected void damage(ServerLevel level) {
        if (cauterized || cutByShears) {
            state = TrapState.COOLDOWN;
            tickCounter = 0;
            return;
        }

        List<LivingEntity> targets = getTargetsInRange(level);

        // Overgrown tissue is weaker
        float damageMultiplier = overgrown ? 0.4f : 1.0f;

        switch (organicType) {
            case BREATHING_WALL -> {
                // Constricts, applying suffocation damage
                DamageSource suffocateSource = level.damageSources().inWall();
                for (LivingEntity target : targets) {
                    target.hurt(suffocateSource, config.damage().amount() * damageMultiplier);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                }
            }
            case GRABBING_ROOTS, GRABBING_TENTACLES -> {
                // Grabs and holds entity, applies crush damage
                DamageSource grabSource = level.damageSources().generic();
                for (LivingEntity target : targets) {
                    target.hurt(grabSource, config.damage().amount() * damageMultiplier);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, GRAB_HOLD_STRENGTH));
                    target.setDeltaMovement(0, 0, 0);
                }
            }
            case HIDDEN_MOUTH -> {
                // Bites with hidden mouth
                DamageSource biteSource = level.damageSources().mobAttack(null);
                for (LivingEntity target : targets) {
                    target.hurt(biteSource, config.damage().amount() * 1.5f * damageMultiplier);
                }
            }
            case DIGESTIVE_FLOOR -> {
                // Acid damage each interval
                if (tickCounter % DIGEST_DAMAGE_INTERVAL == 0) {
                    DamageSource acidSource = level.damageSources().magic();
                    for (LivingEntity target : targets) {
                        target.hurt(acidSource, config.damage().amount() * damageMultiplier);
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                    }
                }
            }
            case COCOON -> {
                // Wraps entity in organic cocoon - blindness + slow damage
                DamageSource wrapSource = level.damageSources().generic();
                for (LivingEntity target : targets) {
                    target.hurt(wrapSource, config.damage().amount() * 0.3f * damageMultiplier);
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                }
            }
            case ORGANIC_EYES -> {
                // Eyes reveal and debuff - glowing + weakness
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                }
            }
        }
    }

    @Override
    protected void reset(ServerLevel level) {
        overgrown = false;
        neutralizedByWater = false;
    }

    /**
     * Cut by shears - disables trap segment temporarily.
     */
    public void cutWithShears() {
        this.cutByShears = true;
        this.regenTimer = 0;
        state = TrapState.COOLDOWN;
        tickCounter = 0;
    }

    /**
     * Cauterize with flint and steel - permanent disable.
     */
    public void cauterize() {
        this.cauterized = true;
        state = TrapState.INACTIVE;
    }

    /**
     * Apply bone meal - causes overgrowth that weakens.
     */
    public void applyBoneMeal() {
        this.overgrown = true;
    }

    public void setOrganicType(OrganicType type) {
        this.organicType = type;
    }

    public OrganicType getOrganicType() {
        return organicType;
    }

    public boolean isCauterized() {
        return cauterized;
    }
}
