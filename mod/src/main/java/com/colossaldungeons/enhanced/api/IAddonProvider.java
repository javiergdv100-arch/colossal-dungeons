package com.colossaldungeons.enhanced.api;

/**
 * Interface for addon mods to implement to register their content with CDE.
 * Addon mods create an implementation of this interface and register it with the AddonRegistry.
 * 
 * During initialization, CDE discovers all registered providers and calls their
 * onRegister method, passing the CDEApi instance for content registration.
 */
public interface IAddonProvider {

    /**
     * Gets the mod ID of this addon.
     *
     * @return the addon's mod ID string
     */
    String getModId();

    /**
     * Called during CDE initialization. Register all content with the provided API instance.
     * This is the main entry point for addons to add traps, puzzles, mechanisms, etc.
     *
     * @param api the CDE API instance to register content with
     */
    void onRegister(CDEApi api);

    /**
     * Gets the content pack descriptor for this addon.
     * Used for display and dependency resolution.
     *
     * @return the addon's content pack description
     */
    AddonContentPack getContentPack();
}
