package com.colossaldungeons.enhanced;

import com.colossaldungeons.enhanced.core.config.CDEConfig;
import com.colossaldungeons.enhanced.core.registry.*;
import com.colossaldungeons.enhanced.dungeon.trap.TrapRegistry;
import com.colossaldungeons.enhanced.network.CDENetworking;
import com.colossaldungeons.enhanced.vanilla.InteractionRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ColossalDungeons.MOD_ID)
public class ColossalDungeons {

    public static final String MOD_ID = "colossal_dungeons_enhanced";
    private static final Logger LOGGER = LoggerFactory.getLogger(ColossalDungeons.class);

    public ColossalDungeons(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("Initializing Colossal Dungeons Enhanced...");

        // Register all DeferredRegisters to the mod event bus
        CDEBlocks.register(modBus);
        CDEItems.register(modBus);
        CDEEntities.register(modBus);
        CDEEffects.register(modBus);
        CDESounds.register(modBus);
        CDEParticles.register(modBus);
        CDEAttachments.register(modBus);
        CDECreativeTabs.register(modBus);

        // Mod bus listeners (lifecycle)
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(CDENetworking::registerPayloads);

        // Register mod configs
        modContainer.registerConfig(ModConfig.Type.SERVER, CDEConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CDEConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, CDEConfig.COMMON_SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            InteractionRegistry.initialize();
            TrapRegistry.initialize();
            LOGGER.info("CDE common setup complete");
        });
    }
}
