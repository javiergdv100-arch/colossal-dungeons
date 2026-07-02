package com.colossaldungeons.enhanced.client;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.client.gui.DungeonCreatorScreen;
import com.colossaldungeons.enhanced.client.particle.CDEEffectManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ClientTickEvent;

/**
 * Handles client-side per-tick logic such as keybind checks and effect ticking.
 * Registered to the GAME bus to receive tick events.
 */
@EventBusSubscriber(modid = ColossalDungeons.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class CDEClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check keybind for opening dungeon creator
        while (CDEClientEvents.OPEN_DUNGEON_CREATOR.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new DungeonCreatorScreen());
            }
        }

        // Tick the effect manager
        CDEEffectManager.tick();
    }
}
