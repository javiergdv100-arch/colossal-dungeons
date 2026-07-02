package com.colossaldungeons.enhanced.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers and manages addon providers for CDE.
 * Addon mods register their IAddonProvider implementations here during mod initialization.
 * During CDE common setup, all registered providers are initialized and their content is registered.
 */
public class AddonRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddonRegistry.class);

    private static final List<IAddonProvider> PROVIDERS = new ArrayList<>();
    private static boolean initialized = false;

    private AddonRegistry() {
        // Static utility class
    }

    /**
     * Registers an addon provider. Must be called before CDE common setup.
     *
     * @param provider the addon provider to register
     */
    public static void registerProvider(IAddonProvider provider) {
        if (initialized) {
            LOGGER.warn("Attempted to register addon provider '{}' after initialization. "
                + "Content may not be properly loaded.", provider.getModId());
        }
        if (provider == null) {
            LOGGER.error("Attempted to register null addon provider");
            return;
        }
        for (IAddonProvider existing : PROVIDERS) {
            if (existing.getModId().equals(provider.getModId())) {
                LOGGER.warn("Addon provider for mod '{}' already registered, skipping duplicate",
                    provider.getModId());
                return;
            }
        }
        PROVIDERS.add(provider);
        LOGGER.info("Registered addon provider: {} ({})",
            provider.getContentPack().displayName(), provider.getModId());
    }

    /**
     * Initializes all registered addon providers by calling their onRegister method.
     * Called during CDE common setup after all DeferredRegisters have fired.
     */
    public static void initializeAll() {
        if (initialized) {
            LOGGER.warn("AddonRegistry.initializeAll() called more than once");
            return;
        }

        LOGGER.info("Initializing {} addon providers...", PROVIDERS.size());
        CDEApi api = CDEApi.getApi();

        for (IAddonProvider provider : PROVIDERS) {
            try {
                provider.onRegister(api);
                AddonContentPack pack = provider.getContentPack();
                LOGGER.info("Initialized addon '{}' v{} with {} content items",
                    pack.displayName(), pack.version(), pack.totalContentCount());
            } catch (Exception e) {
                LOGGER.error("Failed to initialize addon provider: {}", provider.getModId(), e);
            }
        }

        initialized = true;
        LOGGER.info("All addon providers initialized successfully");
    }

    /**
     * Gets an unmodifiable list of all registered providers.
     */
    public static List<IAddonProvider> getProviders() {
        return Collections.unmodifiableList(PROVIDERS);
    }

    /**
     * Gets the number of registered addon providers.
     */
    public static int getProviderCount() {
        return PROVIDERS.size();
    }

    /**
     * Checks if addons have been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets the registry (used for testing or hot-reloading).
     */
    public static void reset() {
        PROVIDERS.clear();
        initialized = false;
    }
}
