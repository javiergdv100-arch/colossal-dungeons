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
import net.minecraft.world.level.block.state.BlockState;

/**
 * TNT interactions within dungeons:
 * - TNT near weak walls = destroy the wall.
 * - TNT near oil surface = ignite chain reaction.
 * - TNT near resonance crystal = instant overload.
 */
public class TNTExplosionInteraction implements IVanillaInteraction {

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.TNT)) return false;

        BlockState state = context.blockState();
        // Can apply to resonance crystals, oil surfaces, or membrane blocks (weak walls)
        return state.is(CDEBlocks.RESONANCE_CRYSTAL.get())
            || state.is(CDEBlocks.OIL_SURFACE.get())
            || state.is(CDEBlocks.MEMBRANE_BLOCK.get());
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();
        BlockPos pos = context.pos();
        BlockState state = context.blockState();

        // TNT + Resonance Crystal = Instant overload
        if (state.is(CDEBlocks.RESONANCE_CRYSTAL.get())) {
            // The ResonanceCrystalEntity would handle the actual overload logic
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // TNT + Oil = Ignite chain
        if (state.is(CDEBlocks.OIL_SURFACE.get())) {
            // Ignite the oil block and propagate fire
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS, 1.0f, 1.0f);

            // Fire would propagate to adjacent oil blocks
            // Handled by chain reaction system
            return InteractionResult.SUCCESS;
        }

        // TNT + Weak wall (membrane block) = Destroy
        if (state.is(CDEBlocks.MEMBRANE_BLOCK.get())) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 1.5f, 0.8f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean consumesItem() {
        return true; // TNT is consumed
    }

    @Override
    public int getCooldown() {
        return 60; // 3 second cooldown (powerful)
    }
}
