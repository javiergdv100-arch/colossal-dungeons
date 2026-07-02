package com.colossaldungeons.enhanced.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * Base weapon class for all CDE custom weapons.
 * Extends SwordItem to inherit melee damage and enchantment behavior.
 * 
 * Features:
 * - Right-click activates the weapon's special ability (performAbility)
 * - Built-in cooldown system
 * - Optional charge mechanic (hold right-click)
 */
public abstract class CDEWeaponItem extends SwordItem {

    private final int abilityCooldown;

    /**
     * Creates a new CDE weapon.
     *
     * @param tier The item tier (determines damage, durability, enchantability)
     * @param properties Item properties with attribute modifiers
     * @param abilityCooldown Cooldown in ticks after using the ability
     */
    protected CDEWeaponItem(Tier tier, Properties properties, int abilityCooldown) {
        super(tier, properties);
        this.abilityCooldown = abilityCooldown;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.getCooldowns().isOnCooldown(this)) {
                boolean success = performAbility(level, serverPlayer, stack);
                if (success) {
                    serverPlayer.getCooldowns().addCooldown(this, abilityCooldown);
                    stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
                    return InteractionResultHolder.success(stack);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Performs the weapon's special ability. Called on right-click when not on cooldown.
     *
     * @param level The world level
     * @param player The player using the weapon
     * @param stack The weapon item stack
     * @return true if the ability was successfully performed (triggers cooldown)
     */
    protected abstract boolean performAbility(Level level, ServerPlayer player, ItemStack stack);

    /**
     * Gets the ability cooldown in ticks.
     */
    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    /**
     * Called when this weapon hits a living entity.
     * Override in subclasses to add on-hit effects.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide() && attacker instanceof ServerPlayer player) {
            onHitEffect(target, player, stack);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * Called server-side when this weapon hits an enemy.
     * Override to add special on-hit effects (e.g., applying status effects).
     *
     * @param target The entity that was hit
     * @param player The player who swung the weapon
     * @param stack The weapon stack
     */
    protected void onHitEffect(LivingEntity target, ServerPlayer player, ItemStack stack) {
        // Default: no special on-hit effect. Override in subclasses.
    }
}
