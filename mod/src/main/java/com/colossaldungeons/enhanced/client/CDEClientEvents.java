package com.colossaldungeons.enhanced.client;

import com.colossaldungeons.enhanced.ColossalDungeons;
import com.colossaldungeons.enhanced.client.gui.DungeonCreatorScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side event handler for CDE.
 * Registers entity renderers, particle providers, and key mappings.
 * Only loaded on the client via @EventBusSubscriber(value = Dist.CLIENT).
 */
@EventBusSubscriber(modid = ColossalDungeons.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CDEClientEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDEClientEvents.class);

    /** Key mapping for opening the Dungeon Creator screen */
    public static final KeyMapping OPEN_DUNGEON_CREATOR = new KeyMapping(
        "key.colossal_dungeons_enhanced.open_creator",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.colossal_dungeons_enhanced"
    );

    /**
     * Register entity renderers for all CDE entities.
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        LOGGER.info("Registering CDE entity renderers");
        // Entity renderers are registered here when entity models are implemented
        // event.registerEntityRenderer(CDEEntities.DUNGEON_BOSS.get(), DungeonBossRenderer::new);
    }

    /**
     * Register particle providers for custom particles.
     */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        LOGGER.info("Registering CDE particle providers");
        // Particle providers are registered here when custom particles are implemented
        // event.registerSpriteSet(CDEParticles.DUNGEON_DUST.get(), DungeonDustParticle.Provider::new);
    }

    /**
     * Register key mappings for CDE.
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DUNGEON_CREATOR);
        LOGGER.info("Registered CDE key mappings");
    }
}
