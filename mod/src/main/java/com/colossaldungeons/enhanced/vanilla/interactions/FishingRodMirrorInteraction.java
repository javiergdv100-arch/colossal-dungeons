package com.colossaldungeons.enhanced.vanilla.interactions;

import com.colossaldungeons.enhanced.core.registry.CDEBlocks;
import com.colossaldungeons.enhanced.vanilla.IVanillaInteraction;
import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import com.colossaldungeons.enhanced.vanilla.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Fishing rod + mirror panel interaction.
 * Allows the player to remotely rotate a mirror panel from a distance
 * using the fishing rod mechanic (cast and hook the mirror to spin it).
 */
public class FishingRodMirrorInteraction implements IVanillaInteraction {

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.FISHING_ROD)) return false;
        return context.blockState().is(CDEBlocks.MIRROR_PANEL.get());
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();

        // Rotate the mirror panel 90 degrees
        // In a full implementation, this would modify a block state property
        // For now, play the rotation sound and notify the puzzle engine
        level.playSound(null, context.pos(), SoundEvents.GRINDSTONE_USE,
            SoundSource.BLOCKS, 0.8f, 1.5f);

        // The actual rotation logic is handled by the LightRedirectPuzzle
        // when it detects an interaction at this position

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean consumesItem() {
        return false; // Fishing rod is not consumed
    }

    @Override
    public int getCooldown() {
        return 10; // Half second cooldown
    }
}
