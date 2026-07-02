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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Water bucket interactions within dungeons:
 * - Water + Oil surface block = steam explosion (3 block radius, 2 hearts AoE damage).
 * - Water + fire trap = extinguish the fire trap.
 */
public class WaterBucketInteraction implements IVanillaInteraction {

    private static final float STEAM_EXPLOSION_RADIUS = 3.0f;
    private static final float STEAM_EXPLOSION_DAMAGE = 4.0f; // 2 hearts

    @Override
    public boolean canApply(InteractionContext context) {
        if (!context.stack().is(Items.WATER_BUCKET)) return false;
        // Can apply to oil surface blocks or braziers
        return context.blockState().is(CDEBlocks.OIL_SURFACE.get())
            || context.blockState().is(CDEBlocks.BRAZIER_ACTIVABLE.get());
    }

    @Override
    public InteractionResult apply(InteractionContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ServerLevel level = serverPlayer.serverLevel();
        BlockPos pos = context.pos();

        // Water + Oil = Steam explosion
        if (context.blockState().is(CDEBlocks.OIL_SURFACE.get())) {
            // Remove the oil block
            level.removeBlock(pos, false);

            // Deal AoE damage to entities in range
            AABB damageArea = new AABB(pos).inflate(STEAM_EXPLOSION_RADIUS);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, damageArea);
            for (LivingEntity entity : entities) {
                entity.hurt(level.damageSources().hotFloor(), STEAM_EXPLOSION_DAMAGE);
            }

            // Sound and visual feedback
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 1.5f, 0.5f);
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 0.8f, 1.5f);

            // Give back empty bucket
            serverPlayer.getInventory().add(Items.BUCKET.getDefaultInstance());

            return InteractionResult.SUCCESS;
        }

        // Water + Brazier = Extinguish
        if (context.blockState().is(CDEBlocks.BRAZIER_ACTIVABLE.get())) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 1.0f, 1.0f);
            serverPlayer.getInventory().add(Items.BUCKET.getDefaultInstance());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean consumesItem() {
        return true; // Water bucket consumed, empty bucket returned in apply()
    }

    @Override
    public int getCooldown() {
        return 40; // 2 second cooldown
    }
}
