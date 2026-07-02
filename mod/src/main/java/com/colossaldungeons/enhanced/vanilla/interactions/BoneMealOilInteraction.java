package com.colossaldungeons.enhanced.vanilla.interactions;

import com.colossaldungeons.enhanced.core.registry.CDEBlocks;
import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import com.colossaldungeons.enhanced.core.registry.CDESounds;
import com.colossaldungeons.enhanced.vanilla.IVanillaInteraction;
import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import com.colossaldungeons.enhanced.vanilla.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;

/**
 * Bone meal + Oil block interaction. EXACTLY per section 7.14:
 * - Bone meal on oil surface block: cleanses the oil block and grants OilProtection (30 minutes).
 * - Bone meal + sneaking when player has Oiled effect: self-cleanse (removes Oiled effect).
 */
public class BoneMealOilInteraction implements IVanillaInteraction {

    private static final int OIL_PROTECTION_DURATION = 30 * 60 * 20; // 30 minutes in ticks

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.BONE_MEAL)) return false;

        // Case 1: Bone meal on oil surface block
        if (context.blockState().is(CDEBlocks.OIL_SURFACE.get())) {
            return true;
        }

        // Case 2: Sneaking with Oiled effect = self-cleanse
        if (context.player().isShiftKeyDown() && context.player() instanceof ServerPlayer sp) {
            return sp.hasEffect(CDEEffects.OILED);
        }

        return false;
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();

        // Case 1: Bone meal on oil surface block
        if (context.blockState().is(CDEBlocks.OIL_SURFACE.get())) {
            // Remove the oil block (cleanse)
            level.removeBlock(context.pos(), false);

            // Grant OilProtection effect for 30 minutes
            serverPlayer.addEffect(new MobEffectInstance(
                CDEEffects.OIL_PROTECTION, OIL_PROTECTION_DURATION, 0,
                false, true, true
            ));

            // Play cleanse sound
            level.playSound(null, context.pos(), CDESounds.BONE_MEAL_CLEANSE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);

            return InteractionResult.SUCCESS;
        }

        // Case 2: Self-cleanse when sneaking with Oiled effect
        if (context.player().isShiftKeyDown() && serverPlayer.hasEffect(CDEEffects.OILED)) {
            serverPlayer.removeEffect(CDEEffects.OILED);

            // Grant a shorter OilProtection
            serverPlayer.addEffect(new MobEffectInstance(
                CDEEffects.OIL_PROTECTION, OIL_PROTECTION_DURATION / 2, 0,
                false, true, true
            ));

            level.playSound(null, serverPlayer.blockPosition(), CDESounds.BONE_MEAL_CLEANSE.get(),
                SoundSource.PLAYERS, 1.0f, 1.2f);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean consumesItem() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 20; // 1 second cooldown
    }
}
