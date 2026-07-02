package com.colossaldungeons.enhanced.core.event;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.dungeon.DungeonManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Handles server-tick events to drive the DungeonManager.
 * Registered to the GAME event bus via @EventBusSubscriber.
 */
@EventBusSubscriber(modid = ColossalDungeons.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class DungeonEventHandler {

    /**
     * Ticks all DungeonManagers on each server tick.
     * Called after all level ticks have completed.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            DungeonManager manager = DungeonManager.get(level);
            manager.tick();
        }
    }
}
