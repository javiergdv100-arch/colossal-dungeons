package com.colossaldungeons.enhanced.item.curio;

import com.colossaldungeons.enhanced.block.puzzle.HiddenButtonBlock;
import com.colossaldungeons.enhanced.core.registry.CDEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Oracle Relic curio - reveals hidden buttons and trap positions.
 * 
 * Behavior:
 * - curioTick: every 40 ticks, scans for hidden buttons within 10 blocks
 * - Sends particle info to the player to highlight hidden elements
 * - Reveals trap positions via glowing outline (server-side entity glow)
 */
public class OracleRelicItem extends Item implements ICurioItem {

    /** Scan radius for hidden elements. */
    private static final int SCAN_RADIUS = 10;

    /** Tick interval between scans. */
    private static final int SCAN_INTERVAL = 40;

    public OracleRelicItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (wearer.level().isClientSide()) return;
        if (!(wearer instanceof ServerPlayer player)) return;

        // Scan every 40 ticks
        if (wearer.tickCount % SCAN_INTERVAL != 0) return;

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        // Scan for hidden buttons and trap blocks in radius
        BlockPos.betweenClosedStream(
            playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
            playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)
        ).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            // Highlight hidden buttons
            if (state.is(CDEBlocks.HIDDEN_BUTTON.get())) {
                if (state.hasProperty(HiddenButtonBlock.REVEALED) && !state.getValue(HiddenButtonBlock.REVEALED)) {
                    // Send particle at the hidden button location
                    level.sendParticles(player, ParticleTypes.ENCHANT,
                        false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Highlight trap spike plates
            if (state.is(CDEBlocks.TRAP_SPIKE_PLATE.get())) {
                level.sendParticles(player, ParticleTypes.CRIT,
                    false, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    2, 0.3, 0.1, 0.3, 0.01);
            }

            // Highlight wall maws
            if (state.is(CDEBlocks.WALL_MAW_BLOCK.get())) {
                level.sendParticles(player, ParticleTypes.DAMAGE_INDICATOR,
                    false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.2, 0.2, 0.2, 0.0);
            }
        });
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // No special equip logic
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // No special unequip logic
    }
}
