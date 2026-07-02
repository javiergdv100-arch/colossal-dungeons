package com.colossaldungeons.enhanced.item;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import com.colossaldungeons.enhanced.core.registry.CDEEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Sanity Candle - consumable item that cleanses sanity effects.
 * 
 * Behavior:
 * - On use: removes SanityDrain and Madness effects
 * - Resets MADNESS_LEVEL attachment to 0
 * - Has durability (64 uses)
 * - Plays soothing sound on use
 */
public class SanityCandleItem extends Item {

    public SanityCandleItem() {
        super(new Properties().durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean hadEffect = false;

            // Remove SanityDrain effect
            if (serverPlayer.hasEffect(CDEEffects.SANITY_DRAIN)) {
                serverPlayer.removeEffect(CDEEffects.SANITY_DRAIN);
                hadEffect = true;
            }

            // Remove Madness effect
            if (serverPlayer.hasEffect(CDEEffects.MADNESS)) {
                serverPlayer.removeEffect(CDEEffects.MADNESS);
                hadEffect = true;
            }

            // Reset MADNESS_LEVEL attachment to 0
            int currentMadness = serverPlayer.getData(CDEAttachments.MADNESS_LEVEL);
            if (currentMadness > 0) {
                serverPlayer.setData(CDEAttachments.MADNESS_LEVEL, 0);
                hadEffect = true;
            }

            if (hadEffect) {
                // Play cleansing sound
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0f, 1.5f);

                // Consume durability
                stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));

                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}
