package com.colossaldungeons.enhanced.item;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Blizzard Bow - frost-enchanted bow that applies FrostSlow on hit.
 * 
 * Behavior:
 * - Extends BowItem for standard bow mechanics
 * - Arrows apply FrostSlow on hit
 * - Charged shot (full draw): creates frost AoE on impact location
 * - Durability: 512
 */
public class BlizzardBow extends BowItem {

    /** Frost slow duration on normal hit (4 seconds). */
    private static final int FROST_DURATION = 80;

    /** Frost AoE radius for charged shots. */
    private static final double FROST_AOE_RADIUS = 4.0;

    /** Frost AoE duration (6 seconds). */
    private static final int FROST_AOE_DURATION = 120;

    public BlizzardBow() {
        super(new Properties().durability(512));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (entity instanceof Player player && !level.isClientSide()) {
            // Check for arrows in inventory
            ItemStack arrowStack = player.getProjectile(stack);
            if (arrowStack.isEmpty() && !player.getAbilities().instabuild) {
                return;
            }

            int chargeTime = this.getUseDuration(stack, entity) - timeCharged;
            float power = getPowerForTime(chargeTime);

            if (power >= 0.1f) {
                Arrow arrow = new Arrow(level, player, arrowStack.copyWithCount(1), stack);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, power * 3.0f, 1.0f);

                if (power >= 1.0f) {
                    arrow.setCritArrow(true);
                    // Mark as charged shot for AoE effect - using custom tag
                    arrow.getPersistentData().putBoolean("cde_frost_charged", true);
                }

                // Apply frost effect data to arrow
                arrow.addEffect(new MobEffectInstance(CDEEffects.FROST_SLOW, FROST_DURATION, 0));

                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));

                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);

                if (!player.getAbilities().instabuild) {
                    arrowStack.shrink(1);
                }
            }
        }
    }

    /**
     * Creates a frost AoE at the impact location.
     * Called from an arrow impact event handler when a blizzard bow arrow hits.
     *
     * @param level The server level
     * @param impactX X coordinate of impact
     * @param impactY Y coordinate of impact
     * @param impactZ Z coordinate of impact
     */
    public static void createFrostAoE(ServerLevel level, double impactX, double impactY, double impactZ) {
        AABB aoe = new AABB(impactX - FROST_AOE_RADIUS, impactY - 1, impactZ - FROST_AOE_RADIUS,
            impactX + FROST_AOE_RADIUS, impactY + 3, impactZ + FROST_AOE_RADIUS);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aoe);
        for (LivingEntity target : entities) {
            target.addEffect(new MobEffectInstance(
                CDEEffects.FROST_SLOW, FROST_AOE_DURATION, 1,
                false, true, true
            ));
        }

        // Play frost sound
        level.playSound(null, (int) impactX, (int) impactY, (int) impactZ,
            SoundEvents.POWDER_SNOW_STEP, SoundSource.NEUTRAL, 1.5f, 0.5f);
    }
}
