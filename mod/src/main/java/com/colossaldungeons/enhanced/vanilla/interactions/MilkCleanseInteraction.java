package com.colossaldungeons.enhanced.vanilla.interactions;

import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import com.colossaldungeons.enhanced.vanilla.IVanillaInteraction;
import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import com.colossaldungeons.enhanced.vanilla.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Milk bucket interaction within dungeons.
 * Removes ALL mod-specific effects: Oiled, Madness, Resonance Charge,
 * Hemorrhage, Frost Slow, Solar Burn, Parasitic Poison, Sanity Drain, etc.
 * Does NOT remove vanilla effects (handled by vanilla milk mechanic).
 */
public class MilkCleanseInteraction implements IVanillaInteraction {

    /** All CDE effects that milk should cleanse. */
    private static final List<DeferredHolder<MobEffect, MobEffect>> MOD_EFFECTS = List.of(
        CDEEffects.OILED,
        CDEEffects.MADNESS,
        CDEEffects.RESONANCE_CHARGE,
        CDEEffects.HEMORRHAGE,
        CDEEffects.FROST_SLOW,
        CDEEffects.SOLAR_BURN,
        CDEEffects.PARASITIC_POISON,
        CDEEffects.SANITY_DRAIN,
        CDEEffects.WEIGHT_ENCUMBRANCE
    );

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.MILK_BUCKET)) return false;

        // Can always apply in a dungeon if the player has any mod effects
        if (context.player() instanceof ServerPlayer sp) {
            return MOD_EFFECTS.stream().anyMatch(holder -> sp.hasEffect(holder));
        }
        return false;
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        boolean removedAny = false;
        for (DeferredHolder<MobEffect, MobEffect> effectHolder : MOD_EFFECTS) {
            if (serverPlayer.hasEffect(effectHolder)) {
                serverPlayer.removeEffect(effectHolder);
                removedAny = true;
            }
        }

        if (removedAny) {
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);

            // Give back empty bucket
            serverPlayer.getInventory().add(Items.BUCKET.getDefaultInstance());

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
        return 100; // 5 second cooldown (powerful effect)
    }
}
