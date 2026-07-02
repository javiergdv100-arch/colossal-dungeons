package com.colossaldungeons.enhanced.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Mod configuration for Colossal Dungeons Enhanced.
 * Split into SERVER, CLIENT, and COMMON configs.
 */
public class CDEConfig {

    // --- Server Config ---
    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    // --- Client Config ---
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    // --- Common Config ---
    public static final CommonConfig COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<ServerConfig, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();

        Pair<ClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();

        Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
    }

    public static class ServerConfig {
        public final ModConfigSpec.IntValue dungeonMaxActiveRooms;
        public final ModConfigSpec.DoubleValue trapDamageMultiplier;
        public final ModConfigSpec.IntValue oilProtectionDuration;
        public final ModConfigSpec.IntValue resonanceMaxCharge;
        public final ModConfigSpec.IntValue madnessThreshold;
        public final ModConfigSpec.IntValue puzzleTimeLimit;
        public final ModConfigSpec.BooleanValue enableVanillaInteractions;
        public final ModConfigSpec.IntValue bossHealthMultiplier;
        public final ModConfigSpec.IntValue roomActivationRadius;
        public final ModConfigSpec.IntValue maxDungeonParticipants;

        ServerConfig(ModConfigSpec.Builder builder) {
            builder.comment("Colossal Dungeons Enhanced - Server Configuration")
                   .push("dungeon");

            dungeonMaxActiveRooms = builder
                .comment("Maximum number of rooms that can be active simultaneously per dungeon")
                .defineInRange("max_active_rooms", 3, 1, 10);

            roomActivationRadius = builder
                .comment("Block radius for room activation detection")
                .defineInRange("room_activation_radius", 16, 8, 64);

            maxDungeonParticipants = builder
                .comment("Maximum players in a single dungeon instance")
                .defineInRange("max_dungeon_participants", 4, 1, 16);

            builder.pop().push("combat");

            trapDamageMultiplier = builder
                .comment("Multiplier for all trap damage (1.0 = default)")
                .defineInRange("trap_damage_multiplier", 1.0, 0.1, 5.0);

            bossHealthMultiplier = builder
                .comment("Multiplier for boss health (percentage, 100 = default)")
                .defineInRange("boss_health_multiplier", 100, 50, 500);

            builder.pop().push("mechanics");

            oilProtectionDuration = builder
                .comment("Duration of oil protection effect in ticks (20 ticks = 1 second)")
                .defineInRange("oil_protection_duration", 600, 100, 6000);

            resonanceMaxCharge = builder
                .comment("Maximum resonance charge level before discharge")
                .defineInRange("resonance_max_charge", 10, 3, 20);

            madnessThreshold = builder
                .comment("Madness level threshold before hallucination effects trigger")
                .defineInRange("madness_threshold", 60, 20, 100);

            builder.pop().push("puzzle");

            puzzleTimeLimit = builder
                .comment("Default time limit for timed puzzles in ticks (0 = no limit)")
                .defineInRange("puzzle_time_limit", 1200, 0, 12000);

            builder.pop().push("vanilla");

            enableVanillaInteractions = builder
                .comment("Enable special vanilla item interactions in dungeons")
                .define("enable_vanilla_interactions", true);

            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue enableScreenShake;
        public final ModConfigSpec.BooleanValue enableParticleEffects;
        public final ModConfigSpec.BooleanValue enableMadnessShaders;
        public final ModConfigSpec.DoubleValue hudOpacity;
        public final ModConfigSpec.BooleanValue showDungeonMinimap;
        public final ModConfigSpec.BooleanValue enableAmbientSounds;
        public final ModConfigSpec.IntValue particleDensity;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("Colossal Dungeons Enhanced - Client Configuration")
                   .push("rendering");

            enableScreenShake = builder
                .comment("Enable screen shake effects during boss attacks and traps")
                .define("enable_screen_shake", true);

            enableParticleEffects = builder
                .comment("Enable custom particle effects")
                .define("enable_particle_effects", true);

            enableMadnessShaders = builder
                .comment("Enable shader effects for madness system")
                .define("enable_madness_shaders", true);

            particleDensity = builder
                .comment("Particle density multiplier (percentage, 100 = normal)")
                .defineInRange("particle_density", 100, 10, 200);

            builder.pop().push("hud");

            hudOpacity = builder
                .comment("HUD element opacity (0.0 = invisible, 1.0 = fully opaque)")
                .defineInRange("hud_opacity", 0.85, 0.0, 1.0);

            showDungeonMinimap = builder
                .comment("Show the dungeon minimap overlay")
                .define("show_dungeon_minimap", true);

            builder.pop().push("audio");

            enableAmbientSounds = builder
                .comment("Enable ambient dungeon sounds")
                .define("enable_ambient_sounds", true);

            builder.pop();
        }
    }

    public static class CommonConfig {
        public final ModConfigSpec.BooleanValue enableDebugLogging;
        public final ModConfigSpec.BooleanValue enableAddonApi;
        public final ModConfigSpec.IntValue maxRegisteredTraps;
        public final ModConfigSpec.IntValue maxRegisteredPuzzles;

        CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("Colossal Dungeons Enhanced - Common Configuration")
                   .push("general");

            enableDebugLogging = builder
                .comment("Enable debug logging for dungeon events")
                .define("enable_debug_logging", false);

            enableAddonApi = builder
                .comment("Enable the addon API for third-party dungeon content")
                .define("enable_addon_api", true);

            builder.pop().push("limits");

            maxRegisteredTraps = builder
                .comment("Maximum number of registered trap types (includes addon traps)")
                .defineInRange("max_registered_traps", 256, 32, 1024);

            maxRegisteredPuzzles = builder
                .comment("Maximum number of registered puzzle types")
                .defineInRange("max_registered_puzzles", 128, 16, 512);

            builder.pop();
        }
    }
}
