package com.colossaldungeons.enhanced.core.event;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.dungeon.DungeonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles combat-related events for dungeon mechanics.
 * Processes oil + fire interaction for 2x damage, effect chaining,
 * and boss death for dungeon completion checks.
 */
@EventBusSubscriber(modid = ColossalDungeons.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CombatEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombatEventHandler.class);

    /** Damage multiplier when entity is on fire while affected by oil */
    private static final float OIL_FIRE_MULTIPLIER = 2.0f;

    /** Tag used to mark entities coated in oil */
    private static final String OIL_TAG = "cde_oil_coated";

    /**
     * Handles damage events for oil + fire interaction and effect chaining.
     * If an entity is tagged as oil-coated and receives fire damage, damage is doubled.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;

        // Oil + Fire interaction: 2x damage if oil-coated and fire damage
        if (entity.getTags().contains(OIL_TAG)) {
            if (entity.isOnFire() || event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                float originalDamage = event.getNewDamage();
                float boosted = originalDamage * OIL_FIRE_MULTIPLIER;
                event.setNewDamage(boosted);

                // Remove oil tag after ignition (consumed by the reaction)
                entity.removeTag(OIL_TAG);

                LOGGER.debug("Oil+fire interaction: {} damage boosted to {} on {}",
                    originalDamage, boosted, entity.getName().getString());
            }
        }

        // Effect chaining: entities with weakness take extra damage from certain sources
        if (entity.hasEffect(MobEffects.WEAKNESS)) {
            // Dungeon entities deal 25% more damage to weakened targets
            if (event.getSource().getEntity() != null
                && event.getSource().getEntity().getTags().contains("cde_dungeon_entity")) {
                float current = event.getNewDamage();
                event.setNewDamage(current * 1.25f);
            }
        }
    }

    /**
     * Handles entity death events to check for boss kills and dungeon completion.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // Check if this was a dungeon boss
        if (entity.getTags().contains("cde_dungeon_boss")) {
            LOGGER.info("Dungeon boss {} was killed", entity.getName().getString());

            // Notify the dungeon manager of boss defeat
            DungeonManager manager = DungeonManager.get(serverLevel);
            manager.onBossDefeated(entity.blockPosition());

            // Notify the killer player
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                LOGGER.info("Boss killed by player: {}", player.getName().getString());
            }
        }
    }
}
