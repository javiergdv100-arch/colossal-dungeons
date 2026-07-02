package com.colossaldungeons.enhanced.vanilla.interactions;

import com.colossaldungeons.enhanced.core.registry.CDEBlocks;
import com.colossaldungeons.enhanced.vanilla.IVanillaInteraction;
import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import com.colossaldungeons.enhanced.vanilla.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;

/**
 * Powder snow bucket interaction over void/abyss areas.
 * Creates a temporary platform that lasts 15-30 seconds.
 * The platform uses the POWDER_SNOW_PLATFORM block registered in CDEBlocks.
 */
public class PowderSnowPlatformInteraction implements IVanillaInteraction {

    private static final int MIN_DURATION_TICKS = 15 * 20; // 15 seconds
    private static final int MAX_DURATION_TICKS = 30 * 20; // 30 seconds

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.POWDER_SNOW_BUCKET)) return false;

        // Must be placed over void (air below) or on air blocks
        BlockPos below = context.pos().below();
        return context.level().getBlockState(below).isAir()
            || context.level().getBlockState(context.pos().above()).isAir();
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();
        BlockPos placePos = context.pos().above();

        // Place the temporary platform block
        if (level.getBlockState(placePos).isAir()) {
            level.setBlock(placePos, CDEBlocks.POWDER_SNOW_PLATFORM.get().defaultBlockState(), 3);

            // Schedule removal after random duration between 15-30 seconds
            int duration = MIN_DURATION_TICKS + level.random.nextInt(MAX_DURATION_TICKS - MIN_DURATION_TICKS);
            level.scheduleTick(placePos, CDEBlocks.POWDER_SNOW_PLATFORM.get(), duration);

            // Sound feedback
            level.playSound(null, placePos, SoundEvents.POWDER_SNOW_PLACE,
                SoundSource.BLOCKS, 1.0f, 1.0f);

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
        return 60; // 3 second cooldown to prevent spam
    }
}
