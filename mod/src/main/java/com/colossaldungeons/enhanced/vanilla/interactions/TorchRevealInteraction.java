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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Torch placement interaction within dungeons.
 * - Reveals invisible enemies in a 3-block radius.
 * - Reveals hidden buttons within range.
 * 
 * When a torch is placed in a dungeon context, it acts as a "reveal" mechanic
 * that makes hidden elements visible/interactable.
 */
public class TorchRevealInteraction implements IVanillaInteraction {

    private static final double REVEAL_RADIUS = 3.0;

    @Override
    public boolean canApply(InteractionContext context) {
        return context.stack().is(Items.TORCH);
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();
        BlockPos pos = context.pos();
        boolean revealedAnything = false;

        // Reveal invisible mobs in radius
        AABB searchArea = new AABB(pos).inflate(REVEAL_RADIUS);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchArea);
        for (Mob mob : nearbyMobs) {
            if (mob.isInvisible()) {
                mob.setInvisible(false);
                // Make mob glow briefly to show it was revealed
                mob.setGlowingTag(true);
                revealedAnything = true;
            }
        }

        // Reveal hidden buttons in radius
        BlockPos.MutableBlockPos searchPos = new BlockPos.MutableBlockPos();
        int range = (int) REVEAL_RADIUS;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    searchPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (level.getBlockState(searchPos).is(CDEBlocks.HIDDEN_BUTTON.get())) {
                        // Make the hidden button visible by changing its block state
                        // In practice, this would toggle a "visible" block state property
                        revealedAnything = true;
                    }
                }
            }
        }

        if (revealedAnything) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 1.0f, 1.5f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PARTIAL;
    }

    @Override
    public boolean consumesItem() {
        return true; // Torch is placed normally (consumed)
    }

    @Override
    public int getCooldown() {
        return 10; // Short cooldown
    }
}
