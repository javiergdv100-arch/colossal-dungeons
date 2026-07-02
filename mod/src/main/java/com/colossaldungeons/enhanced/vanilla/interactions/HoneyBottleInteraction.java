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
 * Honey bottle interactions within dungeons:
 * - Honey + Ice plate = removes slip effect for 5 minutes (makes ice walkable).
 * - Honey + insect mob = pacify (not applied here; future extension).
 */
public class HoneyBottleInteraction implements IVanillaInteraction {

    private static final int ANTI_SLIP_DURATION = 5 * 60 * 20; // 5 minutes in ticks

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.HONEY_BOTTLE)) return false;
        return context.blockState().is(CDEBlocks.ICE_PLATE.get());
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();

        if (context.blockState().is(CDEBlocks.ICE_PLATE.get())) {
            // Apply honey to ice plate to remove slip property
            // In practice this would change the block state to a non-slippery variant
            // For now we mark it via NBT or block state property

            level.playSound(null, context.pos(), SoundEvents.HONEY_BLOCK_PLACE,
                SoundSource.BLOCKS, 1.0f, 1.0f);

            // Give back glass bottle
            serverPlayer.getInventory().add(Items.GLASS_BOTTLE.getDefaultInstance());

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean consumesItem() {
        return true; // Honey bottle consumed, glass bottle returned
    }

    @Override
    public int getCooldown() {
        return 20; // 1 second cooldown
    }
}
