package com.colossaldungeons.enhanced.effect;

import com.colossaldungeons.enhanced.core.registry.CDEAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

/**
 * Madness effect - HARMFUL. Levels 1-5 with increasing distortion effects.
 * Color: 0x800080 (purple).
 * 
 * Level 1: Minor visual distortion (client-side shader trigger)
 * Level 2: Occasional false ambient sounds
 * Level 3: Screen shake and color shift
 * Level 4: Fake entity shadows at edge of vision
 * Level 5: Full hallucination - fake entity rendering, disorienting sounds
 */
public class MadnessEffect extends MobEffect {

    /** Maximum madness amplifier level (0-indexed, so 4 = level 5). */
    public static final int MAX_LEVEL = 4;

    public MadnessEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Tick rate depends on level - higher levels tick faster
        int interval = switch (amplifier) {
            case 0 -> 80;  // Level 1: every 4 seconds
            case 1 -> 60;  // Level 2: every 3 seconds
            case 2 -> 40;  // Level 3: every 2 seconds
            case 3 -> 20;  // Level 4: every 1 second
            default -> 10; // Level 5: every 0.5 seconds
        };
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return true;

        // At level 2+: play false ambient sounds occasionally
        if (amplifier >= 1 && level.random.nextFloat() < 0.3f) {
            playHallucinationSound(level, player, amplifier);
        }

        // At level 5 (amplifier 4): trigger full hallucination
        // Actual rendering is handled client-side via network payload
        if (amplifier >= MAX_LEVEL) {
            // Client-side hallucination rendering triggered via effect presence
            // No additional server action needed; client checks effect level each frame
        }

        return true;
    }

    private void playHallucinationSound(ServerLevel level, ServerPlayer player, int amplifier) {
        // Play random unsettling sounds at random nearby positions
        double offsetX = (level.random.nextDouble() - 0.5) * 16.0;
        double offsetZ = (level.random.nextDouble() - 0.5) * 16.0;

        level.playSound(null,
            player.getX() + offsetX,
            player.getY(),
            player.getZ() + offsetZ,
            SoundEvents.AMBIENT_CAVE.value(),
            SoundSource.AMBIENT,
            0.5f + amplifier * 0.1f,
            0.5f + level.random.nextFloat() * 0.5f
        );
    }

    /**
     * Gets the visual distortion intensity for the given amplifier level.
     * Used by client-side shader rendering.
     *
     * @param amplifier The effect amplifier (0-4)
     * @return Distortion intensity from 0.0 to 1.0
     */
    public static float getDistortionIntensity(int amplifier) {
        return Math.min(1.0f, (amplifier + 1) * 0.2f);
    }
}
